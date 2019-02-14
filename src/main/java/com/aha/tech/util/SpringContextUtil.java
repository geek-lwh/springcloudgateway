package com.aha.tech.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 获取spring容器，以访问容器中定义的其他bean
 *
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(SpringContextUtil.class);

    private static ApplicationContext applicationContext;

    // 获取上下文
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    // 设置上下文
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextUtil.applicationContext = applicationContext;
        logger.info("==============applicationContext:{}=================", applicationContext);
    }

    // 通过名字获取上下文中的bean
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    // 通过类型获取上下文中的bean
    public static <T> T getBean(Class<T> requiredType) {
        return (T) applicationContext.getBean(requiredType);
    }
}
