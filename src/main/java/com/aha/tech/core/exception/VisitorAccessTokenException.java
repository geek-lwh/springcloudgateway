package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class VisitorAccessTokenException extends GatewayException {

    public static String VISITOR_ACCESS_TOKEN_ERROR_MSG = "游客访问令牌不正确";

    public static int VISITOR_ACCESS_TOKEN_ERROR_CODE = 500;

    public VisitorAccessTokenException() {
        super(VISITOR_ACCESS_TOKEN_ERROR_MSG, VISITOR_ACCESS_TOKEN_ERROR_CODE);
    }

}
