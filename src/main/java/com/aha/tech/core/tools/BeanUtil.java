package com.aha.tech.core.tools;

import org.springframework.util.MultiValueMap;

import java.util.LinkedList;

/**
 * @Author: luweihong
 * @Date: 2019/2/21
 */
public class BeanUtil {

    public static <K, V> void copyMultiValueMap(MultiValueMap<K, V> source, MultiValueMap<K, V> target) {
        source.forEach((key, value) -> target.put(key, new LinkedList<>(value)));
    }

}
