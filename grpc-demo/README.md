# gRPC Ping/Pong demo

Java gRPC server (port 8080), Java client and Python client, all sharing `src/main/proto/ping.proto`.
Stop the Spring Boot app first: it also uses port 8080.

Run all commands from the **repository root** (the root `.mvn/maven.config` is picked up there).

```bash
# tests (in-process server)
mvn -f grpc-demo/pom.xml test

# 1. server
mvn -f grpc-demo/pom.xml compile exec:java -Dexec.mainClass=com.tutorial.grpc.PingServer
# 2. Java client (new terminal)
mvn -f grpc-demo/pom.xml exec:java -Dexec.mainClass=com.tutorial.grpc.PingClient
# 3. Python client (new terminal; creates a venv and generates stubs from the same proto)
grpc-demo/python-client/run.sh
```

Both clients print `Response from server: Pong (timestamp=...)`.
