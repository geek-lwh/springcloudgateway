package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class AnonymousUserException extends GatewayException {

    public static String NO_PERMISSION = "无权限访问";

    public static int NO_PERMISSION_ERROR_CODE = 10002;

    public AnonymousUserException() {
        super(NO_PERMISSION, NO_PERMISSION_ERROR_CODE);
    }

}