package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class AuthorizationFailedException extends GatewayException {

    public static String AUTHORIZATION_FAILED_ERROR_MESSAGE = "权限校验异常";

    public static int AUTHORIZATION_FAILED_CODE = 10003;

    public AuthorizationFailedException() {
        super(AUTHORIZATION_FAILED_ERROR_MESSAGE, AUTHORIZATION_FAILED_CODE);
    }

    public AuthorizationFailedException(int code,String message){
        super(message,code);
    }
}