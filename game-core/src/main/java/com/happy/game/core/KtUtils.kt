/*
 *     Copyright (C) 2021  Filippo Scognamiglio
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.happy.game.core

import android.os.Handler
import android.os.Looper

object KtUtils {
    fun runOnUIThread(runnable: () -> Unit) {
        if (isUIThread()) {
            runnable()
        } else {
            Handler(Looper.getMainLooper()).post(runnable)
        }
    }

    private fun isUIThread(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Looper.getMainLooper().isCurrentThread
        } else {
            Thread.currentThread() == Looper.getMainLooper().thread
        }
    }
}
