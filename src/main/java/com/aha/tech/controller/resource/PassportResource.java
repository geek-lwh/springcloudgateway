package com.aha.tech.controller.resource;

import com.aha.tech.controller.resource.fallback.PassportFallbackFactory;
import com.aha.tech.passportserver.facade.api.AuthorizationFacade;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author: luweihong
 * @Date: 2019/1/9
 */
@FeignClient(name = "passportserver", path = "passportserver", fallback = PassportFallbackFactory.class)
public interface PassportResource extends AuthorizationFacade {
}
