package vuong20194412.chat.authentication_api_gateway_service;

import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.*;

@Configuration
class GatewayConfig {

    @Bean
    public RouterFunction<?> routerFunction() {
        return route("user-service-route")
                .route(routeUserService(), HandlerFunctions.http())
                .filter(lb("user-service"))
                .build()
                .and(
            route("user-service-error-route")
                .route(routeErrorUserService(), request -> ServerResponse.badRequest().body("missing headers"))
                .build())
//                .and(
//            route("message-service")
//                .route(path("/api/message/**"), HandlerFunctions.http("lb://message-service"))
//                .build())
                ;
    }

    private RequestPredicate routeUserService() {
        return path("/api/user/**")
                .and(method(HttpMethod.GET, HttpMethod.PUT)
                        .or(method(HttpMethod.POST).and(headerExists("X-USER-SERVICE-TOKEN")))
                        .or(method(HttpMethod.DELETE).and(headerExists("X-USER-ID")).and(headerExists("X-USER-SERVICE-TOKEN")))
                );
    }
    private RequestPredicate routeErrorUserService() {
        return path("/api/user/**")
                .and((method(HttpMethod.POST).and(headerExists("X-USER-SERVICE-TOKEN").negate()))
                        .or(method(HttpMethod.DELETE)
                                .and(headerExists("X-USER-ID").negate()
                                        .or(headerExists("X-USER-SERVICE-TOKEN").negate()))
                        )
                );
    }

    private RequestPredicate headerExists(String key) {
        return request -> request.headers().asHttpHeaders().containsKey(key);
    }

    private RequestPredicate isValidHeaderValue(@NonNull String key, String value) {
        return request -> request.headers().asHttpHeaders().getOrDefault(key, List.of("")).get(0).equals(value);
    }

}

