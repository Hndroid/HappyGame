package com.happiest.game.app

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * 全局的API接口
 */
@SuppressLint("StaticFieldLeak")
object HappyGame {

    private lateinit var context: Context

    private lateinit var handler: Handler


    /**
     * 初始化接口。这里会进行应用程序的初始化操作，一定要在代码执行的最开始调用。
     *
     * @param c
     *          Context参数，注意这里要传入的是Application的Context，千万不能传入Activity或者Service的Context。
     */
    fun initialize(c: Context) {
        context = c
        handler = Handler(Looper.getMainLooper())
    }

    /**
     * 获取全局Context，在代码的任意位置都可以调用，随时都能获取到全局Context对象。
     *
     * @return 全局Context对象。
     */
    fun getContext(): Context {
        return context
    }

    /**
     * 获取创建在主线程上的Handler对象。
     *
     * @return 创建在主线程上的Handler对象。
     */
    fun getHandler(): Handler {
        return handler
    }

    /**
     * 返回当前应用的包名。
     */
    fun getPackageName(): String {
        return context.packageName
    }

    /**
     * 获取服务器主机地址
     */
    fun getHostUrl(): String {
        return "http://baidu.cn/"
    }

    /**
     * 获取资源文件中定义的字符串。
     *
     * @param resId
     * 字符串资源id
     * @return 字符串资源id对应的字符串内容。
     */
    fun getString(resId: Int): String {
        return getContext().resources.getString(resId)
    }

    /**
     * 将当前线程睡眠指定毫秒数。
     *
     * @param millis
     * 睡眠的时长，单位毫秒。
     */
    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
