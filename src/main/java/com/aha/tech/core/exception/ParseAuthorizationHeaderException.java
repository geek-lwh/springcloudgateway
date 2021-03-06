package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class ParseAuthorizationHeaderException extends GatewayException {

    public static String PARSE_AUTHORIZATION_HEADER_ERROR_MSG = "解析Authorization头信息异常";

    public static int PARSE_AUTHORIZATION_HEADER_ERROR_CODE = 500;

    public ParseAuthorizationHeaderException() {
        super(PARSE_AUTHORIZATION_HEADER_ERROR_MSG, PARSE_AUTHORIZATION_HEADER_ERROR_CODE);
    }

    public ParseAuthorizationHeaderException(String msg) {
        super(msg, PARSE_AUTHORIZATION_HEADER_ERROR_CODE);
    }
}
