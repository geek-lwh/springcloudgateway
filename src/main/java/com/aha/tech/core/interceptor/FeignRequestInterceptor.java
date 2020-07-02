package com.aha.tech.core.interceptor;

import com.aha.tech.util.IdWorker;
import com.dianping.cat.Cat;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static com.aha.tech.core.constant.HeaderFieldConstant.X_TRACE_ID;

/**
 * @Author: luweihong
 * @Date: 2018/12/3
 * <p>
 * feign request 拦截器
 * 用于动态传递header信息,如果有需要需要自己定义
 */
//@Component
public class FeignRequestInterceptor implements RequestInterceptor {

    private final Logger logger = LoggerFactory.getLogger(FeignRequestInterceptor.class);

    public static final String TRACE_ID = "traceId";

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String traceId = Cat.createMessageId();
        requestTemplate.header(X_TRACE_ID, traceId);
    }


}
