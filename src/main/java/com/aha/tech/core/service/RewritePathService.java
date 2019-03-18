package com.aha.tech.core.service;

import com.aha.tech.core.model.entity.RouteEntity;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
public interface RewritePathService {

    /**
     * 解析url
     * @param path 原有路径
     * @param skipPart 跳过单元数
     * @return
     */
    String excludeInvalidPath(String path, int skipPart);

    RouteEntity rewritePath(String oldPath);
}
