package com.aha.tech.core.controller;

import com.aha.tech.commons.response.RpcResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: luweihong
 * @Date: 2019/3/12
 */
@RestController
public class FallBackController {

    @RequestMapping(value = "/fallback")
    public RpcResponse fallBack(){
        return RpcResponse.defaultHystrixFallbackResponse();
    }
}
