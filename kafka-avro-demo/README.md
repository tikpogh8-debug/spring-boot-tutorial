# Kafka + Avro demo

Producer and consumer that serialize `User` messages with Avro, using Confluent Schema Registry.
Run Maven commands from the **repository root**. The docker-compose file uses KRaft (no ZooKeeper).

```bash
docker-compose -f kafka-avro-demo/docker-compose.yml up -d      # Kafka :9092, Schema Registry :8081
docker exec kafka-avro-demo-kafka-1 kafka-topics --bootstrap-server kafka:29092 \
  --create --topic users --partitions 1 --replication-factor 1

mvn -f kafka-avro-demo/pom.xml compile
# producer: AvroProducer <schemaVersion 1|2> <id> <name>
mvn -f kafka-avro-demo/pom.xml exec:java -Dexec.mainClass=com.tutorial.avro.AvroProducer -Dexec.args="1 1 Alice"
mvn -f kafka-avro-demo/pom.xml exec:java -Dexec.mainClass=com.tutorial.avro.AvroProducer -Dexec.args="2 2 Bob"
# consumer: reads from the beginning, prints the wire-format header and the decoded record
mvn -f kafka-avro-demo/pom.xml exec:java -Dexec.mainClass=com.tutorial.avro.AvroConsumer -Dexec.args="10"
```

## Different schemas

`src/main/resources/avro/user-v1.avsc` has `id`, `name`. `user-v2.avsc` adds an optional `email`
(with a default, so it is backward compatible). Observed output:

```
offset=0 key=1 | magic=0 schemaId=1 (bytes 1-4 = 00000001) | value={"id": 1, "name": "Alice"}
offset=1 key=2 | magic=0 schemaId=2 (bytes 1-4 = 00000002) | value={"id": 2, "name": "Bob", "email": "bob@example.com"}
```

Each message starts with the Confluent wire format: byte 0 = magic byte `0`, bytes 1-4 = schema id
(big-endian int), then the Avro body. The same ids appear in the registry:

```bash
curl localhost:8081/subjects/users-value/versions   # [1,2]
curl localhost:8081/schemas/ids/2
```
