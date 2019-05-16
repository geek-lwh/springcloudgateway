package com.aha.tech.core.service.impl;

import com.aha.tech.core.service.WhiteListService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/5/16
 */
@Service("whiteListService")
public class WhiteListServiceImpl implements WhiteListService {

    @Resource
    private List<String> authWhiteList;

    @Resource
    private List<String> ipLimiterWhiteList;

    @Resource
    private List<String> urlTamperProofWhiteList;

    @Override
    public List<String> findSkipAuthWhiteList() {
        return authWhiteList;
    }

    @Override
    public List<String> findSkipIpLimiterWhiteList() {
        return ipLimiterWhiteList;
    }

    @Override
    public List<String> findSkipUrlTamperProofWhiteList() {
        return urlTamperProofWhiteList;
    }
}
