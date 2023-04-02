package com.happiest.game.app.shared.http.model

import com.hjq.http.config.IRequestServer

/**
 *    desc   : 服务器配置
 */
class RequestServer : IRequestServer {

    override fun getHost(): String {
        return "http://baidu.cn/"
    }


}
