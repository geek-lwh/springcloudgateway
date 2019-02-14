package com.aha.tech.controller;

import com.aha.tech.controller.resource.PassportResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: luweihong
 * @Date: 2019/2/14
 */
@RestController
public class PublicController {

    @Autowired
    private PassportResource passportResource;

    @RequestMapping(value = "/")
    public String hello(){
        passportResource.verify("");

        return "asd";
    }

}
