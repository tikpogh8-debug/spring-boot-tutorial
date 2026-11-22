package com.tutorial.reactive;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("rakuten")
public record RakutenProperties(String baseUrl, String path, String applicationId, String keyword, int hits,
        int maxPages) {
}
