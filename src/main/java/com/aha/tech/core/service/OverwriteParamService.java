package com.aha.tech.core.service;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
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
     * @param requestAddParamsDto
     * @param chain
     * @param exchange
     * @return
     */
    Mono<Void> modifyRequestBody(RequestAddParamsDto requestAddParamsDto, GatewayFilterChain chain, ServerWebExchange exchange);

    /**
     * 修改GET请求的参数
     * @param requestAddParamsDto
     * @param uri
     * @return
     */
    URI modifyQueryParams(RequestAddParamsDto requestAddParamsDto, URI uri);

}
