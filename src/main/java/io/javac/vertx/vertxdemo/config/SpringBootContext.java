package io.javac.vertx.vertxdemo.config;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author pencilso
 * @date 2020/1/31 10:01 下午
 */
public class SpringBootContext {
    private static ConfigurableApplicationContext applicationContext;

    public static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public static void setApplicationContext(ConfigurableApplicationContext applicationContext) {
        SpringBootContext.applicationContext = applicationContext;
    }
}