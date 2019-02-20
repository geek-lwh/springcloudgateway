package com.aha.tech.core.exception;

import com.aha.tech.commons.exception.BaseException;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class GatewayException extends BaseException {

    public GatewayException(String msg, int code) {
        super(msg, code);
    }

    public GatewayException(String msg, int code, Throwable cause) {
        super(msg, code, cause);
    }

    public GatewayException(int code, Throwable cause) {
        super(code, cause);
    }

    public GatewayException(Throwable cause) {
        super(500, cause);
    }
}
