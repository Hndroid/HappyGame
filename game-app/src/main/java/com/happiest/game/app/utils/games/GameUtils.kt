package com.happiest.game.app.utils.games

import android.content.Context
import com.happiest.game.lib.library.GameSystem
import com.happiest.game.lib.library.db.entity.Game

class GameUtils {

    companion object {
        fun getGameSubtitle(context: Context, game: Game): String {
            val systemName = getSystemNameForGame(context, game)
            val developerName = if (game.developer?.isNotBlank() == true) {
                "- ${game.developer}"
            } else {
                ""
            }
            return "$systemName $developerName"
        }

        private fun getSystemNameForGame(context: Context, game: Game): String {
            val systemTitleResource = GameSystem.findById(game.systemId).shortTitleResId
            return context.getString(systemTitleResource)
        }
    }
}
