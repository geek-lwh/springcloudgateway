package core.env;

import com.aha.tech.core.constant.HeaderFieldConstant;
import com.aha.tech.core.support.ParseHeadersSupport;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
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

    @Test
    @DisplayName("解析pp")
    public void parsePp() {
        String s = "NTA1NjM3OjEwMDA3NzowOjEzOTczMjM6MjowJHBwOGIxMmIwZmFmNDA2NGZkMzZjMzMzZDdlM2ZhMWUzMzI=";
        String pp = new String(org.apache.commons.codec.binary.Base64.decodeBase64(s), StandardCharsets.UTF_8);
        boolean b = ParseHeadersSupport.verifyPp(pp);
        Assertions.assertThat(b).as("校验失败").isEqualTo(true);
    }

    /**
     *
     */
    @Test
    @DisplayName("X-Forwarded-For多个信息")
    public void forwardedTest() {
        HttpHeaders httpHeaders = new HttpHeaders();
        String realIp = "127.0.0.8";
        httpHeaders.add(HeaderFieldConstant.HEADER_X_FORWARDED_FOR, realIp);
        httpHeaders.add(HeaderFieldConstant.HEADER_X_FORWARDED_FOR, "127.0.0.7");
        httpHeaders.add(HeaderFieldConstant.HEADER_X_FORWARDED_FOR, "127.0.0.6");

        Assertions.assertThat(httpHeaders).as("X-Forwarded-For多个值").isNotEmpty();

        List<String> xForwardedForList = httpHeaders.get(HeaderFieldConstant.HEADER_X_FORWARDED_FOR);
        Assertions.assertThat(xForwardedForList.size()).as("头信息不大于1").isGreaterThan(1);

        Assertions.assertThat(ParseHeadersSupport.parseHeaderIp(httpHeaders)).as("ip解析不一致").isEqualTo(realIp);
    }
}
