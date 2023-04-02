package com.happiest.game.app.shared.main

import android.app.Activity

interface BusyActivity {
    fun activity(): Activity
    fun isBusy(): Boolean
}
