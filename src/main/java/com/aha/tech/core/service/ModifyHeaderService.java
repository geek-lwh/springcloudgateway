package com.aha.tech.core.service;

import org.springframework.http.HttpHeaders;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface ModifyHeaderService {

    void initHeaders(HttpHeaders httpHeaders);

    /**
     * 设置头部版本号信息
     * @param httpHeaders
     */
    void versionSetting(HttpHeaders httpHeaders);

    /**
     * 解析X-env 并且设置对应的值传递后端rs服务
     * @param httpHeaders
     */
    void xEnvSetting(HttpHeaders httpHeaders);

    /**
     * 删除无效的头部信息
     * @param httpHeaders
     */
    void removeHeaders(HttpHeaders httpHeaders);

}
