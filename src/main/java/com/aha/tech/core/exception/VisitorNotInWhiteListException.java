package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class VisitorNotInWhiteListException extends GatewayException {

    public static String VISITOR_NOT_IN_WHITE_LIST_ERROR_MSG = "访客请求资源不在白名单列表中";

    public static int VISITOR_NOT_IN_WHITE_LIST_ERROR_CODE = 500;

    public VisitorNotInWhiteListException() {
        super(VISITOR_NOT_IN_WHITE_LIST_ERROR_MSG, VISITOR_NOT_IN_WHITE_LIST_ERROR_CODE);
    }

}
