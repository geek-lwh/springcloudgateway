package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class EmptyBodyException extends GatewayException {

    public static String REQUEST_BODY_EMPTY_ERROR_MSG = "请求体body为空";

    public static int REQUEST_BODY_EMPTY_ERROR_CODE = 500;

    public EmptyBodyException() {
        super(REQUEST_BODY_EMPTY_ERROR_MSG, REQUEST_BODY_EMPTY_ERROR_CODE);
    }

}
