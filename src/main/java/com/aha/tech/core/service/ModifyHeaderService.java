package com.aha.tech.core.service;

import org.springframework.http.HttpHeaders;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface ModifyHeaderService {

    void initRequestHeader(HttpHeaders httpHeaders);

    /**
     * 设置头部版本号信息
     * @param httpHeaders
     */
    void setVersion(HttpHeaders httpHeaders);

    /**
     * 解析X-env 并且设置对应的值传递后端rs服务
     * @param httpHeaders
     */
    void setXEnv(HttpHeaders httpHeaders);

    /**
     * 删除无效的头部信息
     * @param httpHeaders
     */
    void removeInvalidInfo(HttpHeaders httpHeaders);

}
