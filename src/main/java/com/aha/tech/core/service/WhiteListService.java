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
    List<String> findSkipAuthWhiteList();

    /**
     * 查询跳过ip限流白名单列表
     * @return
     */
    List<String> findSkipIpLimiterWhiteList();

    /**
     * 查询跳过url防篡改白名单列表
     * @return
     */
    List<String> findSkipUrlTamperProofWhiteList();
}
