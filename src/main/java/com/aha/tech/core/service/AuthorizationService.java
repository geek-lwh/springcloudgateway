package com.aha.tech.core.service;

import com.aha.tech.core.model.entity.AuthenticationEntity;
import com.aha.tech.core.model.entity.ParamsEntity;
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
    AuthenticationEntity verifyVisitorAccessToken(String accessToken);

    /**
     * 校验访客请求资源是否在白名单中
     * @param id
     * @param path
     * @return
     */
    Boolean verifyVisitorExistWhiteList(String id,String path);

    /**
     * 校验用户信息
     * @param accessToken
     * @return
     */
    AuthenticationEntity verifyUser(String accessToken);

    /**
     * 覆盖请求参数
     * @param serverHttpRequest
     * @param paramsEntity
     * @return
     */
    ServerHttpRequest overwriteParams(ServerHttpRequest serverHttpRequest, ParamsEntity paramsEntity);

}
