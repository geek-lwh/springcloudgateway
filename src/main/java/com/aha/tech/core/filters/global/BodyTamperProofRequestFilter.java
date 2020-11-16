package com.aha.tech.core.filters.global;

import com.aha.tech.core.controller.FallBackController;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.entity.TamperProofEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.TRACE_LOG_ID;
import static com.aha.tech.core.constant.FilterProcessOrderedConstant.BODY_TAMPER_PROOF_FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 预处理过滤器
 */
@Component
public class BodyTamperProofRequestFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(BodyTamperProofRequestFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Value("${gateway.tamper.proof.enable:false}")
    private boolean isEnable;

    @Override
    public int getOrder() {
        return BODY_TAMPER_PROOF_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Tracer tracer = GlobalTracer.get();
        Tracer.SpanBuilder spanBuilder = GlobalTracer.get().buildSpan(this.getClass().getName());

        Span parentSpan = ExchangeSupport.getSpan(exchange);
        Span span = spanBuilder.asChildOf(parentSpan).start();
        ExchangeSupport.setSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span);
            ExchangeSupport.put(exchange, TRACE_LOG_ID, span.context().toTraceId());
            return getResult(exchange, chain);
        } catch (Exception e) {
            TracerUtils.reportErrorTrace(e);
            throw e;
        } finally {
            span.finish();
        }
    }

    /**
     * 获取body防篡改结果
     * @param exchange
     * @param chain
     * @return
     */
    private Mono<Void> getResult(ServerWebExchange exchange, GatewayFilterChain chain) {
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod httpMethod = request.getMethod();
        if (!httpMethod.equals(HttpMethod.POST)) {
            return chain.filter(exchange);
        }
        URI uri = request.getURI();
        Boolean isSkipUrlTamperProof = ExchangeSupport.getIsSkipUrlTamperProof(exchange);
        String traceId = exchange.getAttributeOrDefault(TRACE_LOG_ID, "MISS_TRACE_ID");
        MDC.put("traceId", traceId);

        if (isSkipUrlTamperProof) {
            logger.info("跳过body防篡改,raw_path : {}", uri.getRawPath());
            return chain.filter(exchange);
        }

        HttpHeaders httpHeaders = request.getHeaders();
        MediaType mediaType = httpHeaders.getContentType();
        if (mediaType != null && mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
            logger.info("media type不是json : {}", mediaType);
            return chain.filter(exchange);
        }

        TamperProofEntity tamperProofEntity = new TamperProofEntity(httpHeaders, uri);
        String body = cacheRequestEntity.getRequestBody();
        if (!bodyTamperProof(body, tamperProofEntity)) {
            return Mono.defer(() -> {
                logger.warn("uri: {}} ,params : {}} ,body : {}} ,防篡改校验失败,参数:{}}", uri.getRawPath(), uri.getRawQuery(), body, tamperProofEntity);
                ResponseVo rpcResponse = new ResponseVo(HttpStatus.FORBIDDEN.value(), FallBackController.DEFAULT_SYSTEM_ERROR);
                return ResponseSupport.write(exchange, HttpStatus.FORBIDDEN, rpcResponse);
            });
        }

        return chain.filter(exchange);
    }

    /**
     * 检查body是否合法
     * @param body
     * @param tamperProofEntity
     * @return
     */
    private Boolean bodyTamperProof(String body, TamperProofEntity tamperProofEntity) {
        if (isEnable) {
            String version = tamperProofEntity.getVersion();
            String content = tamperProofEntity.getContent();
            String timestamp = tamperProofEntity.getTimestamp();
            logger.debug("接收到的 body 防篡改参数 : {}", tamperProofEntity);
            return httpRequestHandlerService.bodyTamperProof(version, body, timestamp, content);
        }

        return Boolean.TRUE;
    }
}
