package io.javac.vertx.vertxdemo;

import io.javac.vertx.vertxdemo.config.SpringBootContext;
import io.javac.vertx.vertxdemo.vertx.VerticleMain;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

@Slf4j
@SpringBootApplication
public class VertxdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VertxdemoApplication.class, args);
    }

    /**
     * 监听SpringBoot 启动完毕 开始部署Vertx
     *
     * @param event
     */
    @EventListener
    public void deployVertx(ApplicationReadyEvent event) {
        ConfigurableApplicationContext applicationContext = event.getApplicationContext();
        SpringBootContext.setApplicationContext(applicationContext);
        VerticleMain verticleMain = applicationContext.getBean(VerticleMain.class);
        Vertx vertx = Vertx.vertx();
        //部署vertx
        vertx.deployVerticle(verticleMain, handler -> {
            log.info("vertx deploy state [{}]", handler.succeeded());
        });
    }
}
