package com.aha.tech.core.controller.resource;

import com.aha.tech.core.controller.resource.fallback.PassportFallbackFactory;
import com.aha.tech.core.interceptor.FeignRequestInterceptor;
import com.aha.tech.passportserver.facade.api.AuthorizationFacade;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author: luweihong
 * @Date: 2019/1/9
 */
@FeignClient(name = "passportserver", path = "passportserver", fallbackFactory = PassportFallbackFactory.class,configuration = FeignRequestInterceptor.class)
public interface PassportResource extends AuthorizationFacade {
}
