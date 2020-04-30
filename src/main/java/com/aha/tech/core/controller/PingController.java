package com.aha.tech.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: luweihong
 * @Date: 2019/4/15
 */
@RestController
public class PingController {

    @RequestMapping(value = "/gatewayserver/ping", method = RequestMethod.GET)
    public String ping() {
        return "pong";
    }
}
