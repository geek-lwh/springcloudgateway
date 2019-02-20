package com.aha.tech.core.filters.global;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.constant.FilterOrdered;
import com.aha.tech.core.entity.GlobalResponseVo;
import com.aha.tech.core.exception.EmptyBodyException;
import com.aha.tech.core.handler.SessionHandler;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 *
 * 调用授权系统解析用户主键
 * 修改body请求体
 */
@Component
public class AddRequestBodyGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AddRequestBodyGatewayFilter.class);

    @Override
    public int getOrder() {
        return FilterOrdered.GLOBAL_ADD_REQUEST_BODY_GATEWAY_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行添加post参数过滤器");
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpMethod httpMethod = serverHttpRequest.getMethod();
        URI uri = serverHttpRequest.getURI();

        UserVo userVo = SessionHandler.get();
        if (httpMethod != HttpMethod.POST || userVo == null) {
            logger.info("不满足 执行添加post参数过滤器 要求,url : {},httpMethod : {} ", uri, httpMethod);
            return chain.filter(exchange);
        }

        String resolveBody = resolveBodyFromRequest(serverHttpRequest);

        if (StringUtils.isBlank(resolveBody)) {
            throw new EmptyBodyException();
        }

        URI newUri = UriComponentsBuilder.fromUri(serverHttpRequest.getURI()).build(true).toUri();
        ServerHttpRequest newRequest;
        try {
            newRequest = addRequestBody(resolveBody, userVo, serverHttpRequest, serverHttpRequest.mutate().uri(newUri).build());
        } catch (JSONException e) {
            logger.error("date : {} mutate new request has error", DateUtil.currentDateByDefaultFormat(), e);
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                GlobalResponseVo globalResponseVo = GlobalResponseVo.globalBaseErrorTemplate(110, e.toString(), uri.toString());
                byte[] bytes = JSON.toJSONString(globalResponseVo).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

                return resp.writeWith(Flux.just(buffer));
            });
        }

        return chain.filter(exchange.mutate().request(newRequest).build());
    }


    /**
     * 从request对象中解析body,DataBuffer 转 String
     * @param serverHttpRequest
     * @return
     */
    private String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest) {
        Flux<DataBuffer> body = serverHttpRequest.getBody();

        AtomicReference<String> bodyRef = new AtomicReference<>();
        body.subscribe(buffer -> {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
            DataBufferUtils.release(buffer);
            bodyRef.set(charBuffer.toString());
        });

        return bodyRef.get();
    }

    /**
     * 构建DataBuffer
     * @param value
     * @return
     */
    private DataBuffer stringBuffer(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
        DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
        buffer.write(bytes);
        return buffer;
    }

    private static <K, V> void copyMultiValueMap(MultiValueMap<K, V> source, MultiValueMap<K, V> target) {
        source.forEach((key, value) -> target.put(key, new LinkedList<>(value)));
    }

    /**
     * 添加request body
     * @param resolveBody
     * @param userVo
     * @param serverHttpRequest
     * @param newRequest
     * @return
     * @throws JSONException
     */
    private ServerHttpRequest addRequestBody(String resolveBody, UserVo userVo, ServerHttpRequest serverHttpRequest, ServerHttpRequest newRequest) throws JSONException {
        JSONObject obj = new JSONObject(resolveBody);
        obj.put("user_id", userVo.getUserId());
        DataBuffer bodyDataBuffer = stringBuffer(obj.toString());
        Flux<DataBuffer> bodyFlux = Flux.just(bodyDataBuffer);

        HttpHeaders myHeaders = new HttpHeaders();
        copyMultiValueMap(serverHttpRequest.getHeaders(), myHeaders);
        myHeaders.remove(HttpHeaders.CONTENT_LENGTH);
        int len = bodyDataBuffer.readableByteCount();
        myHeaders.set(HttpHeaders.CONTENT_LENGTH, String.valueOf(len));
        newRequest = new ServerHttpRequestDecorator(newRequest) {
            @Override
            public Flux<DataBuffer> getBody() {
                return bodyFlux;
            }

            @Override
            public HttpHeaders getHeaders() {
                return myHeaders;
            }
        };

        return newRequest;
    }
}
