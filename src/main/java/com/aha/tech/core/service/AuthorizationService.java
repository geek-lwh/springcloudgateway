package com.aha.tech.core.service;

import com.aha.tech.core.model.dto.Params;
import com.aha.tech.core.model.entity.AuthenticationEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface AuthorizationService {

    /**
     * 校验访客信息
     * @param accessToken
     * @return
     */
    AuthenticationEntity verifyVisitor(String accessToken);

    /**
     * 校验用户信息
     * @param accessToken
     * @return
     */
    AuthenticationEntity verifyUser(String accessToken);

    /**
     * 覆盖请求参数
     * @param serverHttpRequest
     * @param params
     * @return
     */
    ServerHttpRequest overwriteParams(ServerHttpRequest serverHttpRequest, Params params);

}
