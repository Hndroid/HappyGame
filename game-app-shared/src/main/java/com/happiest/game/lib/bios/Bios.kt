package com.happiest.game.lib.bios

import com.happiest.game.lib.library.SystemID

data class Bios(
    val fileName: String,
    val crc32: String,
    val md5: String,
    val description: String,
    val systemID: SystemID
)
