package com.happiest.game.app.shared

import com.happiest.game.R
import com.happiest.game.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.happiest.game.app.shared.game.GameLauncher
import com.happiest.game.app.shared.main.BusyActivity
import com.happiest.game.common.displayToast
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.lib.library.db.dao.updateAsync
import com.happiest.game.lib.library.db.entity.Game

class GameInteractor(
    private val activity: BusyActivity,
    private val retrogradeDb: RetrogradeDatabase,
    private val useLeanback: Boolean,
    private val shortcutsGenerator: ShortcutsGenerator,
    private val gameLauncher: GameLauncher
) {
    fun onGamePlay(game: Game) {
        if (activity.isBusy()) {
            activity.activity().displayToast(R.string.game_interactory_busy)
            return
        }
        gameLauncher.launchGameAsync(activity.activity(), game, true, useLeanback)
    }

    fun onGameRestart(game: Game) {
        if (activity.isBusy()) {
            activity.activity().displayToast(R.string.game_interactory_busy)
            return
        }
        gameLauncher.launchGameAsync(activity.activity(), game, false, useLeanback)
    }

    fun onFavoriteToggle(game: Game, isFavorite: Boolean) {
        retrogradeDb.gameDao().updateAsync(game.copy(isFavorite = isFavorite)).subscribe()
    }

    fun onCreateShortcut(game: Game) {
        shortcutsGenerator.pinShortcutForGame(game).subscribe()
    }

    fun supportShortcuts(): Boolean {
        return shortcutsGenerator.supportShortcuts()
    }
}
