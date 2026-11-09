package com.tutorial.avro;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * Sends one User message serialized with Avro.
 * Usage: AvroProducer [schemaVersion=1|2] [id=1] [name=Alice]
 * The schema is registered in Schema Registry (subject "users-value") on the first send.
 */
public class AvroProducer {

    public static void main(String[] args) throws Exception {
        int version = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        long id = args.length > 1 ? Long.parseLong(args[1]) : 1;
        String name = args.length > 2 ? args[2] : "Alice";

        Schema schema = AvroConfig.loadSchema(version);
        GenericRecord user = new GenericData.Record(schema);
        user.put("id", id);
        user.put("name", name);
        if (schema.getField("email") != null) {
            user.put("email", name.toLowerCase() + "@example.com");
        }

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, AvroConfig.BOOTSTRAP);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put("schema.registry.url", AvroConfig.REGISTRY);

        try (KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(props)) {
            RecordMetadata meta = producer.send(new ProducerRecord<>(AvroConfig.TOPIC, String.valueOf(id), user)).get();
            System.out.printf("Sent with schema v%d: %s -> %s-%d@%d%n", version, user, meta.topic(), meta.partition(), meta.offset());
        }
    }
}
