package com.tutorial.reactive;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class SportRouter {

    @Bean
    RouterFunction<ServerResponse> sportRoutes(SportHandler handler) {
        return route(POST("/api/v1/sport/{sportName}"), handler::create)
                .andRoute(GET("/api/v1/sport"), handler::search);
    }
}
