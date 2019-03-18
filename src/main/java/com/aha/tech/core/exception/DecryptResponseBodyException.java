package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 */
public class DecryptResponseBodyException extends GatewayException {

    public static String DECRYPT_RESPONSE_BODY_ERROR_MSG = "解码返回体异常";

    public static int DECRYPT_RESPONSE_BODY_ERROR_CODE = 500;

    public DecryptResponseBodyException() {
        super(DECRYPT_RESPONSE_BODY_ERROR_MSG, DECRYPT_RESPONSE_BODY_ERROR_CODE);
    }

}
