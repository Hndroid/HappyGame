package com.happiest.game.metadata.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.happiest.game.metadata.db.dao.GameDao
import com.happiest.game.metadata.db.entity.LibretroRom

@Database(
    entities = [LibretroRom::class],
    version = 7,
    exportSchema = false
)
abstract class LibretroDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
