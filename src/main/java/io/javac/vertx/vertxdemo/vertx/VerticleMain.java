package io.javac.vertx.vertxdemo.vertx;

import io.javac.vertx.vertxdemo.annotation.RequestBlockingHandler;
import io.javac.vertx.vertxdemo.annotation.RequestBody;
import io.javac.vertx.vertxdemo.annotation.RequestMapping;
import io.javac.vertx.vertxdemo.config.SpringBootContext;
import io.javac.vertx.vertxdemo.handler.TokenCheckHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author pencilso
 * @date 2020/1/31 9:00 下午
 */
@Component
@Slf4j
public class VerticleMain extends AbstractVerticle {
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    TokenCheckHandler tokenCheckHandler;
    /**
     * Controller 所在的包
     */
    private final String controllerBasePackage[] = {
            "io.javac.vertx.vertxdemo.controller"
    };


    @Override
    public void start() throws Exception {
        super.start();
        //路由
        Router router = Router.router(vertx);
        router.route().path("/api/*").handler(tokenCheckHandler);

        //注册Controller
        for (String packagePath : controllerBasePackage) {
            registerController(router, packagePath);
        }
        router.route().failureHandler(handler -> {
            handler.failure().printStackTrace();
        });
        //start listen port
        HttpServer server = vertx.createHttpServer();
        server.requestHandler(router).listen(8888, handler -> {
            log.info("vertx run prot : [{}] run state : [{}]", 8888, handler.succeeded());
        });
    }


    /**
     * register controller
     */
    private void registerController(@NotNull Router router, String packagePath) {
        if (SpringBootContext.getApplicationContext() == null) {
            log.warn("SpringBoot application context is null register controller is fail");
            return;
        }

        try {
            Resource[] resources = VerticleUtils.scannerControllerClass(packagePath, resourceLoader);
            for (Resource resource : resources) {
                String absolutePath = resource.getFile().getAbsolutePath().replace("/", ".");
                absolutePath = absolutePath.substring(absolutePath.indexOf(packagePath));
                absolutePath = absolutePath.replace(".class", "");
                if (StringUtils.isEmpty(absolutePath)) continue;
                //get class
                Class<?> controllerClass = Class.forName(absolutePath);
                //from class get controller instance bean
                Object controller = SpringBootContext.getApplicationContext().getBean(controllerClass);

                RequestMapping classRequestMapping = controllerClass.getAnnotation(RequestMapping.class);
                //if controller class not have requestMapping annotation -> skip register
                if (classRequestMapping == null) continue;
                //register controller method
                registerControllerMethod(router, classRequestMapping, controllerClass, controller);
            }
        } catch (Exception ex) {
            log.error("registerController fail ", ex);
        }
    }

    /**
     * register controller method
     *
     * @param router              route
     * @param classRequestMapping controller requestMapping annotation
     * @param controllerClass     controller class
     * @param controller          controller instance
     */
    private void registerControllerMethod(@NotNull Router router, @NotNull RequestMapping classRequestMapping, @NotNull Class<?> controllerClass, @NotNull Object controller) {
        //获取控制器里的方法
        Method[] controllerClassMethods = controllerClass.getMethods();
        Arrays.asList(controllerClassMethods).stream()
                .filter(method -> method.getAnnotation(RequestMapping.class) != null)
                .forEach(method -> {
                    try {
                        RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
                        String superPath = classRequestMapping.value()[0];
                        String methodPath = methodRequestMapping.value()[0];
                        //if api path empty skip
                        if (StringUtils.isEmpty(superPath) || StringUtils.isEmpty(methodPath)) return;
                        String url = VerticleUtils.buildApiPath(superPath, methodPath);
                        //build route
                        Route route = VerticleUtils.buildRouterUrl(url, router, methodRequestMapping.method());
                        //run controller method get Handler object
                        Handler<RoutingContext> methodHandler = (Handler<RoutingContext>) method.invoke(controller);
                        //register bodyAsJson handler
                        Optional.ofNullable(method.getAnnotation(RequestBody.class)).ifPresent(requestBody -> {
                            route.handler(BodyHandler.create());
                        });
                        //register controller mthod Handler object
                        RequestBlockingHandler requestBlockingHandler = Optional.ofNullable(method.getAnnotation(RequestBlockingHandler.class)).orElseGet(() -> controllerClass.getAnnotation(RequestBlockingHandler.class));
                        if (requestBlockingHandler != null) {
                            //register blocking handler
                            route.blockingHandler(methodHandler);
                        } else {
                            route.handler(methodHandler);
                        }
                        log.info("register controller -> [{}]  method -> [{}]  url -> [{}] ", controllerClass.getName(), method.getName(), url);
                    } catch (Exception e) {
                        log.error("registerControllerMethod fail controller: [{}]  method: [{}]", controllerClass, method.getName());
                    }
                });
    }
}