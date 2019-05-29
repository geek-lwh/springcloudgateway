package com.aha.tech.core.service;

import com.aha.tech.core.model.entity.AuthenticationResultEntity;
import org.springframework.web.server.ServerWebExchange;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface AuthorizationService {

    /**
     * 校验访客信息
     * @param swe
     * @param accessToken
     * @return
     */
    AuthenticationResultEntity verifyVisitorAccessToken(ServerWebExchange swe, String accessToken);

    /**
     * 校验用户信息
     * @param swe
     * @param accessToken
     * @return
     */
    AuthenticationResultEntity verifyUser(ServerWebExchange swe, String accessToken);

}
