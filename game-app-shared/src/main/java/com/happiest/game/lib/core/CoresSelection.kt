package com.happiest.game.lib.core

import android.content.SharedPreferences
import com.happiest.game.lib.library.GameSystem
import com.happiest.game.lib.library.SystemCoreConfig
import com.happiest.game.lib.library.SystemID
import dagger.Lazy
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class CoresSelection(private val sharedPreferences: Lazy<SharedPreferences>) {

    fun getCoreConfigForSystem(system: GameSystem): Single<SystemCoreConfig> {
        return Single.fromCallable { fetchSystemCoreConfig(system) }
            .subscribeOn(Schedulers.io())
    }

    private fun fetchSystemCoreConfig(system: GameSystem): SystemCoreConfig {
        val setting = sharedPreferences.get()
            .getString(computeSystemPreferenceKey(system.id), null)

        return system.systemCoreConfigs.firstOrNull { it.coreID.coreName == setting }
            ?: system.systemCoreConfigs.first()
    }

    companion object {
        private const val CORE_SELECTION_BINDING_PREFERENCE_BASE_KEY = "pref_key_core_selection"

        fun computeSystemPreferenceKey(systemID: SystemID) =
            "${CORE_SELECTION_BINDING_PREFERENCE_BASE_KEY}_${systemID.dbname}"
    }
}
