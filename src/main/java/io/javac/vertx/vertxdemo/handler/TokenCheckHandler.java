package io.javac.vertx.vertxdemo.handler;

import io.javac.vertx.vertxdemo.model.respone.ResponeWrapper;
import io.javac.vertx.vertxdemo.vertx.VertxRespone;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author pencilso
 * @date 2020/1/31 11:31 下午
 */
@Component
public class TokenCheckHandler implements Handler<RoutingContext> {
    @Override
    public void handle(RoutingContext event) {
        HttpServerRequest request = event.request();
        String accesstoken = request.getHeader("accesstoken");
        if (StringUtils.isEmpty(accesstoken)) {
            VertxRespone.build(event).respone(new ResponeWrapper(10002, null, "登录失效，请重新登录！"));
        } else {
            //继续下一个路由
            event.next();
        }
    }
}
