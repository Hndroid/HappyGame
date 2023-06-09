package com.happiest.game.app.mobile.feature.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.happiest.game.common.paging.buildLiveDataPaging
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.lib.library.db.entity.Game

class FavoritesViewModel(retrogradeDb: RetrogradeDatabase) : ViewModel() {

    class Factory(val retrogradeDb: RetrogradeDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FavoritesViewModel(retrogradeDb) as T
        }
    }

    val favorites: LiveData<PagingData<Game>> =
        buildLiveDataPaging(20, viewModelScope) { retrogradeDb.gameDao().selectFavorites() }
}
