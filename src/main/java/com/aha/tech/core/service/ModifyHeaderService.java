package com.aha.tech.core.service;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface ModifyHeaderService {

    void initHeaders(ServerWebExchange exchange, HttpHeaders httpHeaders, String remoteIp);

    /**
     * 设置头部版本号信息
     * @param httpHeaders
     * @param serverWebExchange
     */
    void versionSetting(HttpHeaders httpHeaders, ServerWebExchange serverWebExchange);

    /**
     * 解析X-unit 并且设置对应的值传递后端rs服务
     * @param httpHeaders
     */
    void xEnvSetting(ServerWebExchange serverWebExchange,HttpHeaders httpHeaders);

    /**
     * 删除无效的头部信息
     * @param httpHeaders
     */
    void removeHeaders(HttpHeaders httpHeaders);

}
