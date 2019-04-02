package core.env;

import com.aha.tech.core.support.XEnvSupport;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.charset.StandardCharsets;

/**
 * @Author: monkey
 * @Date: 2019-04-01
 */
@RunWith(JUnit4.class)
@DisplayName("XEnv相关的测试类目")
public class XEnvTest {

    @Test
    @DisplayName("解析pp")
    public void parsePp(){
        String s = "NTA1NjM3OjEwMDA3NzowOjEzOTczMjM6MjowJHBwOGIxMmIwZmFmNDA2NGZkMzZjMzMzZDdlM2ZhMWUzMzI=";
        String pp = new String(org.apache.commons.codec.binary.Base64.decodeBase64(s), StandardCharsets.UTF_8);
        boolean b = XEnvSupport.verifyPp(pp);
        Assertions.assertThat(b).as("是否校验成功").isEqualTo(true);
    }
}
