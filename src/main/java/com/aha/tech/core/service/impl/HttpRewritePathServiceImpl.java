package com.aha.tech.core.service.impl;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.core.exception.NoSuchRouteException;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.service.RewritePathService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

import static com.aha.tech.core.support.URISupport.buildRewritePath;
import static com.aha.tech.core.support.URISupport.excludeStrings;

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
        return excludeStrings(path, Separator.SLASH_MARK, skipPart);
    }

    /**
     * 从请求路径寻找后端对应的rs地址
     * @param realServerHost 真实请求路径
     * @return
     */
    @Override
    public String rewritePath(String routeId, String realServerHost) {
        if (!routeEntityMap.containsKey(routeId)) {
            logger.error("根据routeId : {} 查找不到对应的请求资源 : {}", routeId, routeEntityMap);
            throw new NoSuchRouteException();
        }

        // 重写新的路由
        RouteEntity routeEntity = routeEntityMap.get(routeId);
        String contextPath = routeEntity.getContextPath();
        String rewritePath = buildRewritePath(contextPath, realServerHost);
        logger.debug("重写后的路径是: {}", rewritePath);

        return rewritePath;
    }
}
