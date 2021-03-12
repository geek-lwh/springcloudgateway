package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.constant.SystemConstant;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.AttributeSupport;
import com.aha.tech.core.support.VersionSupport;
import com.aha.tech.util.TagsUtil;
import com.aha.tech.util.TraceUtil;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.apache.commons.lang3.StringUtils;
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

import static com.aha.tech.core.constant.AttributeConstant.*;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.ATTRIBUTE_SETTING_FILTER_ORDER;
import static com.aha.tech.core.support.ParseHeadersSupport.parseHeaderIp;

/**
 * @Author: luweihong
 * @Date: 2019/4/10
 *
 * 过滤器入口
 */
@Component
public class AttributeSettingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AttributeSettingFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Resource
    private Tracer tracer;

    @Override
    public int getOrder() {
        return ATTRIBUTE_SETTING_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = TraceUtil.start(exchange, this.getClass().getSimpleName());
        try (Scope scope = tracer.scopeManager().activate(span)) {
            setting(exchange, span);
            return chain.filter(exchange);
        } catch (Exception e) {
            TagsUtil.setCapturedErrorsTags(e, span);
            throw e;
        } finally {
            span.finish();
        }
    }

    /**
     * 设置属性
     * @param exchange
     * @param span
     */
    private void setting(ServerWebExchange exchange, Span span) {
        String rawPath = exchange.getRequest().getURI().getRawPath();
        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();
        // 是否跳过授权
        Boolean isSkipAuth = httpRequestHandlerService.isSkipAuth(rawPath);

        // 是否跳过url防篡改
        Boolean isSkipUrlTamperProof = httpRequestHandlerService.isSkipUrlTamperProof(rawPath, httpHeaders);

        // ip
        String ip = parseHeaderIp(exchange.getRequest().getHeaders());
        if (StringUtils.isBlank(ip)) {
            ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        Boolean isSkipIpLimiter = httpRequestHandlerService.isSkipIpLimiter(rawPath, ip);

        // 获取版本号和os
        PairEntity pair = parsingAgent(exchange);
        String os = pair.getFirstEntity().toString();
        String version = pair.getSecondEntity().toString();

        // 判断新老版本
        Boolean isOld = Boolean.FALSE;
        int result = VersionSupport.compareVersion(version, SystemConstant.DIVIDING_LINE_OF_VERSION);
        if (result == -1) {
            isOld = Boolean.TRUE;
        }

        AttributeSupport.put(exchange, span, IS_SKIP_AUTH_ATTR, isSkipAuth);
        AttributeSupport.put(exchange, span, IS_SKIP_URL_TAMPER_PROOF_ATTR, isSkipUrlTamperProof);
        AttributeSupport.put(exchange, span, IS_SKIP_IP_LIMITER_ATTR, isSkipIpLimiter);
        AttributeSupport.put(exchange, span, REQUEST_IP_ATTR, ip);
        AttributeSupport.put(exchange, span, IS_OLD_VERSION_ATTR, isOld);
        AttributeSupport.put(exchange, span, APP_OS_ATTR, os);
        AttributeSupport.put(exchange, span, APP_VERSION_ATTR, version);
    }

    /**
     * 根据agent解析os和version
     * @param exchange
     * @return
     */
    private PairEntity parsingAgent(ServerWebExchange exchange) {
        String os = SystemConstant.WEB_CLIENT;
        String version = SystemConstant.DEFAULT_VERSION;
        List<String> agentList = exchange.getRequest().getHeaders().getOrDefault(HeaderFieldConstant.HEADER_USER_AGENT, Collections.emptyList());
        if (!CollectionUtils.isEmpty(agentList)) {
            String agent = agentList.get(0);
            // parse agent and version less than 6.1.6 is old version
            String[] tmp = VersionSupport.parseOsAndVersion(agent);
            os = tmp[0];
            version = tmp[1];

        }

        return new PairEntity(os, version);
    }

}