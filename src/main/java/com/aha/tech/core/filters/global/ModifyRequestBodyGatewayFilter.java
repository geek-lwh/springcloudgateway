package com.aha.tech.core.filters.global;

import com.aha.tech.core.constant.FilterOrderedConstant;
import com.aha.tech.core.exception.EmptyBodyException;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static com.aha.tech.core.constant.ExchangeAttributeConstant.USER_INFO_SESSION;
import static com.aha.tech.core.tools.BeanUtil.copyMultiValueMap;

/**
 * @Author: luweihong
 * @Date: 2019/2/20
 *
 * 调用授权系统解析用户主键
 * 修改body请求体
 */
@Component
public class ModifyRequestBodyGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestBodyGatewayFilter.class);

    private static final String USER_ID_FIELD = "user_id";

    @Override
    public int getOrder() {
        return FilterOrderedConstant.GLOBAL_ADD_REQUEST_BODY_GATEWAY_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.debug("执行添加post参数过滤器");

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpMethod httpMethod = serverHttpRequest.getMethod();
        URI uri = serverHttpRequest.getURI();

        Object obj = exchange.getAttributes().get(USER_INFO_SESSION);
        if (obj == null || httpMethod != HttpMethod.POST) {
            logger.info("请求路径: {} 跳过过授权", uri.getRawPath());
            return chain.filter(exchange);
        }

        UserVo userVo = (UserVo) obj;
        ServerHttpRequest newRequest = modifyRequestBody(serverHttpRequest, userVo);
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    /**
     * 构建新的请求
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest modifyRequestBody(ServerHttpRequest serverHttpRequest, UserVo userVo) {
        String resolveBody = resolveBodyFromRequest(serverHttpRequest);
        if (StringUtils.isBlank(resolveBody)) {
            throw new EmptyBodyException();
        }

        ServerHttpRequest newRequest = addRequestBody(resolveBody, userVo, serverHttpRequest);

        return newRequest;
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


    /**
     * 添加request body
     * @param resolveBody
     * @param userVo
     * @param serverHttpRequest
     * @return
     */
    private ServerHttpRequest addRequestBody(String resolveBody, UserVo userVo, ServerHttpRequest serverHttpRequest) {
        JSONObject obj = JSON.parseObject(resolveBody);
        obj.put(USER_ID_FIELD, userVo.getUserId());
        DataBuffer bodyDataBuffer = stringBuffer(obj.toString());
        Flux<DataBuffer> bodyFlux = Flux.just(bodyDataBuffer);

        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();

        URI newUri = UriComponentsBuilder.fromUri(serverHttpRequest.getURI()).build(true).toUri();
        ServerHttpRequest newRequest = serverHttpRequest.mutate().uri(newUri).build();

        // 插入后计算新的content length,否则会出现异常
        HttpHeaders myHeaders = new HttpHeaders();
        copyMultiValueMap(httpHeaders, myHeaders);
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
