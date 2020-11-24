package com.aha.tech.core.model.wrapper;

import feign.RequestTemplate;
import io.opentracing.propagation.TextMap;

import java.util.Iterator;
import java.util.Map;

/**
 * @Author: luweihong
 * @Date: 2020/11/13
 *
 * 用于gateway 进行feign调用时候 span context 插入header
 *
 */
public class FeignCarrierWrapper implements TextMap {

    public RequestTemplate requestTemplate;

    public FeignCarrierWrapper(RequestTemplate requestTemplate) {
        this.requestTemplate = requestTemplate;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("carrier is write-only");
    }

    @Override
    public void put(String key, String value) {
        requestTemplate.header(key, value);
    }

}
