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

    private final static String URL_TAMPER_PROOF_WHITELIST = "skip.url.tamper.proof.list";

    private final static String KID_ACCOUNT_WHITELIST = "skip.kid.account.list";

    @Override
    public List<String> fetchAuthWhiteList() {
        return readAppProperties(API_LIST_OF_AUTH_WHITELIST);
    }

    @Override
    public List<String> fetchIpLimiterApiWhiteList() {
        return readAppProperties(API_LIST_OF_IP_LIMITER_WHITELIST);
    }

    @Override
    public List<String> fetchIpLimiterIpWhiteList() {
        return readAppProperties(IP_LIST_OF_IP_LIMITER_WHITELIST);
    }

    @Override
    public List<String> fetchUrlTamperProofWhiteList() {
        return readAppProperties(URL_TAMPER_PROOF_WHITELIST);
    }

    @Override
    public List<String> fetchKidAccountWhiteList() {
        return readAppProperties(KID_ACCOUNT_WHITELIST);
    }

    /**
     * 读取application.properties
     * @param propName
     * @return
     */
    private List<String> readAppProperties(String propName) {
        Config config = ConfigService.getAppConfig();
        String[] list = config.getArrayProperty(propName, ",", new String[]{});

        return Arrays.asList(list);
    }
}
