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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.happiest.game.pad.simulation

import com.happiest.game.pad.dials.Dial
import com.happiest.game.pad.event.Event

interface SimulateMotionDial : Dial {

    fun simulateMotion(id: Int, relativeX: Float, relativeY: Float, outEvents: MutableList<Event>): Boolean

    fun clearSimulatedMotion(id: Int, outEvents: MutableList<Event>): Boolean
}
