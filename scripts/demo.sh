#!/usr/bin/env bash
set -euo pipefail

./mvnw -q -DskipTests package
JAR=target/queuectl-1.0.0.jar

function q() { java -jar "$JAR" "$@"; }

echo "Enqueue hello and fail job"
q enqueue '{"id":"job-ok","command":"echo hi","max_retries":3}'
q enqueue '{"id":"job-bad","command":"nosuchcommand","max_retries":2}'

echo "Start 2 workers (will exit when stop flag set)" &
q worker start --count 2 &
WPID=$!

sleep 2
q status

sleep 5
q status

# Signal stop
q worker stop
wait $WPID || true

q list --state completed
q dlq list