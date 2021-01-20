package com.aha.tech.core.service.impl;

import com.aha.tech.core.service.WhiteListService;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: luweihong
 * @Date: 2019/5/16
 */
@Service("whiteListService")
public class WhiteListServiceImpl implements WhiteListService {

    private final static String API_LIST_OF_AUTH_WHITELIST = "skip.auth.list";

    private final static String API_LIST_OF_IP_LIMITER_WHITELIST = "skip.iplimiter.api.list";

    private final static String IP_LIST_OF_IP_LIMITER_WHITELIST = "skip.iplimiter.ip.list";

    private final static String URL_TAMPER_PROOF_WHITELIST = "skip.url.tamper.proof";

    @Override
    public List<String> fetchAuthWhiteList() {
        Config config = ConfigService.getAppConfig();
        String[] list = config.getArrayProperty(API_LIST_OF_AUTH_WHITELIST, ",", new String[]{});
        return Arrays.asList(list);
    }

    @Override
    public List<String> fetchIpLimiterApiWhiteList() {
        Config config = ConfigService.getAppConfig();
        String[] list = config.getArrayProperty(API_LIST_OF_IP_LIMITER_WHITELIST, ",", new String[]{});

        return Arrays.asList(list);
    }

    @Override
    public List<String> fetchIpLimiterIpWhiteList() {
        Config config = ConfigService.getAppConfig();
        String[] list = config.getArrayProperty(IP_LIST_OF_IP_LIMITER_WHITELIST, ",", new String[]{});

        return Arrays.asList(list);
    }

    @Override
    public List<String> fetchUrlTamperProofWhiteList() {
        Config config = ConfigService.getAppConfig();
        String[] list = config.getArrayProperty(URL_TAMPER_PROOF_WHITELIST, ",", new String[]{});

        return Arrays.asList(list);
    }
}
