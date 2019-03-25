package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class XForwardedEmptyException extends GatewayException {

    public static String X_FORWARDED_EMPTY_ERROR_MSG = "x-forwarded-for为空";

    public static int X_FORWARDED_EMPTY_ERROR_CODE = 500;

    public XForwardedEmptyException() {
        super(X_FORWARDED_EMPTY_ERROR_MSG, X_FORWARDED_EMPTY_ERROR_CODE);
    }

    public XForwardedEmptyException(int code, String message){
        super(message,code);
    }
}
