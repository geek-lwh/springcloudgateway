package com.aha.tech.core.filters;

import com.aha.tech.commons.response.RpcResponse;
import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.controller.resource.PassportResource;
import com.aha.tech.core.entity.GlobalResponseVo;
import com.aha.tech.passportserver.facade.model.vo.UserVo;
import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBufAllocator;
import javassist.tools.web.BadHttpRequest;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.AddRequestParameterGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.ws.rs.HttpMethod;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.aha.tech.commons.constants.ResponseConstants.SUCCESS;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setResponseStatus;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 *
 * need to modify the header Content-Length, If you don't do this, the body may be truncated after you have modify the request body and the body becomes longer
 */
@Component
public class AuthGatewayFilterFactory implements GlobalFilter,Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthGatewayFilterFactory.class);

    private static final String ACCESS_TOKEN_HEADER = "token";

    @Autowired(required = false)
    private PassportResource passportResource;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isWhiteList()) {
            logger.info("path : {} 无需授权");
            return Mono.empty();
        }

        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
        List<String> headersOfToken = requestHeaders.get(ACCESS_TOKEN_HEADER);
        String accessToken = CollectionUtils.isEmpty(headersOfToken) ? StringUtils.EMPTY : headersOfToken.get(0);

        // 请求授权系统 验证access token 是否合法
        RpcResponse<UserVo> userVoRpcResponse = passportResource.verify(accessToken);
        if (userVoRpcResponse.getCode() != SUCCESS) {
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                byte[] bytes = JSON.toJSONString(userVoRpcResponse).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = resp.bufferFactory().wrap(bytes);
                return resp.writeWith(Flux.just(buffer));
            });
        }

        UserVo userVo = userVoRpcResponse.getData();

        ServerHttpRequest oldRequest = exchange.getRequest();
        ServerHttpRequest newRequest;

        // add userId
        try {
            newRequest = mutateNewRequest(oldRequest, userVo);
        } catch (Exception e) {
            // 构建上下文信息 新建实体
            logger.error("date : {} mutate new request has error", DateUtil.currentDateByDefaultFormat(), e);
            return Mono.defer(() -> {
                setResponseStatus(exchange, HttpStatus.UNAUTHORIZED);
                final ServerHttpResponse resp = exchange.getResponse();
                GlobalResponseVo globalResponseVo = GlobalResponseVo.globalBaseErrorTemplate(110, e.toString(), oldRequest.getURI().toString());
                byte[] bytes = JSON.toJSONString(globalResponseVo).getBytes(StandardCharsets.UTF_8);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

                return resp.writeWith(Flux.just(buffer));
            });
        }

        return chain.filter(exchange.mutate().request(newRequest).build());
//            return chain.filter(exchange);
    }

    /**
     * 基于原始的请求构建新的请求
     * @param serverHttpRequest
     * @param userVo
     * @return
     * @throws JSONException
     * @throws BadHttpRequest
     */
    private ServerHttpRequest mutateNewRequest(ServerHttpRequest serverHttpRequest, UserVo userVo) throws Exception {
        String method = serverHttpRequest.getMethodValue();
        return method.equals(HttpMethod.GET) ? mutateGetRequest(serverHttpRequest, userVo) : mutatePostRequest(serverHttpRequest, userVo);
    }

    /**
     * 基于原始的post请求构建新的post请求
     * @param serverHttpRequest
     * @param userVo
     * @return
     * @throws Exception
     */
    private ServerHttpRequest mutatePostRequest(ServerHttpRequest serverHttpRequest, UserVo userVo) throws Exception {
        String resolveBody = resolveBodyFromRequest(serverHttpRequest);

        if (StringUtils.isBlank(resolveBody)) {
            logger.error("request body is empty");
            throw new NullPointerException();
        }

        URI newUri = UriComponentsBuilder.fromUri(serverHttpRequest.getURI()).build(true).toUri();

        return addRequestBody(resolveBody, userVo, serverHttpRequest, serverHttpRequest.mutate().uri(newUri).build());
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


    /**
     * 基于原始的get请求构建新的get请求
     * todo get请求添加的user_id 无效
     * @param serverHttpRequest
     * @param userVo
     * @return
     * @throws Exception
     */
    private ServerHttpRequest mutateGetRequest(ServerHttpRequest serverHttpRequest, UserVo userVo) {
        URI uri = serverHttpRequest.getURI();

        String originalQuery = uri.getRawQuery();

        String query = addRequestParams(originalQuery,userVo);
        URI newUri = UriComponentsBuilder.fromUri(uri)
                .replaceQuery(query)
                .build(false)
                .toUri();

        return serverHttpRequest.mutate().uri(newUri).build();
    }

    /**
     * 添加get请求的query param
     * @param originalQuery
     * @param userVo
     * @return
     */
    private String addRequestParams(String originalQuery,UserVo userVo){
        StringBuilder query = new StringBuilder();
        if (org.springframework.util.StringUtils.hasText(originalQuery)) {
            query.append(originalQuery);
            if (originalQuery.charAt(originalQuery.length() - 1) != '&') {
                query.append('&');
            }
        }

        query.append("user_id").append("=").append(userVo.getUserId());

        return query.toString();
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
     * 白名单不用授权
     * @return
     */
    private static Boolean isWhiteList() {
        return Boolean.FALSE;
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
