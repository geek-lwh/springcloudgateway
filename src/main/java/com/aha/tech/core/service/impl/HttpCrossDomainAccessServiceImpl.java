package com.aha.tech.core.service.impl;

import com.aha.tech.core.service.CrossDomainAccessService;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: monkey
 * @Date: 2019/3/30
 * http跨域设置
 */
@Service("httpCrossDomainAccessService")
public class HttpCrossDomainAccessServiceImpl implements CrossDomainAccessService {

    /**
     * 跨域设置
     * @param httpHeaders
     */
    @Override
    public void CrossDomainSetting(HttpHeaders httpHeaders) {
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        // 表明服务器允许客户端使用 POST,PUT,GET,DELETE 发起请求
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_METHODS, "POST,PUT,GET,DELETE");

        // 表明该响应的有效时间为 10 秒
        httpHeaders.add(HEADER_ACCESS_CONTROL_MAX_AGE, "10");

        // 表明服务器允许请求中携带字段 X-PINGOTHER 与 Content-Type x-requested-with
        httpHeaders.add(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, "Authorization,Origin,X-Requested-With,X-Env,X-Request-Page,Content-Type,Accept");
    }
}
