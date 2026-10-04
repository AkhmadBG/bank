package ru.practicum.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.LoadBalancerFilterFunctions.lb;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> accountRoute() {
        return route("account_service_route")
                .route(path("/account/**"), http())
                .filter(lb("accounts"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> cashRoute() {
        return route("cash_service_route")
                .route(path("/cash/**"), http())
                .filter(lb("cash"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transferRoute() {
        return route("transfer_service_route")
                .route(path("/transfer/**"), http())
                .filter(lb("transfer"))
                .build();
    }
}