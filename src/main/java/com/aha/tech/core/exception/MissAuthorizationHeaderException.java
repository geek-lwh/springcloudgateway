package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class MissAuthorizationHeaderException extends GatewayException {

    public static String MISS_AUTHORIZATION_HEADER_ERROR_MSG = "缺少头对象 Authorization";

    public static int MISS_AUTHORIZATION_HEADER_ERROR_CODE = 500;

    public MissAuthorizationHeaderException() {
        super(MISS_AUTHORIZATION_HEADER_ERROR_MSG, MISS_AUTHORIZATION_HEADER_ERROR_CODE);
    }

}
