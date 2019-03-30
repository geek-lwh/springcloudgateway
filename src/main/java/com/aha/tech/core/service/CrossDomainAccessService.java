package com.aha.tech.core.service;

import org.springframework.http.HttpHeaders;

/**
 * @Author: monkey
 * @Date: 2019/3/30
 */
public interface CrossDomainAccessService {

    /**
     * 跨域设置
     * @param httpHeaders
     */
    void CrossDomainSetting(HttpHeaders httpHeaders);

}
