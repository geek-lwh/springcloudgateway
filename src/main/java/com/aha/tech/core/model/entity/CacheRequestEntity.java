package com.aha.tech.core.model.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

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

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
