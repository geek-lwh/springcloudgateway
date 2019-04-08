package core.unit;

import com.aha.tech.commons.symbol.Separator;
import com.aha.tech.commons.utils.DateUtil;
import com.aha.tech.core.model.entity.RouteEntity;
import com.aha.tech.core.support.UriSupport;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aha.tech.core.support.UriSupport.buildRewritePath;

/**
 * @Author: luweihong
 * @Date: 2019/4/2
 */
@DisplayName("模拟网关重写uri地址测试类目")
public class RewritePathTest {

    private static final Logger logger = LoggerFactory.getLogger(RewritePathTest.class);

    private RouteEntity routeEntity;

    private String input;

    private String output;

    @Before
    public void initRequest() {
        input = "/v3/yanxuan/banner/get";
        output = "hjm_order_war/yanxuan/banner/get";

        routeEntity = new RouteEntity();
        routeEntity.setId("yanxuan");
        routeEntity.setUri(output);
        routeEntity.setContextPath("hjm_order_war");
    }

    @Test
    @DisplayName("去除请求url中以下划线分割的第一个区位测试类目")
    public void excludeInvalidPath() {
        logger.info("<<<< {} 开始 [去除请求url中以下划线分割的第一个区位测试类目]", DateUtil.currentDateByDefaultFormat());
        String validPath = UriSupport.excludeStrings(input, Separator.SLASH_MARK, 1);
        String verifyStr = "yanxuan/banner/get";

        System.out.println("入参 : " + validPath);
        System.out.println("期望值 : " + verifyStr);

        logger.info("{} 结束 [去除请求url中以下划线分割的第一个区位测试类目]", DateUtil.currentDateByDefaultFormat());
        Assertions.assertThat(validPath).as("校验失败").isEqualTo(verifyStr);

        logger.info("测试成功 >>>>");
    }

    @Test
    @DisplayName("重写uri测试类目")
    public void rewritePathTest() {
        logger.info("<<<< {} 开始 [重写uri测试类目]", DateUtil.currentDateByDefaultFormat());
        String validPath = "yanxuan/banner/get";
        String id = StringUtils.substringBefore(validPath, Separator.SLASH_MARK);
        logger.info("入参 : {}", id);
        logger.info("期望值 : {}", routeEntity.getId());
        Assertions.assertThat(id).as("重写后与期望值不匹配").isEqualTo(routeEntity.getId());
        logger.info("校验成功");

        String contextPath = routeEntity.getContextPath();
        String rewritePath = buildRewritePath(contextPath, validPath);
        logger.info("入参 : {}", rewritePath);
        logger.info("期望值 : {}", output);

        logger.info("{} 结束 [重写uri测试类目]", DateUtil.currentDateByDefaultFormat());
        Assertions.assertThat(rewritePath).as("重写后与期望值不匹配").isEqualTo(output);

        logger.info("测试成功 >>>>");
    }
}
