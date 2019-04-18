package com.aha.tech.core.model.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.http.HttpHeaders;

import java.net.URI;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/4/18
 */
public class TamperProofEntity {

    private URI uri;

    private String version;

    private String timestamp;

    private String content;

    private String signature;

    public TamperProofEntity() {
        super();
    }

    public TamperProofEntity(HttpHeaders httpHeaders) {
        this.version = httpHeaders.getFirst(HEADER_X_CA_VERSION);
        this.timestamp = httpHeaders.getFirst(HEADER_X_CA_TIMESTAMP);
        this.content = httpHeaders.getFirst(HEADER_X_CA_CONTENT);
        this.signature = httpHeaders.getFirst(HEADER_X_CA_SIGNATURE);
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE);
    }
}
