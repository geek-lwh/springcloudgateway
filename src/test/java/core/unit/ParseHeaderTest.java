package core.unit;

import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.support.ParseHeadersSupport;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: monkey
 * @Date: 2019-04-01
 */
@RunWith(JUnit4.class)
@DisplayName("XEnv相关的测试类目")
public class ParseHeaderTest {

    private static final Logger logger = LoggerFactory.getLogger(ParseHeaderTest.class);


    @Test
    @DisplayName("解析pp的测试类目")
    public void parsePp() {
        logger.info("<<<< {} 开始解析pp的测试类目", DateUtil.currentDateByDefaultFormat());
        String input = "NTA1MDY5OjEwMDMyNTowOjYyODk6MTowJHBwOGVlZWU1ZGU5Mjc3MTcxNWY3NzgyZmY0Y2ZjODkxNDE=";
        String pp = new String(org.apache.commons.codec.binary.Base64.decodeBase64(input), StandardCharsets.UTF_8);
        boolean b = ParseHeadersSupport.verifyPp(pp);
        logger.info("入参 : {}", input);
        logger.info("期望值 : {}", b);

        logger.info(">>>> {} 结束 [开始解析pp的测试类目]", DateUtil.currentDateByDefaultFormat());
        Assertions.assertThat(b).as("校验失败").isEqualTo(true);
        logger.info("测试成功 >>>>");
    }

    @Test
    @DisplayName("xForwardedFor多个值取最左为realIp测试类目")
    public void forwardedTest() {
        logger.info("<<<< {} xForwardedFor多个值取最左为realIp测试类目", DateUtil.currentDateByDefaultFormat());
        HttpHeaders httpHeaders = new HttpHeaders();
        String realIp = "127.0.0.8";
        httpHeaders.add(HeaderFieldConstant.HEADER_X_FORWARDED_FOR, realIp);
        httpHeaders.add(HeaderFieldConstant.HEADER_X_FORWARDED_FOR, "127.0.0.7");
        httpHeaders.add(HeaderFieldConstant.HEADER_X_FORWARDED_FOR, "127.0.0.6");

        String v = ParseHeadersSupport.parseHeaderIp(httpHeaders);
        List<String> input = httpHeaders.get(HeaderFieldConstant.HEADER_X_FORWARDED_FOR);
        logger.info("入参 : {}", input);
        logger.info("期望值 : {}", realIp);

        logger.info(">>>> {} 结束 [xForwardedFor多个值取最左为realIp测试类目]", DateUtil.currentDateByDefaultFormat());
        Assertions.assertThat(v).as("ip解析不一致").isEqualTo(realIp);
        logger.info("测试成功 >>>>");
    }
}
