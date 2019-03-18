package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class NoSuchUserNameMatchException extends GatewayException {

    public static String NO_SUCH_USER_NAME_AUTHORIZATION_HEADER = "Authorization头信息解析后不是约定的userName对象";

    public static int NO_SUCH_USER_NAME_AUTHORIZATION_HEADER_CODE = 500;

    public NoSuchUserNameMatchException() {
        super(NO_SUCH_USER_NAME_AUTHORIZATION_HEADER, NO_SUCH_USER_NAME_AUTHORIZATION_HEADER_CODE);
    }

}
