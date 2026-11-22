package com.tutorial.reactive;

import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

@Component
public class SportHandler {

    private final SportRepository repository;

    public SportHandler(SportRepository repository) {
        this.repository = repository;
    }

    /** POST /api/v1/sport/{sportName}: 201 on success, 409 if the name already exists. */
    public Mono<ServerResponse> create(ServerRequest request) {
        String name = request.pathVariable("sportName").trim();
        if (name.isEmpty()) {
            return error(HttpStatus.BAD_REQUEST, "Sport name must not be blank");
        }
        return repository.existsByName(name)
                .flatMap(exists -> exists ? conflict(name) : save(name))
                // Two concurrent requests can both pass existsByName; the unique constraint decides.
                .onErrorResume(DuplicateKeyException.class, e -> conflict(name));
    }

    /** GET /api/v1/sport?q=...: sports whose name contains q (all sports if q is missing). */
    public Mono<ServerResponse> search(ServerRequest request) {
        String q = request.queryParam("q").orElse("");
        return ServerResponse.ok().body(repository.searchByName(q), Sport.class);
    }

    private Mono<ServerResponse> save(String name) {
        return repository.save(Sport.named(name))
                .flatMap(saved -> ServerResponse
                        .created(UriComponentsBuilder.fromPath("/api/v1/sport").queryParam("q", "{q}").build(saved.name()))
                        .bodyValue(saved));
    }

    private Mono<ServerResponse> conflict(String name) {
        return error(HttpStatus.CONFLICT, "Sport '" + name + "' already exists");
    }

    private Mono<ServerResponse> error(HttpStatus status, String message) {
        return ServerResponse.status(status).bodyValue(Map.of("status", status.value(), "error", message));
    }
}
