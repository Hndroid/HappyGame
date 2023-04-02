package com.happiest.game.app.shared.game

import android.app.Activity
import android.util.Log
import com.happiest.game.lib.core.CoresSelection
import com.happiest.game.lib.library.GameSystem
import com.happiest.game.lib.library.db.entity.Game
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy

class GameLauncher(private val coresSelection: CoresSelection) {

    fun launchGameAsync(activity: Activity, game: Game, loadSave: Boolean, leanback: Boolean) {
        val system = GameSystem.findById(game.systemId)
        coresSelection.getCoreConfigForSystem(system)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy {
                BaseGameActivity.launchGame(activity, it, game, loadSave)
            }
    }
}
