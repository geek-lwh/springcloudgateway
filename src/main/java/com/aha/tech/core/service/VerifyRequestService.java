package com.aha.tech.core.service;

import java.net.URI;

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
    String verifyUrl(URI uri, String timestamp);

    /**
     * 校验body是否合法
     * @param body
     * @param timestamp
     * @return
     */
    String verifyBody(String body, String timestamp);

}
