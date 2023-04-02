/*
 * GameLoader.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.happiest.game.lib.game

import android.content.Context
import android.util.Log
import com.happiest.game.common.rx.toSingleAsOptional
import com.happiest.game.lib.core.CoreVariable
import com.happiest.game.lib.core.CoreVariablesManager
import com.happiest.game.lib.library.CoreID
import com.happiest.game.lib.library.GameSystem
import com.happiest.game.lib.library.LemuroidLibrary
import com.happiest.game.lib.library.SystemCoreConfig
import com.happiest.game.lib.library.db.RetrogradeDatabase
import com.happiest.game.lib.library.db.entity.Game
import com.happiest.game.lib.saves.SaveState
import com.happiest.game.lib.saves.SavesCoherencyEngine
import com.happiest.game.lib.saves.SavesManager
import com.happiest.game.lib.saves.StatesManager
import com.happiest.game.lib.storage.DirectoriesManager
import com.happiest.game.lib.storage.RomFiles
import io.reactivex.Observable
import timber.log.Timber
import java.io.File

class GameLoader(
    private val lemuroidLibrary: LemuroidLibrary,
    private val statesManager: StatesManager,
    private val savesManager: SavesManager,
    private val coreVariablesManager: CoreVariablesManager,
    private val retrogradeDatabase: RetrogradeDatabase,
    private val savesCoherencyEngine: SavesCoherencyEngine,
    private val directoriesManager: DirectoriesManager
) {
    sealed class LoadingState {
        object LoadingCore : LoadingState()
        object LoadingGame : LoadingState()
        class Ready(val gameData: GameData) : LoadingState()
    }

    fun load(
        appContext: Context,
        game: Game,
        loadSave: Boolean,
        systemCoreConfig: SystemCoreConfig
    ): Observable<LoadingState> = Observable.create { emitter ->
        try {
            emitter.onNext(LoadingState.LoadingCore)

            val system = GameSystem.findById(game.systemId)
            Log.d("mylog", "load: " + systemCoreConfig.coreID)
            val coreLibrary = runCatching {
                Log.d("mylog coreLibrary", "load: " + findLibrary(appContext, systemCoreConfig.coreID)!!.absolutePath)
                findLibrary(appContext, systemCoreConfig.coreID)!!.absolutePath
            }.getOrElse { throw GameLoaderException(GameLoaderError.LOAD_CORE) }

            emitter.onNext(LoadingState.LoadingGame)

            if (!areRequiredBiosFilesPresent(systemCoreConfig)) {
                throw GameLoaderException(GameLoaderError.MISSING_BIOS)
            }

            val gameFiles = runCatching {
                val useVFS = systemCoreConfig.useLibretroVFS
                val dataFiles = retrogradeDatabase.dataFileDao().selectDataFilesForGame(game.id)
                lemuroidLibrary.getGameFiles(game, dataFiles, useVFS).blockingGet()
            }.getOrElse { throw GameLoaderException(GameLoaderError.LOAD_GAME) }

            val saveRAMData = runCatching {
                savesManager.getSaveRAM(game).toSingleAsOptional().blockingGet().toNullable()
            }.getOrElse { throw GameLoaderException(GameLoaderError.SAVES) }

            val quickSaveData = runCatching {
                val shouldDiscardSave =
                    !savesCoherencyEngine.shouldDiscardAutoSaveState(game, systemCoreConfig.coreID)

                if (systemCoreConfig.statesSupported && loadSave && shouldDiscardSave) {
                    statesManager.getAutoSave(game, systemCoreConfig.coreID)
                        .toSingleAsOptional()
                        .blockingGet()
                        .toNullable()
                } else {
                    null
                }
            }.getOrElse { throw GameLoaderException(GameLoaderError.SAVES) }

            val coreVariables = coreVariablesManager.getOptionsForCore(system.id, systemCoreConfig)
                .blockingGet()
                .toTypedArray()

            val systemDirectory = directoriesManager.getSystemDirectory()
            val savesDirectory = directoriesManager.getSavesDirectory()

            emitter.onNext(
                LoadingState.Ready(
                    GameData(
                        game,
                        coreLibrary,
                        gameFiles,
                        quickSaveData,
                        saveRAMData,
                        coreVariables,
                        systemDirectory,
                        savesDirectory
                    )
                )
            )
        } catch (e: GameLoaderException) {
            Timber.e(e, "Error while preparing game")
            emitter.onError(e)
        } catch (e: Exception) {
            Timber.e(e, "Error while preparing game")
            emitter.onError(GameLoaderException(GameLoaderError.GENERIC))
        } finally {
            emitter.onComplete()
        }
    }

    private fun findLibrary(context: Context, coreID: CoreID): File? {


        val files = sequenceOf(
            File(context.applicationInfo.nativeLibraryDir),
            context.filesDir
        )
        Log.d("mylog nativeLibraryDir", "load: " + context.applicationInfo.nativeLibraryDir)
        Log.d("mylog libretroFileName", "load: " + coreID.libretroFileName)


        return files
            .flatMap { it.walkBottomUp() }
            .firstOrNull { it.name == coreID.libretroFileName }
    }

    private fun areRequiredBiosFilesPresent(systemCoreConfig: SystemCoreConfig): Boolean {
        return systemCoreConfig.requiredBIOSFiles
            .map { File(directoriesManager.getSystemDirectory(), it) }
            .all { it.exists() }
    }

    @Suppress("ArrayInDataClass")
    data class GameData(
        val game: Game,
        val coreLibrary: String,
        val gameFiles: RomFiles,
        val quickSaveData: SaveState?,
        val saveRAMData: ByteArray?,
        val coreVariables: Array<CoreVariable>,
        val systemDirectory: File,
        val savesDirectory: File
    )
}
