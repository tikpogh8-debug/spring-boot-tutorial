#!/usr/bin/env bash
# Generates Python stubs from the shared proto and runs the client.
set -euo pipefail
cd "$(dirname "$0")"
[ -d .venv ] || python3 -m venv .venv
.venv/bin/pip install -q -r requirements.txt
.venv/bin/python -m grpc_tools.protoc -I ../src/main/proto --python_out=. --grpc_python_out=. ../src/main/proto/ping.proto
.venv/bin/python ping_client.py "$@"
