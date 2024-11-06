package vuong20194412.chat.authentication_api_gateway_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import vuong20194412.chat.authentication_api_gateway_service.util.DomainAddressUtil;
import vuong20194412.chat.authentication_api_gateway_service.entity.Account;
import vuong20194412.chat.authentication_api_gateway_service.repository.AccountRepository;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeadersIfNotPresent;
import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.addRequestHeader;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.*;

@Configuration
class GatewayConfig {

    private final AccountRepository accountRepository;

    private final String domainAddress;

    @Autowired
    GatewayConfig(AccountRepository accountRepository, DomainAddressUtil domainAddressConfig) {
        this.accountRepository = accountRepository;
        this.domainAddress = domainAddressConfig.getDomainAddress();
    }

    @Bean
    public RouterFunction<?> routerFunction() {
        return route()
                .add(userServiceRoute())
//                .add(
//            route("message-service")
//                .route(path("/api/message/**"), HandlerFunctions.http("lb://message-service"))
//                .build())
                .before(addRequestHeadersIfNotPresent("X-Forwarded-Host", "localhost"))
                .before(addRequestHeadersIfNotPresent("X-Forwarded-Port", "8000"))
                .build();
    }

    private RouterFunction<ServerResponse> userServiceRoute() {
        RouterFunction<ServerResponse> GETRoute = route(path("/api/user/**").and(method(HttpMethod.GET)), HandlerFunctions.http());

        RouterFunction<ServerResponse> POSTRoute = route(path("/api/user/**").and(method(HttpMethod.POST)), request -> {
            if (!hasHeader(request, "X-USER-SERVICE-TOKEN"))
                return ServerResponse.badRequest().body("missing headers");
            return HandlerFunctions.http().handle(request);
        });

        RouterFunction<ServerResponse> PUTRoute = route().PUT(path("/api/user/**"), HandlerFunctions.http())
                .before(addRequestHeader("X-USER-ID", getUserId())).build();

        RouterFunction<ServerResponse> DELETERoute = route().DELETE(path("/api/user/**"), request -> {
            if (!hasHeader(request, "X-USER-SERVICE-TOKEN"))
                return ServerResponse.badRequest().body("missing headers");
            return HandlerFunctions.http().handle(request);
        }).before(addRequestHeader("X-USER-ID", getUserId())).build();

        return route("user-service-route")
                .add(GETRoute)
                .add(POSTRoute)
                .add(PUTRoute)
                .add(DELETERoute)
                .filter(lb("user-service"))
                .build();
    }

    private boolean hasHeader(ServerRequest request, String key) {
        return request.headers().asHttpHeaders().containsKey(key);
    }

    private String getUserId() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = accountRepository.findByEmail(user.getUsername());
        return String.valueOf(account.getId());
    }
}

