package com.aha.tech.core.filters.global;

import com.aha.tech.core.controller.FallBackController;
import com.aha.tech.core.model.entity.TamperProofEntity;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.service.RequestHandlerService;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.aha.tech.core.support.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;

import static com.aha.tech.core.constant.FilterProcessOrderedConstant.URL_TAMPER_PROOF_FILTER;

/**
 * @Author: luweihong
 * @Date: 2019/4/8
 *
 * 预处理过滤器
 */
@Component
public class UrlTamperProofRequestFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(UrlTamperProofRequestFilter.class);

    @Resource
    private RequestHandlerService httpRequestHandlerService;

    @Value("${gateway.tamper.proof.enable:false}")
    private boolean isEnable;

    @Override
    public int getOrder() {
        return URL_TAMPER_PROOF_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        String rawPath = uri.getRawPath();
        if (ExchangeSupport.getIsSkipUrlTamperProof(exchange)) {
            logger.info("跳过url防篡改 : {}", rawPath);
            return chain.filter(exchange);
        }

        HttpHeaders httpHeaders = request.getHeaders();
        TamperProofEntity tamperProofEntity = new TamperProofEntity(httpHeaders, uri);
        boolean isURIValid = urlTamperProof(tamperProofEntity, uri.getRawQuery(), rawPath);
        if (!isURIValid) {
            return Mono.defer(() -> {
                String errorMsg = String.format("url防篡改校验失败,参数:%s", tamperProofEntity);
                logger.error("{}", errorMsg);
                ResponseVo rpcResponse = new ResponseVo(HttpStatus.FORBIDDEN.value(), FallBackController.DEFAULT_SYSTEM_ERROR);
                return ResponseSupport.write(exchange, HttpStatus.FORBIDDEN,rpcResponse);
            });
        }

        return chain.filter(exchange);
    }

    /**
     * 校验url
     * @param tamperProofEntity
     * @param rawQuery
     * @param rawPath
     * @return
     */
    private boolean urlTamperProof(TamperProofEntity tamperProofEntity, String rawQuery, String rawPath) {
        if (!isEnable) {
            return Boolean.TRUE;
        }

        logger.debug("原始请求地址 : {} , 加密信息 :{} ", rawPath, tamperProofEntity);
        String timestamp = tamperProofEntity.getTimestamp();
        String signature = tamperProofEntity.getSignature();
        String version = tamperProofEntity.getVersion();
        MultiValueMap<String, String> queryParams = URISupport.initQueryParams(rawQuery);
        String sortQueryParams = URISupport.queryParamsSort(queryParams);
        logger.debug("sort query params : {}", sortQueryParams);
        return httpRequestHandlerService.urlTamperProof(version, timestamp, signature, rawPath, sortQueryParams);
    }

}
