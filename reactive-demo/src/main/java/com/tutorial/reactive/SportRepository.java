package com.tutorial.reactive;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SportRepository extends ReactiveCrudRepository<Sport, Integer> {

    Mono<Boolean> existsByName(String name);

    /** Case-insensitive "contains" search used by GET /api/v1/sport?q=... */
    @Query("SELECT * FROM sport WHERE LOWER(name) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY name")
    Flux<Sport> searchByName(String q);
}
