package com.happiest.game.app.mobile.feature.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.happiest.game.R
import com.happiest.game.app.mobile.feature.favorites.FavoritesFragment
import com.happiest.game.app.mobile.feature.games.GamesFragment
import com.happiest.game.app.mobile.feature.home.HomeFragment
import com.happiest.game.app.mobile.feature.settings.AdvancedSettingsFragment
import com.happiest.game.app.mobile.feature.settings.BiosSettingsFragment
import com.happiest.game.app.mobile.feature.settings.CoresSelectionFragment
import com.happiest.game.app.mobile.feature.settings.GamepadSettingsFragment
import com.happiest.game.app.mobile.feature.settings.SaveSyncFragment
import com.happiest.game.app.mobile.feature.settings.SettingsFragment
import com.happiest.game.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.happiest.game.app.shared.GameInteractor
import com.happiest.game.app.shared.about.AboutActivity
import com.happiest.game.app.shared.game.BaseGameActivity
import com.happiest.game.app.shared.game.GameLauncher
import com.happiest.game.app.shared.main.BusyActivity
import com.happiest.game.app.shared.main.PostGameHandler
import com.happiest.game.app.shared.settings.SettingsInteractor
import com.happiest.game.ext.feature.review.ReviewManager
import com.happiest.game.lib.android.RetrogradeAppCompatActivity
import com.happiest.game.lib.injection.PerActivity
import com.happiest.game.lib.injection.PerFragment
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.lib.storage.DirectoriesManager
import com.happiest.game.common.view.setVisibleOrGone
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class MainActivity : RetrogradeAppCompatActivity(), BusyActivity {

    @Inject lateinit var postGameHandler: PostGameHandler

    private val reviewManager = ReviewManager()
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeActivity()
    }

    override fun activity(): Activity = this
    override fun isBusy(): Boolean = mainViewModel?.displayProgress?.value ?: false

    private fun initializeActivity() {
        setSupportActionBar(findViewById(R.id.toolbar))

        reviewManager.initialize(applicationContext)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.itemIconTintList = null
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val topLevelIds = setOf(
            R.id.navigation_home,
            R.id.navigation_favorites,
            R.id.navigation_settings
        )
        val appBarConfiguration = AppBarConfiguration(topLevelIds)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        mainViewModel = ViewModelProvider(this, MainViewModel.Factory(applicationContext))
            .get(MainViewModel::class.java)

        mainViewModel?.displayProgress?.observe(this) { isRunning ->
            findViewById<ProgressBar>(R.id.progress).setVisibleOrGone(isRunning)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            BaseGameActivity.REQUEST_PLAY_GAME -> {
                postGameHandler.handle(true, this, resultCode, data)
                    .subscribeBy(Timber::e) { }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mobile_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_options_help -> {
//                displayLemuroidHelp()
                displayAboutPager()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * 显示关于页面
     */
    private fun displayAboutPager() {
        AboutActivity.launchAboutActivity(activity())
    }

    override fun onSupportNavigateUp() = findNavController(R.id.nav_host_fragment).navigateUp()

    companion object {
        fun start(activity: Activity) {
            val intent = Intent(activity, MainActivity::class.java)
            activity.startActivity(intent)
            activity.finish()
        }
    }

    @dagger.Module
    abstract class Module {

        @PerFragment
        @ContributesAndroidInjector(modules = [SettingsFragment.Module::class])
        abstract fun settingsFragment(): SettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [GamesFragment.Module::class])
        abstract fun gamesFragment(): GamesFragment

//        @PerFragment
//        @ContributesAndroidInjector(modules = [MetaSystemsFragment.Module::class])
//        abstract fun systemsFragment(): MetaSystemsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [HomeFragment.Module::class])
        abstract fun homeFragment(): HomeFragment

//        @PerFragment
//        @ContributesAndroidInjector(modules = [SearchFragment.Module::class])
//        abstract fun searchFragment(): SearchFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [FavoritesFragment.Module::class])
        abstract fun favoritesFragment(): FavoritesFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [GamepadSettingsFragment.Module::class])
        abstract fun gamepadSettings(): GamepadSettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [BiosSettingsFragment.Module::class])
        abstract fun biosInfoFragment(): BiosSettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [AdvancedSettingsFragment.Module::class])
        abstract fun advancedSettingsFragment(): AdvancedSettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [SaveSyncFragment.Module::class])
        abstract fun saveSyncFragment(): SaveSyncFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [CoresSelectionFragment.Module::class])
        abstract fun coresSelectionFragment(): CoresSelectionFragment

        @dagger.Module
        companion object {

            @Provides
            @PerActivity
            @JvmStatic
            fun settingsInteractor(activity: MainActivity, directoriesManager: DirectoriesManager) =
                SettingsInteractor(activity, directoriesManager)

            @Provides
            @PerActivity
            @JvmStatic
            fun gameInteractor(
                activity: MainActivity,
                retrogradeDb: RetrogradeDatabase,
                shortcutsGenerator: ShortcutsGenerator,
                gameLauncher: GameLauncher
            ) = GameInteractor(activity, retrogradeDb, false, shortcutsGenerator, gameLauncher)
        }
    }
}
