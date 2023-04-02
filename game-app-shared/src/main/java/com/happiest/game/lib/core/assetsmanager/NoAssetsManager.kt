package com.happiest.game.lib.core.assetsmanager

import android.content.SharedPreferences
import com.happiest.game.lib.core.CoreUpdater
import com.happiest.game.lib.library.CoreID
import com.happiest.game.lib.storage.DirectoriesManager
import io.reactivex.Completable

class NoAssetsManager : CoreID.AssetsManager {

    override fun clearAssets(directoriesManager: DirectoriesManager) = Completable.complete()

    override fun retrieveAssetsIfNeeded(
        coreUpdaterApi: CoreUpdater.CoreManagerApi,
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences
    ): Completable {
        return Completable.complete()
    }
}
