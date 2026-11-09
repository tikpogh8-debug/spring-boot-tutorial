package com.tutorial.avro;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Reads the topic from the beginning, deserializes every message and prints it together with the
 * Confluent wire format header: byte 0 = magic byte (0), bytes 1-4 = schema id (big-endian int).
 * Usage: AvroConsumer [seconds-to-run=30]
 */
public class AvroConsumer {

    public static void main(String[] args) {
        long runSeconds = args.length > 0 ? Long.parseLong(args[0]) : 30;

        // We read raw bytes so we can show the header, then hand the bytes to the Avro deserializer.
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, AvroConfig.BOOTSTRAP);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "avro-demo-" + System.currentTimeMillis());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        try (KafkaAvroDeserializer avro = new KafkaAvroDeserializer();
             KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            avro.configure(Map.of("schema.registry.url", AvroConfig.REGISTRY), false);
            consumer.subscribe(List.of(AvroConfig.TOPIC));
            System.out.println("Listening on topic '" + AvroConfig.TOPIC + "' for " + runSeconds + "s ...");

            long end = System.currentTimeMillis() + runSeconds * 1000;
            while (System.currentTimeMillis() < end) {
                for (ConsumerRecord<String, byte[]> rec : consumer.poll(Duration.ofSeconds(1))) {
                    byte[] bytes = rec.value();
                    ByteBuffer header = ByteBuffer.wrap(bytes);
                    byte magic = header.get();
                    int schemaId = header.getInt();
                    Object value = avro.deserialize(rec.topic(), bytes);
                    System.out.printf("offset=%d key=%s | magic=%d schemaId=%d (bytes 1-4 = %s) | value=%s%n",
                            rec.offset(), rec.key(), magic, schemaId,
                            HexFormat.of().formatHex(bytes, 1, 5), value);
                }
            }
        }
    }
}
