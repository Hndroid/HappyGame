package com.happiest.game.app.mobile.feature.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.happiest.game.R
import com.happiest.game.app.shared.savesync.SaveSyncMonitor
import com.happiest.game.app.shared.settings.SaveSyncPreferences
import com.happiest.game.lib.preferences.SharedPreferencesHelper
import com.happiest.game.lib.savesync.SaveSyncManager
import com.happiest.game.lib.storage.DirectoriesManager
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class SaveSyncFragment : PreferenceFragmentCompat() {

    @Inject lateinit var directoriesManager: DirectoriesManager
    @Inject lateinit var saveSyncManager: SaveSyncManager
    private lateinit var saveSyncPreferences: SaveSyncPreferences

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore =
            SharedPreferencesHelper.getSharedPreferencesDataStore(requireContext())

        saveSyncPreferences = SaveSyncPreferences(saveSyncManager)
        setPreferencesFromResource(R.xml.empty_preference_screen, rootKey)
        saveSyncPreferences.addSaveSyncPreferences(preferenceScreen)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (saveSyncPreferences.onPreferenceTreeClick(activity, preference))
            return true

        return super.onPreferenceTreeClick(preference)
    }

    override fun onResume() {
        super.onResume()
        saveSyncPreferences.updatePreferences(preferenceScreen, false)
        SaveSyncMonitor(requireContext()).getLiveData().observe(this) {
            saveSyncPreferences.updatePreferences(preferenceScreen, it)
        }
    }

    @dagger.Module
    class Module
}
