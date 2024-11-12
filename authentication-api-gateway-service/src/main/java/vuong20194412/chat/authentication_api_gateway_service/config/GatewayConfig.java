package vuong20194412.chat.authentication_api_gateway_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import vuong20194412.chat.authentication_api_gateway_service.entity.Account;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;

import java.util.List;
import java.util.function.Function;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.method;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

@Configuration
class GatewayConfig {

    private final AccountRepository accountRepository;

    @Autowired
    GatewayConfig(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Bean
    public RouterFunction<?> routerFunction() {
        return route()
                .add(userServiceRoute())
//                .add(
//            route("message-service")
//                .route(path("/api/message/**"), HandlerFunctions.http("lb://message-service"))
//                .build())
//                .before(
//                        request -> {
//                            HttpHeaders headers = new HttpHeaders();
//                            headers.putAll(request.headers().asHttpHeaders());
//                            System.out.println(request.headers().asHttpHeaders());
//                            UriComponents uriComponents =  ServletUriComponentsBuilder.fromCurrentRequest().build();
//                            if (!headers.containsKey("X-Forwarded-Proto") && uriComponents.getScheme() != null)
//                                headers.add("X-Forwarded-Proto", uriComponents.getScheme());
//                            if (!headers.containsKey("X-Forwarded-Host") && !headers.containsKey("X-Forwarded-Port")) {
//                                if (uriComponents.getHost() != null) {
//                                    headers.add("X-Forwarded-Host", uriComponents.getHost());
//                                    if (uriComponents.getPort() > 0)
//                                        headers.add("X-Forwarded-Port", String.valueOf(uriComponents.getPort()));
//                                }
//                            }
//                            ServerRequest _request = ServerRequest.from(request).headers(httpHeaders -> httpHeaders.putAll(headers)).build();
//                            System.out.println(_request.headers().asHttpHeaders());
//                            return _request;
//                        })
                //.before(addRequestHeadersIfNotPresent("X-Forwarded-Host", ServletUriComponentsBuilder.fromCurrentRequest().build().getHost()))
                //.before(addRequestHeadersIfNotPresent("X-Forwarded-Port", String.valueOf(ServletUriComponentsBuilder.fromCurrentRequest().build().getPort())))
                .build();
    }

    private RouterFunction<ServerResponse> userServiceRoute() {
        RouterFunction<ServerResponse> GETRoute = route(path("/api/user/**").and(method(HttpMethod.GET)), HandlerFunctions.http());

        RouterFunction<ServerResponse> POSTRoute = route(path("/api/user/**").and(method(HttpMethod.POST)), filterUserTokenHeader());

        RouterFunction<ServerResponse> PUTRoute = route().PUT(path("/api/user/**"), HandlerFunctions.http())
                .before(setUserIdHeader()).build();

        RouterFunction<ServerResponse> DELETERoute = route().DELETE(path("/api/user/**"), filterUserTokenHeader())
                .before(setUserIdHeader()).build();

        return route("user-service-route")
                .add(GETRoute)
                .add(POSTRoute)
                .add(PUTRoute)
                .add(DELETERoute)
                .filter(lb("user-service"))
                .build();
    }

    private static HandlerFunction<ServerResponse> filterUserTokenHeader() {
        return request -> {
            if (!request.headers().asHttpHeaders().containsKey("X-USER-SERVICE-TOKEN"))
                return ServerResponse.badRequest().header("content-type", "application/problem+json").body("{\"Bad request\": \"Let's call different api to affect user\"");
            return HandlerFunctions.http().handle(request);
        };
    }

    private Function<ServerRequest, ServerRequest> setUserIdHeader() {
        return request -> {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(request.headers().asHttpHeaders());
            System.out.println(request.headers().asHttpHeaders());
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                Account account = accountRepository.findByEmail(user.getUsername());
                System.out.println(account.getUserId());
                headers.put("X-USER-ID", List.of(String.valueOf(account.getUserId())));
            } else {
                headers.remove("X-USER-ID");
            }

            ServerRequest _request = ServerRequest.from(request).headers(httpHeaders -> httpHeaders.putAll(headers)).build();
            System.out.println(_request.headers().asHttpHeaders());
            return _request;
        };
    }

}

