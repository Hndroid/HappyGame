package com.happy.game.core

class HappyFunJni {

    external fun create(
        GLESVersion: Int,
        coreFilePath: String?,
        systemDir: String?,
        savesDir: String?,
        variables: Array<Variable>,
        shaderType: Int,
        refreshRate: Float,
        preferLowLatencyAudio: Boolean,
        enableVirtualFileSystem: Boolean,
        language: String?
    )
    external fun loadGameFromPath(gameFilePath: String?)

    external fun loadGameFromBytes(gameFileBytes: ByteArray)

    external fun loadGameFromVirtualFiles(virtualFiles: List<DetachedVirtualFile?>?)

    external fun resume()

    external fun onSurfaceCreated()
    external fun onSurfaceChanged(width: Int, height: Int)

    external fun pause()
    external fun destroy()

    external fun step(retroView: GLRetroView?)

    external fun reset()

    external fun setRumbleEnabled(enabled: Boolean)

    external fun setFrameSpeed(speed: Int)

    external fun setAudioEnabled(enabled: Boolean)

    external fun serializeState(): ByteArray

    external fun unserializeState(state: ByteArray?): Boolean

    external fun serializeSRAM(): ByteArray
    external fun unserializeSRAM(sram: ByteArray?): Boolean

    external fun availableDisks(): Int

    external fun updateVariable(variable: Variable?)
    external fun getVariables(): Array<Variable>

    external fun currentDisk(): Int
    external fun changeDisk(index: Int)

    /** Send motion events. Analog events in range [-1, +1] and touch events in range [0,1]  */
    external fun onMotionEvent(port: Int, motionSource: Int, xAxis: Float, yAxis: Float)

    external fun onKeyEvent(port: Int, action: Int, keyCode: Int)

    external fun getAspectRatio(): Float

    external fun getControllers(): Array<Array<Controller>>
    external fun setControllerType(port: Int, type: Int)

    companion object {

        /* Double Check */
        val INSTANCE: HappyFunJni by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { HappyFunJni() }

        const val MOTION_SOURCE_DPAD = 0
        const val MOTION_SOURCE_ANALOG_LEFT = 1
        const val MOTION_SOURCE_ANALOG_RIGHT = 2
        const val MOTION_SOURCE_POINTER = 3

        const val SHADER_DEFAULT = 0
        const val SHADER_CRT = 1
        const val SHADER_LCD = 2
        const val SHADER_SHARP = 3

        const val ERROR_LOAD_LIBRARY = 0
        const val ERROR_LOAD_GAME = 1
        const val ERROR_GL_NOT_COMPATIBLE = 2
        const val ERROR_SERIALIZATION = 3
        const val ERROR_GENERIC = 4

        init {
            System.loadLibrary("happygame")
        }
    }
}
