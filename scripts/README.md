# queuectl — CLI Background Job Queue (Java + Spring Boot + Picocli + SQLite)

A minimal, production-grade background job queue with:
- Enqueue + persistent storage (SQLite)
- Multiple workers
- Exponential backoff retries
- Dead Letter Queue (DLQ)
- Graceful stop via DB signal
- Clean CLI via Picocli

## Requirements
- Java 17+
- Maven 3.9+

## Setup
```
mvn -q -DskipTests package
alias queuectl='java -jar target/queuectl-1.0.0.jar'
```

## Usage
```
# Enqueue
queuectl enqueue '{"id":"job1","command":"echo Hello","max_retries":3}'
queuectl enqueue --command "sleep 2" --max-retries 4 --priority 10

# Start workers (foreground); stop from another terminal
queuectl worker start --count 3
queuectl worker stop

# Status & lists
queuectl status
queuectl list --state pending
queuectl dlq list
queuectl dlq retry job1

# Config
queuectl config get
queuectl config set backoff-base 3
queuectl config set max-retries 5
```

## Architecture Overview
- **Storage**: SQLite (`queue.db`), schema created on boot via `schema.sql`.
- **Job lifecycle**: `pending → processing → (completed | failed→pending with backoff | dead)`.
- **Locking**: optimistic claim via `UPDATE ... WHERE state='pending' AND lock free`. Workers heartbeat in `workers` table.
- **Backoff**: `delay = base^attempts` seconds, `base` configurable (`config.backoff_base`).
- **Graceful stop**: set `control.stop_all=1` using `queuectl worker stop`. Workers check each loop and exit after current job.
- **Execution**: commands run via `bash -lc <command>`; combined stdout/stderr captured; non-zero exit → failure.

## Assumptions & Trade-offs
- Single-node safe; SQLite WAL mode gives good durability. For multi-host, replace with Postgres and SKIP LOCKED.
- `worker start` is foreground; external stop through DB flag. For daemonization, use a systemd service.
- Default timeout 10 minutes per job; adjust in code if needed.
- Output stored only in logs (stdout); consider persisting logs for auditing as a future enhancement.

## Testing
- Run `scripts/demo.sh` for a quick E2E.
- JUnit smoke test included (`QueueCtlSmokeTest`).

## Bonus Ideas
- Add `run_at` scheduling UI, priorities (already supported), and per-job timeout/kill.
- Add metrics endpoint or Prometheus counters.
- Add simple web dashboard (Spring Boot MVC) listing jobs.
```
```

---

## Notes
- The CLI is packaged as a single executable JAR; you can symlink it as `queuectl` for convenience.
- To persist across restarts: the SQLite `queue.db` file is created next to the JAR; keep it.
- For Windows, change the `ProcessBuilder` to use `cmd /c` instead of `bash -lc` or detect OS.
