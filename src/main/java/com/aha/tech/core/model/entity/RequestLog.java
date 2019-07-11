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

    private String ipLimitRemaining;

    private String ipLimitBurstCapacity;

    private String ipLimitReplenishRate;

    private String realIp;

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

    public String getIpLimitRemaining() {
        return ipLimitRemaining;
    }

    public void setIpLimitRemaining(String ipLimitRemaining) {
        this.ipLimitRemaining = ipLimitRemaining;
    }

    public String getIpLimitBurstCapacity() {
        return ipLimitBurstCapacity;
    }

    public void setIpLimitBurstCapacity(String ipLimitBurstCapacity) {
        this.ipLimitBurstCapacity = ipLimitBurstCapacity;
    }

    public String getIpLimitReplenishRate() {
        return ipLimitReplenishRate;
    }

    public void setIpLimitReplenishRate(String ipLimitReplenishRate) {
        this.ipLimitReplenishRate = ipLimitReplenishRate;
    }

    public String getRealIp() {
        return realIp;
    }

    public void setRealIp(String realIp) {
        this.realIp = realIp;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("requestId").append(Separator.COLON_MARK).append(this.requestId).append(System.lineSeparator());
        sb.append("请求行").append(Separator.COLON_MARK).append(this.uri).append(System.lineSeparator());
        sb.append("耗时").append(Separator.COLON_MARK).append(this.cost).append(System.lineSeparator());
        sb.append("请求头").append(Separator.COLON_MARK).append(ResponseSupport.formatHttpHeaders(this.httpHeaders)).append(System.lineSeparator());
        sb.append("请求体").append(Separator.COLON_MARK).append(this.body).append(System.lineSeparator());

        sb.append("ip").append(Separator.COLON_MARK).append(this.realIp).append(System.lineSeparator());
        sb.append("ip限流桶容量").append(Separator.COLON_MARK).append(this.ipLimitBurstCapacity).append(System.lineSeparator());
        sb.append("ip限流剩余桶容量").append(Separator.COLON_MARK).append(this.ipLimitRemaining).append(System.lineSeparator());
        sb.append("ip限流每秒新增桶容量").append(Separator.COLON_MARK).append(this.ipLimitReplenishRate).append(System.lineSeparator());

        return sb.toString();
    }
}
