package com.happiest.game.app.shared.about.bean

data class UpdateConfigData(
    val isforce: Boolean,
    val path: String,
    val platform: String,
    val target_size: String,
    val update_log: String,
    val version: String,
    val version_i: Int,
    val md5: String
)
