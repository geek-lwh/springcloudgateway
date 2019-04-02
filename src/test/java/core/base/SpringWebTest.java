package core.base;

import com.aha.tech.GatewayApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @Author: luweihong
 * @Date: 2019/4/2
 */
@RunWith(SpringRunner.class)
@Profile("localtest")
@SpringBootTest(classes = GatewayApplication.class)
public class SpringWebTest {
}
