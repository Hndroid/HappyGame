package com.happiest.game.app.mobile.feature.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.happiest.game.common.paging.buildLiveDataPaging
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.lib.library.db.entity.Game

class SearchViewModel(private val retrogradeDb: RetrogradeDatabase) : ViewModel() {

    class Factory(val retrogradeDb: RetrogradeDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SearchViewModel(retrogradeDb) as T
        }
    }

    val queryString = MutableLiveData<String>()

    val searchResults: LiveData<PagingData<Game>> = Transformations.switchMap(queryString) {
        buildLiveDataPaging(20, viewModelScope) { retrogradeDb.gameSearchDao().search(it) }
    }
}
