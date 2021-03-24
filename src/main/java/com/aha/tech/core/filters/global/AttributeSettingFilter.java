package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.constant.SystemConstant;
import com.aha.tech.core.model.entity.PairEntity;
import com.aha.tech.core.model.entity.SnapshotRequestEntity;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
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
        ServerHttpRequest request = exchange.getRequest();

        String originalApi = request.getURI().getRawPath();
        HttpHeaders httpHeaders = request.getHeaders();

        SnapshotRequestEntity snapshotRequestEntity = AttributeSupport.getSnapshotRequest(exchange);
        snapshotRequestEntity.setRequestLine(exchange.getRequest().getURI());
        snapshotRequestEntity.setOriginalRequestHttpHeaders(request.getHeaders());

        // 是否跳过授权
        Boolean skipAuth = httpRequestHandlerService.isSkipAuth(originalApi);

        // 是否跳过url防篡改
        Boolean skipUrlTamperProof = httpRequestHandlerService.isSkipUrlTamperProof(originalApi, httpHeaders);

        // ip
        String ip = parseHeaderIp(httpHeaders);
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }

        Boolean skipIpLimiter = httpRequestHandlerService.isSkipIpLimiter(originalApi, ip);

        Boolean ignoreEmptyKidMapping = httpRequestHandlerService.isIgnoreEmptyKidMapping(originalApi);

        // 获取版本号和os
        PairEntity pair = parsingAgent(httpHeaders);
        String os = pair.getFirstEntity().toString();
        String version = pair.getSecondEntity().toString();

        // 判断新老版本
        Boolean oldVersion = Boolean.FALSE;
        int var = VersionSupport.compareVersion(version, SystemConstant.FIX_AHA_KID_USER_AGENT_VERSION);
        if (var == -1) {
            oldVersion = Boolean.TRUE;
        }

        // 如果孩子账户被删除
        Boolean needUpgrade = Boolean.FALSE;
        int var2 = VersionSupport.compareVersion(version, SystemConstant.COMPATIBILITY_5300_VERSION);
        if (var2 == -1) {
            needUpgrade = Boolean.TRUE;
        }

        AttributeSupport.put(exchange, span, IS_SKIP_AUTH_ATTR, skipAuth);
        AttributeSupport.put(exchange, span, IS_IGNORE_EMPTY_KID_MAPPING_ATTR, ignoreEmptyKidMapping);
        AttributeSupport.put(exchange, span, IS_SKIP_URL_TAMPER_PROOF_ATTR, skipUrlTamperProof);
        AttributeSupport.put(exchange, span, IS_SKIP_IP_LIMITER_ATTR, skipIpLimiter);
        AttributeSupport.put(exchange, span, REQUEST_IP_ATTR, ip);
        AttributeSupport.put(exchange, span, IS_OLD_VERSION_ATTR, oldVersion);
        AttributeSupport.put(exchange, span, IS_NEED_UPGRADE_ATTR, needUpgrade);
        AttributeSupport.put(exchange, span, APP_OS_ATTR, os);
        AttributeSupport.put(exchange, span, APP_VERSION_ATTR, version);
        AttributeSupport.put(exchange, GATEWAY_SNAPSHOT_REQUEST_ATTR, snapshotRequestEntity);
    }

    /**
     * 根据agent解析os和version
     * @param httpHeaders
     * @return
     */
    private PairEntity parsingAgent(HttpHeaders httpHeaders) {
        String os = SystemConstant.WEB_CLIENT;
        String version = SystemConstant.DEFAULT_VERSION;
        List<String> agentList = httpHeaders.getOrDefault(HeaderFieldConstant.HEADER_USER_AGENT, Collections.emptyList());
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
