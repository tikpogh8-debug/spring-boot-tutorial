package com.tutorial.grpc;

import com.tutorial.grpc.proto.PingRequest;
import com.tutorial.grpc.proto.PingResponse;
import com.tutorial.grpc.proto.PingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

/** Java gRPC client: sends "Ping", prints the response. */
public class PingClient {

    public static void main(String[] args) throws InterruptedException {
        String target = args.length > 0 ? args[0] : "localhost:8080";
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        try {
            PingServiceGrpc.PingServiceBlockingStub stub = PingServiceGrpc.newBlockingStub(channel);
            PingResponse response = stub.ping(PingRequest.newBuilder()
                    .setMessage("Ping")
                    .setSender("java-client")
                    .build());
            System.out.println("Response from server: " + response.getMessage()
                    + " (timestamp=" + response.getTimestampMillis() + ")");
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
