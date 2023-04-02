package com.happiest.game.app.mobile.feature.settings

import android.content.Context
import android.os.Bundle
import android.view.InputDevice
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.happiest.game.R
import com.happiest.game.app.shared.input.InputDeviceManager
import com.happiest.game.app.shared.settings.GamePadPreferencesHelper
import com.happiest.game.lib.preferences.SharedPreferencesHelper
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

class GamepadSettingsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var gamePadPreferencesHelper: GamePadPreferencesHelper
    @Inject lateinit var inputDeviceManager: InputDeviceManager

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore =
            SharedPreferencesHelper.getSharedPreferencesDataStore(requireContext())
        setPreferencesFromResource(R.xml.empty_preference_screen, rootKey)
        inputDeviceManager.getGamePadsObservable()
            .distinctUntilChanged()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribe { refreshGamePads(it) }
    }

    private fun refreshGamePads(gamePads: List<InputDevice>) {
        preferenceScreen.removeAll()
        gamePadPreferencesHelper.addGamePadsPreferencesToScreen(
            requireContext(),
            preferenceScreen,
            gamePads
        )
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.pref_key_reset_gamepad_bindings) -> handleResetBindings()
        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun handleResetBindings() {
        gamePadPreferencesHelper.resetBindingsAndRefresh()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribe { refreshGamePads(it) }
    }

    @dagger.Module
    class Module
}
