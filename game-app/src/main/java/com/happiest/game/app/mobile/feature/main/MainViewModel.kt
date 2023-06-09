package com.happiest.game.app.mobile.feature.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.happiest.game.app.shared.library.LibraryIndexMonitor
import com.happiest.game.app.shared.savesync.SaveSyncMonitor
import com.happiest.game.app.utils.livedata.CombinedLiveData

class MainViewModel(appContext: Context) : ViewModel() {

    class Factory(private val appContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainViewModel(appContext) as T
        }
    }

    private val indexingInProgress = LibraryIndexMonitor(appContext).getLiveData()
    private val saveSyncInProgress = SaveSyncMonitor(appContext).getLiveData()
    val displayProgress = CombinedLiveData(indexingInProgress, saveSyncInProgress) { a, b -> a || b }
}
