package com.aha.tech.core.model.entity;

import java.net.URI;

/**
 * @Author: luweihong
 * @Date: 2019/4/17
 */
public class CacheRequestEntity {

    private URI requestLine;

    private String requestBody;

    public URI getRequestLine() {
        return requestLine;
    }

    public void setRequestLine(URI requestLine) {
        this.requestLine = requestLine;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }
}
