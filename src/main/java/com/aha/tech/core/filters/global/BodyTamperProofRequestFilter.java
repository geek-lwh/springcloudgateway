package com.aha.tech.core.filters.global;

import com.aha.tech.core.controller.FallBackController;
import com.aha.tech.core.model.entity.CacheRequestEntity;
import com.aha.tech.core.model.entity.TamperProofEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.util.LogUtils;
import com.aha.tech.util.TracerUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import static com.aha.tech.core.constant.AttributeConstant.HTTP_STATUS;
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

    @Resource
    private Tracer tracer;

    @Override
    public int getOrder() {
        return BODY_TAMPER_PROOF_FILTER;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Span span = TracerUtils.startAndRef(exchange, this.getClass().getName());
        ExchangeSupport.setSpan(exchange, span);
        try (Scope scope = tracer.scopeManager().activate(span)) {
            TracerUtils.setClue(span, exchange);
            Boolean isVialdBody = verifyBody(exchange);
            if (!isVialdBody) {
                ExchangeSupport.setHttpStatus(exchange, HttpStatus.FORBIDDEN);
                span.setTag(HTTP_STATUS, HttpStatus.FORBIDDEN.value());
                Tags.ERROR.set(span, true);
                return Mono.defer(() -> {
                    ResponseVo rpcResponse = new ResponseVo(HttpStatus.FORBIDDEN.value(), FallBackController.DEFAULT_SYSTEM_ERROR);
                    return ResponseSupport.write(exchange, HttpStatus.FORBIDDEN, rpcResponse);
                });
            }

            return chain.filter(exchange);
        } catch (Exception e) {
            TracerUtils.logError(e);
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
    private Boolean verifyBody(ServerWebExchange exchange) {
        LogUtils.combineLog(exchange);
        CacheRequestEntity cacheRequestEntity = ExchangeSupport.getCacheRequest(exchange);
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod httpMethod = request.getMethod();
        if (!httpMethod.equals(HttpMethod.POST)) {
            return Boolean.TRUE;
        }
        URI uri = request.getURI();
        Boolean isSkipUrlTamperProof = ExchangeSupport.getIsSkipUrlTamperProof(exchange);
        if (isSkipUrlTamperProof) {
            logger.info("跳过body防篡改,raw_path : {}", uri.getRawPath());
            return Boolean.TRUE;
        }

        HttpHeaders httpHeaders = request.getHeaders();
        MediaType mediaType = httpHeaders.getContentType();
        if (mediaType != null && mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) {
            logger.info("media type不是json : {}", mediaType);
            return Boolean.TRUE;
        }

        TamperProofEntity tamperProofEntity = new TamperProofEntity(httpHeaders, uri);
        String body = cacheRequestEntity.getRequestBody();

        return bodyTamperProof(body, tamperProofEntity);
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
