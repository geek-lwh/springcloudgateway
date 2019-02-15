package com.aha.tech.core.filters;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 */
@Component
public class AuthCheckGatewayFilterFactory implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayFilter.class);

    @Autowired(required = false)
    private PassportResource passportResource;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        List<String> tokenList = requestHeaders.get("token");
        if (CollectionUtils.isEmpty(tokenList)) {
            return null;
        }

        String accessToken = tokenList.get(0);
        RpcResponse<UserVo> response = passportResource.verify(accessToken);
        int code = response.getCode();
        if (code != SUCCESS) {
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                byte[] bytes = JSON.toJSONString(response).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
//                        response.getHeaders().set("aaa", "bbb");

                return resp.writeWith(Flux.just(buffer));
            });
        }
            UserVo userVo = response.getData();
//            return chain.filter(exchange).then(
//                    Mono.fromRunnable(() -> {
//                        logger.info("user info is : {}", userVo);
//                    })
//            );

            return chain.filter(exchange);
        }

    }
