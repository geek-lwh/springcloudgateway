package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.service.RewritePathService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: luweihong
 * @Date: 2019/3/15
 */
@Service("httpRewritePathService")
public class HttpRewritePathServiceImpl implements RewritePathService {

    private static final Logger logger = LoggerFactory.getLogger(HttpRewritePathServiceImpl.class);

    @Resource
    private Map<String, RouteEntity> routeEntityMap;

    /**
     * 去除无效的路径
     *
     * 目前去除V1
     *
     * @param path 原有路径 : http://host:port/V1/yanxuan/banner/get
     * @param skipPart 跳过单元数
     * @return
     */
    @Override
    public String excludeInvalidPath(String path, int skipPart) {
        String[] arr = org.springframework.util.StringUtils.tokenizeToStringArray(path, Separator.SLASH_MARK);
        Stream<String> validPathStream = Arrays.stream(arr).skip(skipPart);

        String validPath = validPathStream.collect(Collectors.joining(Separator.SLASH_MARK));
        return validPath;
    }

    /**
     * 从请求路径寻找后端对应的rs地址
     * @param validPath 真实请求路径
     * @return
     */
    @Override
    public String rewritePath(String validPath) {
        String id = StringUtils.substringBefore(validPath, Separator.SLASH_MARK);
        if (!routeEntityMap.containsKey(id)) {
            logger.error("没有匹配的路由地址 : {}", validPath);
            return null;
        }

        // 重写新的路由
        RouteEntity routeEntity = routeEntityMap.get(id);
        String rewritePath = new StringBuilder()
                .append(routeEntity.getContextPath())
                .append(Separator.SLASH_MARK)
                .append(validPath).toString();

        logger.debug("重写后的路径是: {}", rewritePath);

        return rewritePath;
    }
}
