package com.happiest.game.app.shared.http.exception;

import com.hjq.http.exception.HttpException;

/**
 *    desc   : Token 失效异常
 */
public final class TokenException extends HttpException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}