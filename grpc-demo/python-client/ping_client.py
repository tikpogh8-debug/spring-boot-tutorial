"""Python gRPC client: sends "Ping", prints the response. Uses the same proto as the Java code."""
import sys

import grpc

import ping_pb2
import ping_pb2_grpc


def main() -> None:
    target = sys.argv[1] if len(sys.argv) > 1 else "localhost:8080"
    with grpc.insecure_channel(target) as channel:
        stub = ping_pb2_grpc.PingServiceStub(channel)
        response = stub.Ping(ping_pb2.PingRequest(message="Ping", sender="python-client"))
        print(f"Response from server: {response.message} (timestamp={response.timestamp_millis})")


if __name__ == "__main__":
    main()
