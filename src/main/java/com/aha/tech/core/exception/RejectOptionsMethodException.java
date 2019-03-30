package com.aha.tech.core.exception;

/**
 * @Author: monkey
 * @Date: 2019/3/30
 */
public class RejectOptionsMethodException extends GatewayException {

    public static String REJECT_OPTIONS_METHOD_ERROR_MSG = "拒绝处理Http_method=options的请求";

    public static int REJECT_OPTIONS_METHOD_ERROR_CODE = 500;

    public RejectOptionsMethodException() {
        super(REJECT_OPTIONS_METHOD_ERROR_MSG, REJECT_OPTIONS_METHOD_ERROR_CODE);
    }

    public RejectOptionsMethodException(String msg, int code) {
        super(msg, code);
    }
}
