package com.aha.tech.core.service;

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

    /**
     * 重写转发的请求地址
     * @param routeId
     * @param oldPath
     * @return
     */
    String rewritePath(String routeId,String oldPath);

}
