package com.tutorial.avro;

import org.apache.avro.Schema;

import java.io.IOException;
import java.io.InputStream;

final class AvroConfig {
    static final String BOOTSTRAP = System.getProperty("bootstrap", "localhost:9092");
    static final String REGISTRY = System.getProperty("registry", "http://localhost:8081");
    static final String TOPIC = System.getProperty("topic", "users");

    private AvroConfig() {
    }

    /** Loads src/main/resources/avro/user-v{version}.avsc. */
    static Schema loadSchema(int version) {
        String path = "avro/user-v" + version + ".avsc";
        try (InputStream in = AvroConfig.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("No schema file " + path);
            }
            return new Schema.Parser().parse(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
