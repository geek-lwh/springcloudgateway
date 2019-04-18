package com.aha.tech.core.service.impl;

import com.aha.tech.core.service.VerifyRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

import static com.aha.tech.core.support.URISupport.encryptBody;
import static com.aha.tech.core.support.URISupport.encryptUrl;

/**
 * @Author: luweihong
 * @Date: 2019/4/16
 * 校验请求是否合法
 */
@Service("httpVerifyRequestService")
public class HttpVerifyRequestServiceImpl implements VerifyRequestService {

    private static final Logger logger = LoggerFactory.getLogger(HttpVerifyRequestServiceImpl.class);

    @Value("${gateway.secret.key:d1f1bd03e3b0e08d6ebbecaa60e14445}")
    private String secretKey;

    /**
     * 校验请求url是否合法
     * @param uri
     * @param timestamp
     * @return
     */
    @Override
    public String verifyUrl(URI uri, String timestamp) {
        String rawQuery = uri.getRawQuery();
        String rawPath = uri.getRawPath();
        String encryptStr = encryptUrl(rawPath, rawQuery, timestamp, secretKey);
        return encryptStr;
    }

    /**
     * 校验请求body是否合法
     * @param body
     * @param timestamp
     * @return
     */
    @Override
    public String verifyBody(String body, String timestamp) {
        return encryptBody(body, timestamp, secretKey);
    }
}