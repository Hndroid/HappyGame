package com.happiest.game.lib.storage

data class GroupedStorageFiles(
    val primaryFile: BaseStorageFile,
    val dataFiles: List<BaseStorageFile>
) {
    fun allFiles() = listOf(primaryFile) + dataFiles
}
