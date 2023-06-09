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

package com.happiest.game.pad

import android.graphics.Canvas
import android.graphics.RectF
import com.happiest.game.pad.dials.Dial
import com.happiest.game.pad.event.Event
import com.happiest.game.pad.event.GestureType
import com.happiest.game.pad.math.Sector
import com.happiest.game.pad.touchbound.EmptyTouchBound
import com.happiest.game.pad.touchbound.TouchBound
import com.happiest.game.pad.utils.TouchUtils

internal class DialInteractor(val dial: Dial, var touchBound: TouchBound = EmptyTouchBound) {

    fun measure(drawingRect: RectF, secondarySector: Sector? = null) {
        dial.measure(drawingRect, secondarySector)
    }

    fun draw(canvas: Canvas) {
        dial.draw(canvas)
    }

    fun forwardTouch(
        fingers: List<TouchUtils.FingerPosition>,
        outEvents: MutableList<Event>,
        allTrackedFingers: Set<Int>
    ): Boolean {
        val goodFingers = fingers.filter {
            val isTrackedByThisDial = it.pointerId in dial.trackedPointersIds()
            val isTrackedByOtherDial = !isTrackedByThisDial && it.pointerId in allTrackedFingers
            val isContained = touchBound.contains(it.x, it.y)
            isTrackedByThisDial || (!isTrackedByOtherDial && isContained)
        }

        return dial.touch(
            TouchUtils.computeRelativeFingerPosition(goodFingers, dial.drawingBox()),
            outEvents
        )
    }

    fun gesture(
        x: Float,
        y: Float,
        gestureType: GestureType,
        outEvents: MutableList<Event>
    ): Boolean {
        if (touchBound.contains(x, y)) {
            val relativePosition = TouchUtils.computeRelativePosition(x, y, dial.drawingBox())
            return dial.gesture(relativePosition.x, relativePosition.y, gestureType, outEvents)
        }

        return false
    }
}
