package com.happiest.game.app.shared.settings

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.happiest.game.app.shared.library.LibraryIndexScheduler
import com.happiest.game.lib.preferences.SharedPreferencesHelper
import com.happiest.game.lib.storage.DirectoriesManager
import com.happiest.game.app.shared.storage.cache.CacheCleanerWork
import com.qmuiteam.qmui.skin.QMUISkinManager
import com.qmuiteam.qmui.widget.dialog.QMUIDialog

/**
 * 文件选择器
 */
class SettingsInteractor(
    private val context: Context,
    private val directoriesManager: DirectoriesManager
) {
    fun changeLocalStorageFolder() {
        StorageFrameworkPickerLauncher.pickFolder(context)
    }

    fun resetAllSettings() {
        SharedPreferencesHelper.getLegacySharedPreferences(context).edit().clear().apply()
        SharedPreferencesHelper.getSharedPreferences(context).edit().clear().apply()
        LibraryIndexScheduler.scheduleFullSync(context.applicationContext)
        CacheCleanerWork.enqueueCleanCacheAll(context.applicationContext)
        deleteDownloadedCores()
    }

    private fun deleteDownloadedCores() {
        directoriesManager.getCoresDirectory()
            .listFiles()
            ?.forEach { runCatching { it.deleteRecursively() } }
    }
}
