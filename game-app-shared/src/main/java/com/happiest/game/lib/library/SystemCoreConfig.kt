package com.happiest.game.lib.library

import com.happiest.game.lib.controller.ControllerConfig
import com.happiest.game.lib.core.CoreVariable
import java.io.Serializable

data class SystemCoreConfig(
    val coreID: CoreID,
    val controllerConfigs: HashMap<Int, ArrayList<ControllerConfig>>,
    val exposedSettings: List<ExposedSetting> = listOf(),
    val exposedAdvancedSettings: List<ExposedSetting> = listOf(),
    val defaultSettings: List<CoreVariable> = listOf(),
    val statesSupported: Boolean = true,
    val rumbleSupported: Boolean = false,
    val requiredBIOSFiles: List<String> = listOf(),
    val statesVersion: Int = 0,
    val useLibretroVFS: Boolean = false
) : Serializable
