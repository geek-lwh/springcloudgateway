package core.unit;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.service.ModifyHeaderService;
import core.base.SpringWebTest;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import javax.annotation.Resource;
import java.util.List;

import static com.aha.tech.core.constant.HeaderFieldConstant.*;

/**
 * @Author: luweihong
 * @Date: 2019/4/2
 */
@DisplayName("修改请求头测试类目")
public class ModifyRequestHeadersTest extends SpringWebTest {

    private static final Logger logger = LoggerFactory.getLogger(ModifyRequestHeadersTest.class);


    private HttpHeaders httpHeaders;

    private String ENCODE_X_ENV = "eyJ1dG1fc291cmNlIjoiIiwidXRtX21lZGl1bSI6IiIsInV0bV9jYW1wYWlnbiI6IiIsInV0bV90ZXJtIjoiIiwidXRtX2NvbnRlbnQiOiIiLCJwayI6Ikx6WTVOVE09IiwicGQiOiIiLCJwcyI6InVwNDY2MDhiMGZjMTA1MDBlNzgyYTVjYmJiZTg2M2E1YWUiLCJwcCI6IiIsImFwcF90eXBlIjoxLCJndW5pcWlkIjoiZWM3NjdhZjI5ZDY5Zjg0YjViYjEwZGFmOGNiZjk1OGIifQ==";

    private static String VERSION = "4.3.0";

    private static String OS = "ios";

    private String USER_AGENT = "ahaschool/" + OS + "/" + VERSION + "/12.0/iPhone/757797D4-1282-4668-8AA3-FE4189AAED12";

    @Resource
    private ModifyHeaderService httpModifyHeaderService;

    @Before
    public void initHeaders() {
        logger.info("初始化http header");
        httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
        httpHeaders.add(HttpHeaders.CONNECTION, "keep-alive");
        httpHeaders.add(HttpHeaders.PRAGMA, "no-cache");
        httpHeaders.add(HttpHeaders.CACHE_CONTROL, "no-cache");
        httpHeaders.add(HEADER_X_REQUEST_PAGE, "https://devm.ahaschool.com/home#!/");
        httpHeaders.add(HttpHeaders.ORIGIN, "https://devm.ahaschool.com");
        httpHeaders.add(HEADER_X_ENV, ENCODE_X_ENV);
        httpHeaders.add(HttpHeaders.ACCEPT, "application/json, text/javascript, */*; q=0.01");
        httpHeaders.add("X-Requested-With", "XMLHttpRequest");
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
        httpHeaders.add(HttpHeaders.REFERER, "https://devm.ahaschool.com/home");
        httpHeaders.add(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
        httpHeaders.add(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8");
        logger.info("初始化结束 httpHeaders ==> : {}", httpHeaders);
    }

    @Test
    @DisplayName("请求头设置版本号测试类目")
    public void versionSetting() {
        logger.info("<<<< {} 请求头设置版本号测试类目", DateUtil.currentDateByDefaultFormat());

        String testValue = httpHeaders.get(HEADER_OS).get(0);
        logger.info("入参 : {}", httpHeaders.get(HttpHeaders.USER_AGENT));
        System.out.println("期望值 : " + OS);

        httpModifyHeaderService.versionSetting(httpHeaders);
        Assertions.assertThat(testValue).as("重写后与期望值不匹配").isEqualTo(OS);

        String testValue2 = httpHeaders.get(HEADER_VERSION).get(0);
        System.out.println("入参 : " + httpHeaders.get(HttpHeaders.USER_AGENT));
        System.out.println("期望值 : " + VERSION);
        logger.info(">>>> {} 请求头设置版本号测试类目", DateUtil.currentDateByDefaultFormat());
        Assertions.assertThat(testValue2).as("重写后与期望值不匹配").isEqualTo(VERSION);
        logger.info("测试成功");
    }

//    @Test
//    @DisplayName("请求解析XEnv报头并生成新的头")
//    public void xEnvSetting() {
//        logger.info("<<<< {} 请求解析XEnv报头并生成新的头", DateUtil.currentDateByDefaultFormat());
//
//        List<String> xEnv = httpHeaders.get(HEADER_X_ENV);
//        logger.info("入参 : {}", xEnv);
//        httpModifyHeaderService.xEnvSetting(httpHeaders);
//
//        logger.info(">>>> {} 请求解析XEnv报头并生成新的头", DateUtil.currentDateByDefaultFormat());
//        logger.info("测试成功");
//    }
}
