package com.aha.tech.core.service.impl;

import com.aha.tech.core.service.OverwriteParamService;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 */
@Service("overwriteParamService")
public class OverwriteParamServiceImpl implements OverwriteParamService {

    private static final String USER_ID_FIELD = "user_id";

    /**
     * 修改POST请求参数
     * @param userVo
     * @param chain
     * @param exchange
     * @return
     */
    @Override
    public Mono<Void> modifyRequestBody(UserVo userVo, GatewayFilterChain chain, ServerWebExchange exchange) {
        ServerRequest serverRequest = new DefaultServerRequest(exchange);
        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class);
        modifiedBody.flatMap(body -> {
            JSONObject obj = JSON.parseObject(body);
            obj.put(USER_ID_FIELD, userVo.getUserId());
            return Mono.just(obj.toJSONString());
        });

        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());

        headers.remove(HttpHeaders.CONTENT_LENGTH);

        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        return bodyInserter.insert(outputMessage, new BodyInserterContext())
                .then(Mono.defer(() -> {
                    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(
                            exchange.getRequest()) {
                        @Override
                        public HttpHeaders getHeaders() {
                            long contentLength = headers.getContentLength();
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
     * @param userVo
     * @param uri
     * @return
     */
    @Override
    public URI modifyQueryParams(UserVo userVo, URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam(USER_ID_FIELD, userVo.getUserId())
                .build(true)
                .toUri();
    }
}
