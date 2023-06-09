/*
 * Created by Filippo Scognamiglio.
 * Copyright (c) 2020. This file is part of RadialGamePad.
 *
 * RadialGamePad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RadialGamePad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RadialGamePad.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.happiest.game.pad.haptics

import android.view.View
import com.happiest.game.pad.event.Event

class AdvancedHapticEngine : HapticEngine() {

    override fun performHapticForEvents(events: List<Event>, view: View) {
        val strongestEffect = events
            .maxBy { it.haptic }
            ?.haptic ?: EFFECT_NONE

        performHaptic(strongestEffect, view)
    }
}
