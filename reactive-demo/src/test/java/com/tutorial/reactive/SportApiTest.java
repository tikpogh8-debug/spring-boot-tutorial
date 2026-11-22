package com.tutorial.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SportApiTest {

    @LocalServerPort
    int port;

    @Autowired
    SportRepository repository;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void createsSport() {
        client.post().uri("/api/v1/sport/Tennis").exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.name").isEqualTo("Tennis").jsonPath("$.id").isNumber();
        assertThat(repository.existsByName("Tennis").block()).isTrue();
    }

    @Test
    void duplicateNameReturnsConflict() {
        client.post().uri("/api/v1/sport/Tennis").exchange().expectStatus().isCreated();
        client.post().uri("/api/v1/sport/Tennis").exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().jsonPath("$.error").isEqualTo("Sport 'Tennis' already exists");
        assertThat(repository.count().block()).isEqualTo(1);
    }

    @Test
    void searchIsCaseInsensitiveContains() {
        for (String name : new String[] { "Tennis", "Table Tennis", "Golf" }) {
            client.post().uri("/api/v1/sport/{n}", name).exchange().expectStatus().isCreated();
        }
        client.get().uri("/api/v1/sport?q=tenn").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Table Tennis")
                .jsonPath("$[1].name").isEqualTo("Tennis");
        client.get().uri("/api/v1/sport?q=zzz").exchange()
                .expectStatus().isOk().expectBody().json("[]");
    }
}
