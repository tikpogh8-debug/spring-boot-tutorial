package com.tutorial.reactive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

/** Reads item names from the Rakuten Ichiba Item Search API, page by page, as a Flux. */
@Component
public class RakutenClient {

    private static final int MAX_NAME_LENGTH = 255;

    private final WebClient webClient;
    private final RakutenProperties props;

    @Autowired
    public RakutenClient(WebClient.Builder builder, RakutenProperties props) {
        this(builder.baseUrl(props.baseUrl()).build(), props);
    }

    RakutenClient(WebClient webClient, RakutenProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    /** Item names across up to {@code max-pages} pages. Pages are fetched lazily, one at a time. */
    public Flux<String> itemNames() {
        return fetchPage(1)
                .expand(page -> {
                    int next = page.path("page").asInt(1) + 1;
                    boolean more = next <= page.path("pageCount").asInt(0) && next <= props.maxPages();
                    return more ? fetchPage(next) : Mono.empty();
                })
                .flatMapIterable(RakutenClient::items)
                .map(item -> item.path("Item").isMissingNode() ? item : item.path("Item"))
                .map(item -> item.path("itemName").asString(""))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name);
    }

    private Mono<JsonNode> fetchPage(int page) {
        return webClient.get()
                .uri(uri -> uri.path(props.path())
                        .queryParam("applicationId", props.applicationId())
                        .queryParam("keyword", props.keyword())
                        .queryParam("hits", props.hits())
                        .queryParam("page", page)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class);
    }

    private static Iterable<JsonNode> items(JsonNode page) {
        return page.path("Items");
    }
}
