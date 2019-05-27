package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/5/27
 */
public class LimiterException extends GatewayException {

    public LimiterException(String msg) {
        super(msg, 429);
    }
}
