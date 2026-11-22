package com.tutorial.grpc;

import com.tutorial.grpc.proto.PingRequest;
import com.tutorial.grpc.proto.PingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingServerTest {

    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void start() throws Exception {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
                .addService(new PingServer.PingServiceImpl()).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    }

    @AfterEach
    void stop() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void pingReturnsPong() {
        var stub = PingServiceGrpc.newBlockingStub(channel);
        assertEquals("Pong", stub.ping(PingRequest.newBuilder().setMessage("Ping").build()).getMessage());
    }

    @Test
    void otherMessageIsReportedAsUnknown() {
        var stub = PingServiceGrpc.newBlockingStub(channel);
        assertEquals("Unknown message: Hi", stub.ping(PingRequest.newBuilder().setMessage("Hi").build()).getMessage());
    }
}
