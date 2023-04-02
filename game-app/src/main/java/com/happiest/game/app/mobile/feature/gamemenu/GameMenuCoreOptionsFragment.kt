package com.happiest.game.app.mobile.feature.gamemenu

import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.happiest.game.R
import com.happiest.game.app.shared.GameMenuContract
import com.happiest.game.app.shared.coreoptions.CoreOptionsPreferenceHelper
import com.happiest.game.app.shared.coreoptions.LemuroidCoreOption
import com.happiest.game.app.shared.input.InputDeviceManager
import com.happiest.game.lib.library.SystemCoreConfig
import com.happiest.game.lib.library.db.entity.Game
import com.happiest.game.lib.preferences.SharedPreferencesHelper
import com.happiest.game.lib.util.subscribeBy
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.android.support.AndroidSupportInjection
import io.reactivex.android.schedulers.AndroidSchedulers
import java.security.InvalidParameterException
import javax.inject.Inject

class GameMenuCoreOptionsFragment : PreferenceFragmentCompat() {

    @Inject lateinit var inputDeviceManager: InputDeviceManager

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore =
            SharedPreferencesHelper.getSharedPreferencesDataStore(requireContext())
        addPreferencesFromResource(R.xml.empty_preference_screen)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inputDeviceManager
            .getGamePadsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribeBy { updateScreen(it.size) }
    }

    fun updateScreen(connectedGamePads: Int) {
        preferenceScreen.removeAll()

        val extras = activity?.intent?.extras

        val coreOptions = extras?.getSerializable(GameMenuContract.EXTRA_CORE_OPTIONS) as Array<LemuroidCoreOption>?
            ?: throw InvalidParameterException("Missing EXTRA_CORE_OPTIONS")

        val advancedCoreOptions = extras?.getSerializable(
            GameMenuContract.EXTRA_ADVANCED_CORE_OPTIONS
        ) as Array<LemuroidCoreOption>? ?: throw InvalidParameterException("Missing EXTRA_ADVANCED_CORE_OPTIONS")

        val game = extras?.getSerializable(GameMenuContract.EXTRA_GAME) as Game?
            ?: throw InvalidParameterException("Missing EXTRA_GAME")

        val coreConfig = extras?.getSerializable(GameMenuContract.EXTRA_SYSTEM_CORE_CONFIG) as SystemCoreConfig?
            ?: throw InvalidParameterException("Missing EXTRA_SYSTEM_CORE_CONFIG")

        CoreOptionsPreferenceHelper.addPreferences(
            preferenceScreen,
            game.systemId,
            coreOptions.toList(),
            advancedCoreOptions.toList()
        )

        CoreOptionsPreferenceHelper.addControllers(
            preferenceScreen,
            game.systemId,
            coreConfig.coreID,
            maxOf(1, connectedGamePads),
            coreConfig.controllerConfigs
        )
    }

    @dagger.Module
    class Module
}
