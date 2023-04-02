package com.happiest.game.app.mobile.feature.tilt

import com.happiest.game.pad.RadialGamePad

interface TiltTracker {

    fun updateTracking(xTilt: Float, yTilt: Float, pads: Sequence<RadialGamePad>)

    fun stopTracking(pads: Sequence<RadialGamePad>)

    fun trackedIds(): Set<Int>
}
