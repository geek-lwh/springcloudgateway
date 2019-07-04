package com.aha.tech.core.service;

/**
 * @Author: luweihong
 * @Date: 2019/4/16
 */
public interface VerifyRequestService {

    /**
     * 校验url是否合法
     * @param uri
     * @param timestamp
     * @return
     */
    String verifyUrl(String rawPath, String uri, String timestamp, String signature);

    /**
     * 校验body是否合法
     * @param body
     * @param timestamp
     * @return
     */
    String verifyBody(String body, String timestamp);

}
