package com.aha.tech.core.service;

import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 */
public interface OverwriteParamService {

    /**
     * 修改POST请求的参数
     * @param userVo
     * @param chain
     * @param exchange
     * @return
     */
    Mono<Void> modifyRequestBody(UserVo userVo, GatewayFilterChain chain, ServerWebExchange exchange);

    /**
     * 修改GET请求的参数
     * @param userVo
     * @param uri
     * @return
     */
    URI modifyQueryParams(UserVo userVo, URI uri);
}
