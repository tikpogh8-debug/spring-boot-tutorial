package com.tutorial.grpc;

import com.tutorial.grpc.proto.PingRequest;
import com.tutorial.grpc.proto.PingResponse;
import com.tutorial.grpc.proto.PingServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;

/** gRPC server: answers "Ping" with "Pong". Listens on port 8080 by default. */
public class PingServer {

    static class PingServiceImpl extends PingServiceGrpc.PingServiceImplBase {
        @Override
        public void ping(PingRequest request, StreamObserver<PingResponse> responseObserver) {
            System.out.println("Received: message=" + request.getMessage() + ", sender=" + request.getSender());
            String reply = "Ping".equalsIgnoreCase(request.getMessage()) ? "Pong" : "Unknown message: " + request.getMessage();
            responseObserver.onNext(PingResponse.newBuilder()
                    .setMessage(reply)
                    .setTimestampMillis(System.currentTimeMillis())
                    .build());
            responseObserver.onCompleted();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Server server = ServerBuilder.forPort(port).addService(new PingServiceImpl()).build().start();
        System.out.println("gRPC server started on port " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
    }
}
