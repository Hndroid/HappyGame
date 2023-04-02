package com.happiest.game.app.mobile.feature.gamemenu

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.happiest.game.R
import com.happiest.game.app.shared.GameMenuContract
import com.happiest.game.app.shared.gamemenu.GameMenuHelper
import com.happiest.game.common.preferences.DummyDataStore
import com.happiest.game.common.rx.toSingleAsOptional
import com.happiest.game.lib.library.SystemCoreConfig
import com.happiest.game.lib.library.db.entity.Game
import com.happiest.game.lib.saves.StatesManager
import com.happiest.game.lib.saves.StatesPreviewManager
import com.happiest.game.lib.util.subscribeBy
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import dagger.android.support.AndroidSupportInjection
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.security.InvalidParameterException
import javax.inject.Inject

class GameMenuLoadFragment : PreferenceFragmentCompat() {

    @Inject lateinit var statesManager: StatesManager
    @Inject lateinit var statesPreviewManager: StatesPreviewManager

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = DummyDataStore
        addPreferencesFromResource(R.xml.empty_preference_screen)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = activity?.intent?.extras

        val game = extras?.getSerializable(GameMenuContract.EXTRA_GAME) as Game?
            ?: throw InvalidParameterException("Missing EXTRA_GAME")

        val systemCoreConfig = extras?.getSerializable(GameMenuContract.EXTRA_SYSTEM_CORE_CONFIG) as SystemCoreConfig?
            ?: throw InvalidParameterException("Missing EXTRA_SYSTEM_CORE_CONFIG")

        setupLoadPreference(game, systemCoreConfig)
    }

    private fun setupLoadPreference(game: Game, systemCoreConfig: SystemCoreConfig) {
        statesManager.getSavedSlotsInfo(game, systemCoreConfig.coreID)
            .toObservable()
            .flatMap {
                Observable.fromIterable(it.mapIndexed { index, saveInfo -> index to saveInfo })
            }
            .flatMapSingle { (index, saveInfo) ->
                GameMenuHelper.getSaveStateBitmap(
                    requireContext(),
                    statesPreviewManager,
                    saveInfo,
                    game,
                    systemCoreConfig.coreID,
                    index
                )
                    .toSingleAsOptional()
                    .map { Triple(index, saveInfo, it) }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .autoDispose(scope())
            .subscribeBy { (index, saveInfo, bitmap) ->
                GameMenuHelper.addLoadPreference(
                    preferenceScreen,
                    index,
                    saveInfo,
                    bitmap.toNullable()
                )
            }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return GameMenuHelper.onPreferenceTreeClicked(activity, preference)
    }

    @dagger.Module
    class Module
}
