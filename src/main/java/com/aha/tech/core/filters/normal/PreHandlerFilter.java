package com.aha.tech.core.filters.normal;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.constant.SystemConstant;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.VersionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.*;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.PRE_HANDLER_FILTER_ORDER;

/**
 * @Author: luweihong
 * @Date: 2019/4/10
 *
 * 访问日志打印过滤器
 */
@Component
public class PreHandlerFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(PreHandlerFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Override
    public int getOrder() {
        return PRE_HANDLER_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
        // 是否跳过授权
        Boolean isSkipAuth = httpRequestHandlerService.isSkipAuth(rawPath);

        // 是否跳过url防篡改
        Boolean isSkipUrlTamperProof = httpRequestHandlerService.isSkipUrlTamperProof(rawPath, httpHeaders);
        // 获取版本号 判断新老版本
        String os = SystemConstant.WEB_CLIENT;
        String version = SystemConstant.DEFAULT_VERSION;
        Boolean isOldVersion = Boolean.FALSE;
        List<String> agentList = exchange.getRequest().getHeaders().getOrDefault(HeaderFieldConstant.HEADER_USER_AGENT, Collections.emptyList());
        if (!CollectionUtils.isEmpty(agentList)) {
            String agent = agentList.get(0);
            // parse agent and version less than 6.1.6 is old version
            String[] tmp = VersionSupport.parseOsAndVersion(agent);
            os = tmp[0];
            version = tmp[1];
            int result = VersionSupport.compareVersion(tmp[1], SystemConstant.CURRENT_VERSION);
            if (result == -1) {
                isOldVersion = Boolean.TRUE;
            }
        }

        ExchangeSupport.put(exchange, IS_SKIP_AUTH_ATTR, isSkipAuth);
        ExchangeSupport.put(exchange, IS_SKIP_URL_TAMPER_PROOF_ATTR, isSkipUrlTamperProof);
        ExchangeSupport.put(exchange, IS_OLD_VERSION_ATTR, isOldVersion);
        ExchangeSupport.put(exchange, APP_OS_ATTR, os);
        ExchangeSupport.put(exchange, APP_VERSION_ATTR, version);

        return chain.filter(exchange);
    }

}
