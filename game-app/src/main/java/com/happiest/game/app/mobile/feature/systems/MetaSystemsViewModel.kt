package com.happiest.game.app.mobile.feature.systems

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.happiest.game.app.shared.systems.MetaSystemInfo
import com.happiest.game.lib.library.GameSystem
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.lib.library.metaSystemID
import io.reactivex.Observable

class MetaSystemsViewModel(retrogradeDb: RetrogradeDatabase) : ViewModel() {

    class Factory(val retrogradeDb: RetrogradeDatabase) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MetaSystemsViewModel(retrogradeDb) as T
        }
    }

    val availableMetaSystems: Observable<List<MetaSystemInfo>> = retrogradeDb.gameDao()
        .selectSystemsWithCount()
        .map { systemCounts ->
            systemCounts.filter { (_, count) -> count > 0 }
                .map { (systemId, count) -> GameSystem.findById(systemId).metaSystemID() to count }
                .groupBy { (metaSystemId, _) -> metaSystemId }
                .map { (metaSystemId, counts) -> MetaSystemInfo(metaSystemId, counts.sumBy { it.second }) }
        }
}
