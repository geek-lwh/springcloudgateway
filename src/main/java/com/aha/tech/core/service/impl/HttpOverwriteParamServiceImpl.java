package com.aha.tech.core.service.impl;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.service.OverwriteParamService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.DefaultServerRequest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 */
@Service("httpOverwriteParamService")
public class HttpOverwriteParamServiceImpl implements OverwriteParamService {

    private static final Logger logger = LoggerFactory.getLogger(HttpOverwriteParamServiceImpl.class);

    private static final String USER_ID_FIELD = "user_id";

    /**
     * 修改POST请求参数
     * @param requestAddParamsDto
     * @param chain
     * @param exchange
     * @return
     */
    @Override
    public Mono<Void> modifyRequestBody(RequestAddParamsDto requestAddParamsDto, GatewayFilterChain chain, ServerWebExchange exchange) {
        ServerRequest serverRequest = new DefaultServerRequest(exchange);
        AtomicInteger length = new AtomicInteger();
        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class).flatMap(body -> {
            JSONObject obj = JSON.parseObject(body);
            obj.put(USER_ID_FIELD, requestAddParamsDto.getUserId());
            String newBody = obj.toJSONString();
            length.set(newBody.length());
            return Mono.just(newBody);
        });

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());

        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
            ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                public HttpHeaders getHeaders() {
                    long contentLength = length.get();
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.putAll(super.getHeaders());
                    if (contentLength > 0) {
                        httpHeaders.setContentLength(contentLength);
                    } else {
                        httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                    }
                    return httpHeaders;
                }

                @Override
                public Flux<DataBuffer> getBody() {
                    return outputMessage.getBody();
                }
            };

            return chain.filter(exchange.mutate().request(decorator).build());
        }));
    }

    /**
     * 修改GET请求的参数
     * @param requestAddParamsDto
     * @param uri
     * @return
     */
    @Override
    public URI modifyQueryParams(RequestAddParamsDto requestAddParamsDto, URI uri) {
        URI newURI = UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam(USER_ID_FIELD, requestAddParamsDto.getUserId())
                .build(true)
                .toUri();
        logger.debug("修改queryParams后,新的uri : {}", newURI);
        return newURI;
    }

    /**
     * 修改mediaType=application/x-www-form-urlencoded的请求参数
     * @param requestAddParamsDto
     * @param uri
     * @return
     */
    @Override
    public URI modifyParamsWithFormUrlencoded(RequestAddParamsDto requestAddParamsDto, URI uri) {
        return this.modifyQueryParams(requestAddParamsDto, uri);
    }
}
