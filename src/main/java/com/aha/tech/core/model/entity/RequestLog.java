package com.aha.tech.core.model.entity;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.support.ResponseSupport;
import org.springframework.http.HttpHeaders;

import java.net.URI;

/**
 * @Author: luweihong
 * @Date: 2019/6/26
 */
public class RequestLog {

    private String requestId;

    private HttpHeaders httpHeaders;

    private URI uri;

    private String body;


    private Long cost;

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Long getCost() {
        return cost;
    }

    public void setCost(Long cost) {
        this.cost = cost;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("requestId").append(Separator.COLON_MARK).append(this.requestId).append(System.lineSeparator());
        sb.append("请求行").append(Separator.COLON_MARK).append(this.uri);
        sb.append(Separator.COMMA_MARK).append("耗时").append(Separator.COLON_MARK).append(this.cost).append(System.lineSeparator());
        sb.append("请求头").append(Separator.COLON_MARK).append(ResponseSupport.formatHttpHeaders(this.httpHeaders)).append(System.lineSeparator());
        sb.append("请求体").append(Separator.COLON_MARK).append(this.body).append(System.lineSeparator());

        return sb.toString();
    }
}
