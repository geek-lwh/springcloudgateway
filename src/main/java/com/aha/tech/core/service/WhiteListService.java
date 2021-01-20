package com.aha.tech.core.service;

import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/5/16
 */
public interface WhiteListService {

    /**
     * 查询跳过授权白名单列表
     * @return
     */
    List<String> fetchAuthWhiteList();

    /**
     * 根据接口维度查询跳过api的白名单
     * @return
     */
    List<String> fetchIpLimiterApiWhiteList();

    List<String> fetchIpLimiterIpWhiteList();

    /**
     * 查询跳过url防篡改白名单列表
     * @return
     */
    List<String> fetchUrlTamperProofWhiteList();
}
