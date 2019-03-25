package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class MissHeaderXForwardedException extends GatewayException {

    public static String MISS_HEADER_FORWARDED_ERROR_MSG = "缺失X-forwarded-for报头";

    public static int MISS_HEADER_FORWARDED_ERROR_CODE = 500;

    public MissHeaderXForwardedException() {
        super(MISS_HEADER_FORWARDED_ERROR_MSG, MISS_HEADER_FORWARDED_ERROR_CODE);
    }

    public MissHeaderXForwardedException(int code, String message){
        super(message,code);
    }
}
