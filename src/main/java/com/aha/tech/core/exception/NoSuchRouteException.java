package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class NoSuchRouteException extends GatewayException {

    public static String NO_SUCH_ROUTE_ERROR_MSG = "没有匹配的路由关系映射,请检查配置";

    public static int NO_SUCH_ROUTE_ERROR_CODE = 500;

    public NoSuchRouteException() {
        super(NO_SUCH_ROUTE_ERROR_MSG, NO_SUCH_ROUTE_ERROR_CODE);
    }

}
