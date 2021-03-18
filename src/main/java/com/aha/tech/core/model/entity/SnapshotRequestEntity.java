package com.aha.tech.core.model.entity;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.util.HeaderUtil;
import org.springframework.http.HttpHeaders;

import java.net.URI;

/**
 * @Author: luweihong
 * @Date: 2019/4/17
 */
public class SnapshotRequestEntity {

    private HttpHeaders originalRequestHttpHeaders;

    private HttpHeaders afterModifyRequestHttpHeaders;

    private URI requestLine;

    private String realServer;

    private String requestBody;

    public URI getRequestLine() {
        return requestLine;
    }

    public HttpHeaders getOriginalRequestHttpHeaders() {
        return originalRequestHttpHeaders;
    }


    public void setOriginalRequestHttpHeaders(HttpHeaders originalRequestHttpHeaders) {
        this.originalRequestHttpHeaders = originalRequestHttpHeaders;
    }

    public HttpHeaders getAfterModifyRequestHttpHeaders() {
        return afterModifyRequestHttpHeaders;
    }

    public void setAfterModifyRequestHttpHeaders(HttpHeaders afterModifyRequestHttpHeaders) {
        this.afterModifyRequestHttpHeaders = afterModifyRequestHttpHeaders;
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

    public String getRealServer() {
        return realServer;
    }

    public void setRealServer(String realServer) {
        this.realServer = realServer;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("请求行").append(Separator.COLON_MARK).append(this.requestLine).append(System.lineSeparator());
        sb.append("路由服务器").append(Separator.COLON_MARK).append(this.realServer).append(System.lineSeparator());
        sb.append("原始请求头").append(Separator.COLON_MARK).append(HeaderUtil.formatHttpHeaders(this.originalRequestHttpHeaders)).append(System.lineSeparator());
        sb.append("修改后请求头").append(Separator.COLON_MARK).append(HeaderUtil.formatHttpHeaders(this.afterModifyRequestHttpHeaders)).append(System.lineSeparator());
        sb.append("请求体").append(Separator.COLON_MARK).append(this.requestBody);
        return sb.toString();
    }

}
