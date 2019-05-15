package com.aha.tech.core.service;

import com.aha.tech.core.model.entity.AuthenticationEntity;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface AuthorizationService {

    /**
     * 是否跳过授权
     * @param path
     * @return
     */
    Boolean isSkipAuth(String path);
    /**
     * 校验访客信息
     * @param accessToken
     * @return
     */
    AuthenticationEntity verifyVisitorAccessToken(String accessToken);

    /**
     * 校验用户信息
     * @param accessToken
     * @return
     */
    AuthenticationEntity verifyUser(String accessToken);

}
