package com.happiest.game.app.shared.settings

import android.content.Context
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.happiest.game.app.shared.library.LibraryIndexScheduler
import com.happiest.game.lib.core.CoresSelection
import com.happiest.game.lib.library.GameSystem

class CoresSelectionPreferences {

    fun addCoresSelectionPreferences(preferenceScreen: PreferenceScreen) {
        val context = preferenceScreen.context

        GameSystem.all()
            .filter { it.systemCoreConfigs.size > 1 }
            .forEach {
                preferenceScreen.addPreference(createPreference(context, it))
            }
    }

    private fun createPreference(context: Context, system: GameSystem): Preference {
        val preference = ListPreference(context)
        preference.key = CoresSelection.computeSystemPreferenceKey(system.id)
        preference.title = context.getString(system.titleResId)
        preference.isIconSpaceReserved = false
        preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
        preference.setDefaultValue(system.systemCoreConfigs.map { it.coreID.coreName }.first())
        preference.isEnabled = system.systemCoreConfigs.size > 1

        preference.entries = system.systemCoreConfigs
            .map { it.coreID.coreDisplayName }
            .toTypedArray()

        preference.entryValues = system.systemCoreConfigs
            .map { it.coreID.coreName }
            .toTypedArray()

        preference.setOnPreferenceChangeListener { _, _ ->
            LibraryIndexScheduler.scheduleCoreUpdate(context.applicationContext)
            true
        }

        return preference
    }
}
