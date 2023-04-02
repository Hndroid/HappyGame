/*
 * RetrogradeApplicationModule.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.happiest.game.app

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.happiest.game.app.mobile.feature.game.GameActivity
import com.happiest.game.app.mobile.feature.gamemenu.GameMenuActivity
import com.happiest.game.app.mobile.feature.main.MainActivity
import com.happiest.game.app.mobile.feature.settings.RxSettingsManager
import com.happiest.game.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.happiest.game.app.shared.dialog.PreShareActivity
import com.happiest.game.app.shared.rumble.RumbleManager
import com.happiest.game.app.shared.game.ExternalGameLauncherActivity
import com.happiest.game.app.shared.game.GameLauncher
import com.happiest.game.app.shared.main.PostGameHandler
import com.happiest.game.app.shared.settings.BiosPreferences
import com.happiest.game.app.shared.settings.ControllerConfigsManager
import com.happiest.game.app.shared.settings.CoresSelectionPreferences
import com.happiest.game.app.shared.input.InputDeviceManager
import com.happiest.game.app.shared.launch.SplashActivity
import com.happiest.game.app.shared.settings.GamePadPreferencesHelper
import com.happiest.game.app.shared.settings.StorageFrameworkPickerLauncher
import com.happiest.game.ext.feature.core.CoreUpdaterImpl
import com.happiest.game.ext.feature.review.ReviewManager
import com.happiest.game.ext.feature.savesync.SaveSyncManagerImpl
import com.happiest.game.lib.bios.BiosManager
import com.happiest.game.lib.core.CoreUpdater
import com.happiest.game.lib.core.CoreVariablesManager
import com.happiest.game.lib.core.CoresSelection
import com.happiest.game.lib.game.GameLoader
import com.happiest.game.lib.injection.PerActivity
import com.happiest.game.lib.injection.PerApp
import com.happiest.game.lib.library.LemuroidLibrary
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.lib.library.db.dao.GameSearchDao
import com.happiest.game.lib.library.db.dao.Migrations
import com.happiest.game.lib.logging.RxTimberTree
import com.happiest.game.lib.preferences.SharedPreferencesHelper
import com.happiest.game.lib.saves.SavesCoherencyEngine
import com.happiest.game.lib.saves.SavesManager
import com.happiest.game.lib.saves.StatesManager
import com.happiest.game.lib.saves.StatesPreviewManager
import com.happiest.game.lib.savesync.SaveSyncManager
import com.happiest.game.lib.storage.DirectoriesManager
import com.happiest.game.lib.storage.StorageProvider
import com.happiest.game.lib.storage.StorageProviderRegistry
import com.happiest.game.lib.storage.local.LocalStorageProvider
import com.happiest.game.lib.storage.local.StorageAccessFrameworkProvider
import com.happiest.game.metadata.LibretroDBMetadataProvider
import com.happiest.game.metadata.db.LibretroDBManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import dagger.multibindings.IntoSet
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.InputStream
import java.lang.reflect.Type
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import dagger.Lazy

@Module
abstract class HappyGameApplicationModule {

    @Binds
    abstract fun context(app: HappyGameApplication): Context

    @Binds
    abstract fun saveSyncManager(saveSyncManagerImpl: SaveSyncManagerImpl): SaveSyncManager

    @PerActivity
    @ContributesAndroidInjector(modules = [MainActivity.Module::class])
    abstract fun mainActivity(): MainActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun externalGameLauncherActivity(): ExternalGameLauncherActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun gameActivity(): GameActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun preShareActivity(): PreShareActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun splashActivity(): SplashActivity

    @PerActivity
    @ContributesAndroidInjector(modules = [GameMenuActivity.Module::class])
    abstract fun gameMenuActivity(): GameMenuActivity

    @PerActivity
    @ContributesAndroidInjector
    abstract fun storageFrameworkPickerLauncher(): StorageFrameworkPickerLauncher

    @Module
    companion object {
        @Provides
        @PerApp
        @JvmStatic
        fun executorService(): ExecutorService = Executors.newSingleThreadExecutor()

        @Provides
        @PerApp
        @JvmStatic
        fun libretroDBManager(app: HappyGameApplication, executorService: ExecutorService) =
            LibretroDBManager(app, executorService)

        @Provides
        @PerApp
        @JvmStatic
        fun retrogradeDb(app: HappyGameApplication) =
            Room.databaseBuilder(app, RetrogradeDatabase::class.java, RetrogradeDatabase.DB_NAME)
                .addCallback(GameSearchDao.CALLBACK)
                .addMigrations(GameSearchDao.MIGRATION, Migrations.VERSION_8_9)
                .fallbackToDestructiveMigration()
                .build()

        @Provides
        @PerApp
        @JvmStatic
        fun ovgdbMetadataProvider(ovgdbManager: LibretroDBManager) = LibretroDBMetadataProvider(
            ovgdbManager
        )

        @Provides
        @PerApp
        @IntoSet
        @JvmStatic
        fun localSAFStorageProvider(
            context: Context,
            metadataProvider: LibretroDBMetadataProvider
        ): StorageProvider = StorageAccessFrameworkProvider(context, metadataProvider)

        @Provides
        @PerApp
        @IntoSet
        @JvmStatic
        fun localGameStorageProvider(
            context: Context,
            directoriesManager: DirectoriesManager,
            metadataProvider: LibretroDBMetadataProvider
        ): StorageProvider =
            LocalStorageProvider(context, directoriesManager, metadataProvider)

        @Provides
        @PerApp
        @JvmStatic
        fun gameStorageProviderRegistry(
            context: Context,
            providers: Set<@JvmSuppressWildcards StorageProvider>
        ) =
            StorageProviderRegistry(context, providers)

        @Provides
        @PerApp
        @JvmStatic
        fun lemuroidLibrary(
            db: RetrogradeDatabase,
            storageProviderRegistry: Lazy<StorageProviderRegistry>,
            biosManager: BiosManager
        ) = LemuroidLibrary(db, storageProviderRegistry, biosManager)

        @Provides
        @PerApp
        @JvmStatic
        fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .build()

        @Provides
        @PerApp
        @JvmStatic
        fun retrofit(): Retrofit = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createAsync())
            .baseUrl("https://example.com")
            .addConverterFactory(
                object : Converter.Factory() {
                    override fun responseBodyConverter(
                        type: Type,
                        annotations: Array<out Annotation>,
                        retrofit: Retrofit
                    ): Converter<ResponseBody, *>? {
                        if (type == ZipInputStream::class.java) {
                            return Converter<ResponseBody, ZipInputStream> { responseBody ->
                                ZipInputStream(responseBody.byteStream())
                            }
                        }
                        if (type == InputStream::class.java) {
                            return Converter<ResponseBody, InputStream> { responseBody ->
                                responseBody.byteStream()
                            }
                        }
                        return null
                    }
                }
            )
            .build()

        @Provides
        @PerApp
        @JvmStatic
        fun directoriesManager(context: Context) = DirectoriesManager(context)

        @Provides
        @PerApp
        @JvmStatic
        fun statesManager(directoriesManager: DirectoriesManager) = StatesManager(directoriesManager)

        @Provides
        @PerApp
        @JvmStatic
        fun savesManager(directoriesManager: DirectoriesManager) = SavesManager(directoriesManager)

        @Provides
        @PerApp
        @JvmStatic
        fun statesPreviewManager(directoriesManager: DirectoriesManager) =
            StatesPreviewManager(directoriesManager)

        @Provides
        @PerApp
        @JvmStatic
        fun coreManager(
            directoriesManager: DirectoriesManager,
            retrofit: Retrofit
        ): CoreUpdater = CoreUpdaterImpl(directoriesManager, retrofit)

        @Provides
        @PerApp
        @JvmStatic
        fun rxTree() = RxTimberTree()

        @Provides
        @PerApp
        @JvmStatic
        fun coreVariablesManager(sharedPreferences: Lazy<SharedPreferences>) = CoreVariablesManager(sharedPreferences)

        @Provides
        @PerApp
        @JvmStatic
        fun gameLoader(
            lemuroidLibrary: LemuroidLibrary,
            statesManager: StatesManager,
            savesManager: SavesManager,
            coreVariablesManager: CoreVariablesManager,
            retrogradeDatabase: RetrogradeDatabase,
            savesCoherencyEngine: SavesCoherencyEngine,
            directoriesManager: DirectoriesManager
        ) = GameLoader(
            lemuroidLibrary,
            statesManager,
            savesManager,
            coreVariablesManager,
            retrogradeDatabase,
            savesCoherencyEngine,
            directoriesManager
        )

        @Provides
        @PerApp
        @JvmStatic
        fun gamepadsManager(context: Context, sharedPreferences: Lazy<SharedPreferences>) =
            InputDeviceManager(context, sharedPreferences)

        @Provides
        @PerApp
        @JvmStatic
        fun biosManager(directoriesManager: DirectoriesManager) = BiosManager(directoriesManager)

        @Provides
        @PerApp
        @JvmStatic
        fun biosPreferences(biosManager: BiosManager) = BiosPreferences(biosManager)

        @Provides
        @PerApp
        @JvmStatic
        fun coresSelection(sharedPreferences: Lazy<SharedPreferences>) = CoresSelection(sharedPreferences)

        @Provides
        @PerApp
        @JvmStatic
        fun coreSelectionPreferences() = CoresSelectionPreferences()

        @Provides
        @PerApp
        @JvmStatic
        fun inputDeviceManager(inputDeviceManager: InputDeviceManager) =
            GamePadPreferencesHelper(inputDeviceManager)

        @Provides
        @PerApp
        @JvmStatic
        fun savesCoherencyEngine(savesManager: SavesManager, statesManager: StatesManager) =
            SavesCoherencyEngine(savesManager, statesManager)

        @Provides
        @PerApp
        @JvmStatic
        fun saveSyncManagerImpl(
            context: Context,
            directoriesManager: DirectoriesManager
        ) = SaveSyncManagerImpl(context, directoriesManager)

        @Provides
        @PerApp
        @JvmStatic
        fun postGameHandler(retrogradeDatabase: RetrogradeDatabase) =
            PostGameHandler(ReviewManager(), retrogradeDatabase)

        @Provides
        @PerApp
        @JvmStatic
        fun shortcutsGenerator(context: Context, retrofit: Retrofit) =
            ShortcutsGenerator(context, retrofit)

//        @Provides
//        @PerApp
//        @JvmStatic
//        fun channelHandler(
//            context: Context,
//            retrogradeDatabase: RetrogradeDatabase,
//            retrofit: Retrofit
//        ) =
//            ChannelHandler(context, retrogradeDatabase, retrofit)

        @Provides
        @PerApp
        @JvmStatic
        fun retroControllerManager(sharedPreferences: Lazy<SharedPreferences>) =
            ControllerConfigsManager(sharedPreferences)

        @Provides
        @PerApp
        @JvmStatic
        fun rxSettingsManager(context: Context, sharedPreferences: Lazy<SharedPreferences>) =
            RxSettingsManager(context, sharedPreferences)

        @Provides
        @PerApp
        @JvmStatic
        fun sharedPreferences(context: Context) =
            SharedPreferencesHelper.getSharedPreferences(context)

        @Provides
        @PerApp
        @JvmStatic
        fun gameLauncher(coresSelection: CoresSelection) =
            GameLauncher(coresSelection)

        @Provides
        @PerApp
        @JvmStatic
        fun rumbleManager(
            context: Context,
            rxSettingsManager: RxSettingsManager,
            inputDeviceManager: InputDeviceManager
        ) =
            RumbleManager(context, rxSettingsManager, inputDeviceManager)
    }
}
