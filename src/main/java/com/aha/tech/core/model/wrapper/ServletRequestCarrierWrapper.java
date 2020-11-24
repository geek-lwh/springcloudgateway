package com.aha.tech.core.model.wrapper;

import io.opentracing.propagation.TextMap;
import org.springframework.http.HttpHeaders;

import java.util.Iterator;
import java.util.Map;

/**
 * @Author: luweihong
 * @Date: 2020/11/13
 *
 * 用于trace inject时的封装
 * 用于gateway lb时候 span context 插入header
 *
 */
public class ServletRequestCarrierWrapper implements TextMap {

    public HttpHeaders httpHeaders;

    public ServletRequestCarrierWrapper(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("carrier is write-only");
    }

    @Override
    public void put(String key, String value) {
        httpHeaders.set(key, value);
    }

}
