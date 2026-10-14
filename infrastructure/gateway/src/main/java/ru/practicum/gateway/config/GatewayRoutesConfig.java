package ru.practicum.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration
public class GatewayRoutesConfig {

    private final Environment environment;

    public GatewayRoutesConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public RouterFunction<ServerResponse> accountRoute() {
        return route("account_service_route")
                .route(path("/account/**"), http())
                .before(uri(environment.getProperty(
                        "services.accounts.url", "http://accounts:8082")))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> cashRoute() {
        return route("cash_service_route")
                .route(path("/cash/**"), http())
                .before(uri(environment.getProperty(
                        "services.cash.url", "http://cash:8083")))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> transferRoute() {
        return route("transfer_service_route")
                .route(path("/transfer/**"), http())
                .before(uri(environment.getProperty(
                        "services.transfer.url", "http://transfer:8084")))
                .build();
    }

}