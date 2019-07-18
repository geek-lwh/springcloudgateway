package com.aha.tech.core.service.impl;

import com.aha.tech.core.service.VerifyRequestService;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

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
     * @param rawPath
     * @param uri
     * @param timestamp
     * @return
     */
    @Override
    public String verifyUrl(String rawPath, String uri, String timestamp, String signature) {
        String encryptStr = encryptUrl(rawPath, uri, timestamp, secretKey, signature);
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
        logger.debug("<<<<< 防篡改 body : {}", body);
        byte[] base64Body = Base64.encodeBase64(body.getBytes(StandardCharsets.UTF_8));
        String encodeBody = new String(base64Body, StandardCharsets.UTF_8);
        logger.debug("<<<<< 防篡改 base64 body : {}", body);
        return encryptBody(encodeBody, timestamp, secretKey);
    }
}
