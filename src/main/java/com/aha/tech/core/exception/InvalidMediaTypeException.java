package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class InvalidMediaTypeException extends GatewayException {

    public static String INVALID_MEDIATYPE_ERROR_MSG = "无效的MediaType";

    public static int INVALID_MEDIATYPE_ERROR_CODE = 500;

    public InvalidMediaTypeException() {
        super(INVALID_MEDIATYPE_ERROR_MSG, INVALID_MEDIATYPE_ERROR_CODE);
    }

    public InvalidMediaTypeException(int code, String message) {
        super(message, code);
    }
}
