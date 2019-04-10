package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class NoSuchUserNameException extends GatewayException {

    public static String NO_SUCH_USER_NAME_ERROR_MSG = "Authorization头信息解析后不是约定的userName对象";

    public static int NO_SUCH_USER_NAME_ERROR_CODE = 500;

    public NoSuchUserNameException() {
        super(NO_SUCH_USER_NAME_ERROR_MSG, NO_SUCH_USER_NAME_ERROR_CODE);
    }

    public NoSuchUserNameException(String msg) {
        super(msg, NO_SUCH_USER_NAME_ERROR_CODE);
    }

}
