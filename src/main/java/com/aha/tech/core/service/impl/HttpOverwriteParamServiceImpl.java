package com.aha.tech.core.service.impl;

import com.aha.tech.core.model.dto.RequestAddParamsDto;
import com.aha.tech.core.service.OverwriteParamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.CachedBodyOutputMessage;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

import static com.aha.tech.passportserver.facade.constants.AuthorizationServerConstants.ANONYMOUS_KID_ID;
import static com.aha.tech.passportserver.facade.constants.AuthorizationServerConstants.ANONYMOUS_USER_ID;

/**
 * @Author: luweihong
 * @Date: 2019/3/27
 * 重写参数业务类
 */
@Service("httpOverwriteParamService")
public class HttpOverwriteParamServiceImpl implements OverwriteParamService {

    private static final Logger logger = LoggerFactory.getLogger(HttpOverwriteParamServiceImpl.class);

    private static final String USER_ID_FIELD = "user_id";

    private static final String KID_ID_FIELD = "kid_id";

//    private static final String SPECIAL_SYMBOL = "[]";

    /**
     * 修改POST请求参数
     * @param body
     * @param chain
     * @param exchange
     * @return
     */
    @Override
    public Mono<Void> rebuildRequestBody(String body, GatewayFilterChain chain, ServerWebExchange exchange, URI uri) {
        Mono<String> modifiedBody = Mono.just(body);

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        headers.remove(HttpHeaders.CONTENT_LENGTH);

        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);

        return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
            ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
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

                @Override
                public URI getURI() {
                    return uri;
                }
            };

            return chain.filter(exchange.mutate().request(decorator).build());
        }));
    }

    /**
     * 修改GET请求的参数
     * 兼容php get请求 传递数组问题 a[]=1&a[]=
     * @param requestAddParamsDto
     * @param request
     * @return
     */
    @Override
    public URI modifyQueryParams(RequestAddParamsDto requestAddParamsDto, ServerHttpRequest request) {
        URI uri = request.getURI();

        if (requestAddParamsDto == null || requestAddParamsDto.getUserId() == null) {
            logger.warn("缺失user_id,默认为游客访问");
            requestAddParamsDto.setUserId(ANONYMOUS_USER_ID);
        }

        if (requestAddParamsDto == null || requestAddParamsDto.getKidId() == null) {
            logger.warn("user_id : {} 缺少kid_id,默认设置为0", requestAddParamsDto.getUserId());
            requestAddParamsDto.setKidId(ANONYMOUS_KID_ID);
        }

        URI newURI = UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam(USER_ID_FIELD, requestAddParamsDto.getUserId())
                .replaceQueryParam(KID_ID_FIELD, requestAddParamsDto.getKidId())
                .build(true)
                .toUri();

        logger.debug("修改queryParams后,新的uri : {}", newURI);
        return newURI;
    }

    /**
     * 修改mediaType=application/x-www-form-urlencoded的请求参数
     * @param requestAddParamsDto
     * @param request
     * @return
     */
    @Override
    public URI modifyParamsWithFormUrlencoded(RequestAddParamsDto requestAddParamsDto, ServerHttpRequest request) {
        return this.modifyQueryParams(requestAddParamsDto, request);
    }
}
