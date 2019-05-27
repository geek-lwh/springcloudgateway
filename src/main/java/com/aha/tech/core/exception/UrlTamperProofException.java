package com.aha.tech.core.exception;

/**
 * @Author: luweihong
 * @Date: 2019/5/27
 */
public class UrlTamperProofException extends GatewayException {

    public UrlTamperProofException(String msg) {
        super(msg, 403);
    }
}
