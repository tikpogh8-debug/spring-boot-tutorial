package com.tutorial.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootTest
class SetupTest {

    @Autowired
    SportRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll().block();
    }

    @Test
    void loadRequestsTwentyAtATime() {
        var setup = new Setup(null, repository, props(), 20);
        Flux<String> names = Flux.range(1, 45).map(i -> "Sport " + i);

        AtomicReference<Setup.BatchSubscriber> probe = new AtomicReference<>();
        Long saved = Mono.<Long>create(sink -> {
            var subscriber = new Setup.BatchSubscriber(20, sink);
            probe.set(subscriber);
            setup.pipeline(names).subscribe(subscriber);
        }).block();

        assertThat(saved).isEqualTo(45);
        assertThat(probe.get().requests()).containsExactly(20L, 20L, 20L);
        assertThat(repository.count().block()).isEqualTo(45);
    }

    @Test
    void loadIsIdempotentAndSkipsDuplicates() {
        var setup = new Setup(null, repository, props(), 20);
        assertThat(setup.load(Flux.just("Golf", "Golf", "Judo")).block()).isEqualTo(2);
        assertThat(setup.load(Flux.just("Golf", "Judo", "Chess")).block()).isEqualTo(1);
        assertThat(repository.count().block()).isEqualTo(3);
    }

    @Test
    void parsesRakutenJsonAcrossPages() {
        WebClient webClient = WebClient.builder().exchangeFunction(request -> {
            int page = Integer.parseInt(request.url().getQuery().replaceAll(".*page=(\\d+).*", "$1"));
            String body = """
                    {"count":3,"page":%d,"pageCount":2,"Items":[{"Item":{"itemName":"  Ball %d "}},
                    {"Item":{"itemName":"Racket %d"}}]}""".formatted(page, page, page);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(body).build());
        }).build();
        var client = new RakutenClient(webClient, props());

        assertThat(client.itemNames().collectList().block())
                .containsExactly("Ball 1", "Racket 1", "Ball 2", "Racket 2");
    }

    private static RakutenProperties props() {
        return new RakutenProperties("http://localhost", "/x", "key", "sports", 30, 5);
    }
}
