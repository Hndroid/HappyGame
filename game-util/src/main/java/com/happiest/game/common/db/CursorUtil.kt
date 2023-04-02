package com.happiest.game.common.db

import android.database.Cursor

fun Cursor.asSequence(): Sequence<Cursor> = generateSequence { if (moveToNext()) this else null }
