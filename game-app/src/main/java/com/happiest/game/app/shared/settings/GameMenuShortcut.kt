package com.happiest.game.app.shared.settings

import android.view.InputDevice
import com.happiest.game.app.shared.input.getInputClass

data class GameMenuShortcut(val name: String, val keys: Set<Int>) {

    companion object {

        fun getDefault(inputDevice: InputDevice): GameMenuShortcut? {
            return inputDevice.getInputClass()
                .getSupportedShortcuts()
                .firstOrNull { shortcut ->
                    inputDevice.hasKeys(*(shortcut.keys.toIntArray())).all { it }
                }
        }

        fun findByName(device: InputDevice, name: String): GameMenuShortcut? {
            return device.getInputClass()
                .getSupportedShortcuts()
                .firstOrNull { it.name == name }
        }
    }
}
