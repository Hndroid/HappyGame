package com.happiest.game.app.shared.http.model

open class HttpData<T> {

    /** 返回码 */
    private val code: Int = 0

    /** 提示语 */
    private val msg: String? = null

    /** 数据 */
    private val data: T? = null

    fun getCode(): Int {
        return code
    }

    fun getMessage(): String? {
        return msg
    }

    fun getData(): T? {
        return data
    }

    /**
     * 是否请求成功
     */
    fun isRequestSucceed(): Boolean {
        return code == 200
    }

    /**
     * 是否 Token 失效
     */
    fun isTokenFailure(): Boolean {
        return code == 1001
    }
}
