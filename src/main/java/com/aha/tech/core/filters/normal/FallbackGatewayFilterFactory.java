package com.aha.tech.core.filters.normal;

import com.aha.tech.core.constant.SystemConstant;
import com.aha.tech.core.exception.GatewayException;
import com.aha.tech.core.model.vo.ResponseVo;
import com.aha.tech.core.support.ExchangeSupport;
import com.aha.tech.core.support.ResponseSupport;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Subscription;

import java.net.URI;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.aha.tech.core.constant.AttributeConstant.HTTP_STATUS;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;


/**
 * @Author: luweihong
 * @Date: 2019/4/11
 *  重写hystrixGatewayFilterFactory 自定义熔断结果
 */
@Component
public class FallbackGatewayFilterFactory extends AbstractGatewayFilterFactory<FallbackGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(FallbackGatewayFilterFactory.class);

    private final ObjectProvider<DispatcherHandler> dispatcherHandler;

    public FallbackGatewayFilterFactory(ObjectProvider<DispatcherHandler> dispatcherHandler) {
        super(FallbackGatewayFilterFactory.Config.class);
        this.dispatcherHandler = dispatcherHandler;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return singletonList(NAME_KEY);
    }

    public GatewayFilter apply(String routeId, Consumer<FallbackGatewayFilterFactory.Config> consumer) {
        FallbackGatewayFilterFactory.Config config = newConfig();
        consumer.accept(config);

        if (StringUtils.isEmpty(config.getName()) && !StringUtils.isEmpty(routeId)) {
            config.setName(routeId);
        }

        return apply(config);
    }

    @Override
    public GatewayFilter apply(FallbackGatewayFilterFactory.Config config) {
        if (config.setter == null) {
            Assert.notNull(config.name, "A name must be supplied for the Hystrix Command Key");
            HystrixCommandGroupKey groupKey = HystrixCommandGroupKey.Factory.asKey(getClass().getSimpleName());
            HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(config.name);

            config.setter = Setter.withGroupKey(groupKey)
                    .andCommandKey(commandKey);
        }

        return (exchange, chain) -> {
            FallbackGatewayFilterFactory.RouteHystrixCommand command = new FallbackGatewayFilterFactory.RouteHystrixCommand(config.setter, config.fallbackUri, exchange, chain);
            return Mono.create(s -> {
                Subscription sub = command.toObservable().subscribe(s::success, s::error, s::success);
                s.onCancel(sub::unsubscribe);
            }).onErrorResume((Function<Throwable, Mono<Void>>) throwable -> {
                if (throwable instanceof HystrixRuntimeException) {
                    HystrixRuntimeException e = (HystrixRuntimeException) throwable;
                    HystrixRuntimeException.FailureType failureType = e.getFailureType();
                    // 从exchange中获取真实错误内容
                    String message = exchange.getAttributes().getOrDefault(ServerWebExchangeUtils.HYSTRIX_EXECUTION_EXCEPTION_ATTR, e.getMessage()).toString();

                    switch (failureType) {
                        case SHORTCIRCUIT:
                            return shortCircuit(exchange, throwable, e, message);
                        case TIMEOUT:
                            return timeout(exchange, throwable, e, message);
                        case COMMAND_EXCEPTION:
                            return commandException(exchange, throwable, e, message);
                        default:
                            return otherError(exchange, throwable, e, message);
                    }
                }

                return Mono.error(throwable);
            }).then();
        };
    }

    /**
     * 未知错误
     * @param exchange
     * @param throwable
     * @param e
     * @param message
     * @return
     */
    private Mono<Void> otherError(ServerWebExchange exchange, Throwable throwable, HystrixRuntimeException e, String message) {
        logger.error("HYSTRIX FALL BACK EXCEPTION : {}", message, e);
        exchange.getAttributes().put(HTTP_STATUS, HttpStatus.BAD_REQUEST.value());
        ResponseVo responseVo = new ResponseVo(HttpStatus.BAD_REQUEST.value(), SystemConstant.DEFAULT_ERROR_MESSAGE);
        return ResponseSupport.interrupt(exchange, responseVo, HttpStatus.BAD_REQUEST, new GatewayException(throwable));
    }

    /**
     * 操作错误
     * @param exchange
     * @param throwable
     * @param e
     * @param message
     * @return
     */
    private Mono<Void> commandException(ServerWebExchange exchange, Throwable throwable, HystrixRuntimeException e, String message) {
        return Mono.defer(() -> {
            logger.error("COMMAND_EXCEPTION : {}", message, e);
            exchange.getAttributes().put(HTTP_STATUS, HttpStatus.BAD_REQUEST.value());
            ResponseVo responseVo = new ResponseVo(HttpStatus.BAD_REQUEST.value(), SystemConstant.DEFAULT_ERROR_MESSAGE);
            return ResponseSupport.interrupt(exchange, responseVo, HttpStatus.BAD_REQUEST, new GatewayException(throwable));
        });
    }

    /**
     * 超时
     * @param exchange
     * @param throwable
     * @param e
     * @param message
     * @return
     */
    private Mono<Void> timeout(ServerWebExchange exchange, Throwable throwable, HystrixRuntimeException e, String message) {
        return Mono.defer(() -> {
            logger.error("TIMEOUT : {}", message, e);
            exchange.getAttributes().put(HTTP_STATUS, HttpStatus.REQUEST_TIMEOUT.value());
            ResponseVo responseVo = new ResponseVo(HttpStatus.REQUEST_TIMEOUT.value(), SystemConstant.DEFAULT_ERROR_MESSAGE);
            return ResponseSupport.interrupt(exchange, responseVo, HttpStatus.REQUEST_TIMEOUT, new GatewayException(throwable));
        });
    }

    /**
     * 短路
     * @param exchange
     * @param throwable
     * @param e
     * @param message
     * @return
     */
    private Mono<Void> shortCircuit(ServerWebExchange exchange, Throwable throwable, HystrixRuntimeException e, String message) {
        return Mono.defer(() -> {
            logger.error("SHORTCIRCUIT : {}", message, e);
            ExchangeSupport.setHttpStatus(exchange, HttpStatus.REQUEST_TIMEOUT);
            exchange.getAttributes().put(HTTP_STATUS, HttpStatus.REQUEST_TIMEOUT.value());
            ResponseVo responseVo = new ResponseVo(HttpStatus.REQUEST_TIMEOUT.value(), SystemConstant.DEFAULT_ERROR_MESSAGE);
            return ResponseSupport.interrupt(exchange, responseVo, HttpStatus.REQUEST_TIMEOUT, new GatewayException(throwable));
        });
    }

    private class RouteHystrixCommand extends HystrixObservableCommand<Void> {

        private final URI fallbackUri;
        private final ServerWebExchange exchange;
        private final GatewayFilterChain chain;

        RouteHystrixCommand(Setter setter, URI fallbackUri, ServerWebExchange exchange, GatewayFilterChain chain) {
            super(setter);
            this.fallbackUri = fallbackUri;
            this.exchange = exchange;
            this.chain = chain;
        }

        @Override
        protected Observable<Void> construct() {
            return RxReactiveStreams.toObservable(this.chain.filter(exchange));
        }

        @Override
        protected Observable<Void> resumeWithFallback() {
            if (this.fallbackUri == null) {
                return super.resumeWithFallback();
            }

            // 重写 requestToUrlFilter
            URI uri = exchange.getRequest().getURI();
            // 判断url是否encode
            boolean encoded = containsEncodedParts(uri);
            URI requestUrl = UriComponentsBuilder.fromUri(uri)
                    .host(null)
                    .port(null)
                    .uri(this.fallbackUri)
                    .build(encoded)
                    .toUri();
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
            addExceptionDetails();

            ServerHttpRequest request = this.exchange.getRequest().mutate().uri(requestUrl).build();
            ServerWebExchange mutated = exchange.mutate().request(request).build();
            DispatcherHandler dispatcherHandler = FallbackGatewayFilterFactory.this.dispatcherHandler.getIfAvailable();
            return RxReactiveStreams.toObservable(dispatcherHandler.handle(mutated));
        }

        // 将错误信息设置到attributes 提供controller 使用参数
        private void addExceptionDetails() {
            Throwable executionException = getExecutionException();
            ofNullable(executionException)
                    .ifPresent(exception -> exchange.getAttributes().put(HYSTRIX_EXECUTION_EXCEPTION_ATTR, exception));
        }
    }

    public static class Config {
        private String name;
        private Setter setter;
        private URI fallbackUri;

        public String getName() {
            return name;
        }

        public FallbackGatewayFilterFactory.Config setName(String name) {
            this.name = name;
            return this;
        }

        public FallbackGatewayFilterFactory.Config setFallbackUri(String fallbackUri) {
            if (fallbackUri != null) {
                setFallbackUri(URI.create(fallbackUri));
            }
            return this;
        }

        public URI getFallbackUri() {
            return fallbackUri;
        }

        // 设置fallback url 必须forward开头
        public void setFallbackUri(URI fallbackUri) {
            if (fallbackUri != null && !"forward".equals(fallbackUri.getScheme())) {
                throw new IllegalArgumentException("Hystrix Filter currently only supports 'forward' URIs, found " + fallbackUri);
            }
            this.fallbackUri = fallbackUri;
        }

        public FallbackGatewayFilterFactory.Config setSetter(Setter setter) {
            this.setter = setter;
            return this;
        }
    }
}
