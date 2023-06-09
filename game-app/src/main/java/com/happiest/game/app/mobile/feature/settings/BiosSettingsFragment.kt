package com.happiest.game.app.mobile.feature.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.happiest.game.R
import com.happiest.game.app.shared.settings.BiosPreferences
import com.happiest.game.common.preferences.DummyDataStore
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class BiosSettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var biosPreferences: BiosPreferences

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DummyDataStore
        setPreferencesFromResource(R.xml.empty_preference_screen, rootKey)
        biosPreferences.addBiosPreferences(preferenceScreen)
    }

    @dagger.Module
    class Module
}
