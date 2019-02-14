package com.aha.tech.filters;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.controller.resource.PassportResource;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 */
@Component
public class AuthCheckGatewayFilterFactory implements GlobalFilter, Ordered, GatewayFilter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayFilter.class);

    @Autowired(required = false)
    private PassportResource passportResource;

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
            return null;
        }

        UserVo userVo = response.getData();
        return chain.filter(exchange).then(
                Mono.fromRunnable(() -> {
                    logger.info("user info is : {}", userVo);
                })
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
