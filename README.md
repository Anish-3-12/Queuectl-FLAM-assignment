queuectl — CLI Background Job Queue

Java + Spring Boot + Picocli + SQLite

A lightweight, production-grade background job queue system with:

Persistent job storage (SQLite)
Multi-worker execution
Exponential backoff retries
Dead Letter Queue (DLQ)
Graceful shutdown
Configurable retry/backoff parameters
Clean CLI built with Picocli

Requirements

Java 17+

Maven 3.9+

Works on Windows, macOS, and Linux

Setup Instructions
1] Build the project
mvn -q -DskipTests package

2️] (Optional) Create an alias

Linux/macOS

alias queuectl='java -jar target/queuectl-1.0.0.jar'


Windows PowerShell

function queuectl { java -jar "target/queuectl-1.0.0.jar" @args }

Usage Examples
1] Enqueue Jobs
queuectl enqueue --command "echo Hello" --max-retries 3
queuectl enqueue --command "sleep 2" --priority 10
queuectl enqueue '{"id":"j1","command":"echo JSON payload","max_retries":2}'

2] Start & Stop Workers
queuectl worker start --count 3     # Run 3 workers
queuectl worker stop                # Gracefully stop

3] Job Inspection
queuectl status                     # Summary
queuectl list --state pending       # List only pending jobs
queuectl dlq list                   # Dead Letter Queue
queuectl dlq retry <jobId>          # Retry a dead job

Configuration
queuectl config get
queuectl config set backoff-base 3
queuectl config set max-retries 5

Execution Model
Workers:
Poll for pending jobs eligible to run (run_at <= now)
Claim a job atomically using an SQL UPDATE with conditions:

UPDATE jobs 
SET state='processing', locked_by=?, locked_until=NOW+5min 
WHERE id=? AND state='pending' AND lock expired

Run command via:

Windows: cmd /c <command>

Linux/macOS: bash -lc <command>

Send heartbeats to workers table

Respect global stop flag

Backoff
delaySeconds = backoff_base ^ attempts


backoff_base is configurable via CLI.

✅ Persistence

SQLite database: queue.db

Automatically created with schema from schema.sql

WAL mode enabled for durability

Jobs survive restart

Workers stop and resume safely without corruption

⚙️ Assumptions & Trade-offs
✅ SQLite is ideal for single-machine, simple deployments.

For multi-node clusters, switch to PostgreSQL with SKIP LOCKED.

✅ Workers run in foreground

Useful for debugging.
A systemd service (or Windows service) can wrap worker start.

✅ Backoff strategy is exponential only

Could extend to:

jitter

fixed delay

linear backoff

✅ Output is printed to stdout

Not persisted; could be stored for auditing in a future version.

✅ Jobs run locally on OS shell

Sandboxing (Docker, chroot) not included for simplicity.

✅ Testing Instructions

You MUST validate the following before submission:

✅ 1. Basic Job Execution
queuectl enqueue --command "echo Hello"
queuectl worker start --count 1


Expected: Job completes.

✅ 2. Retry + Dead Letter Queue
queuectl enqueue --command "exit 1" --max-retries 2
queuectl worker start --count 1


Expected:

Attempts increase

Job moves to DLQ after final failure
Check:

queuectl dlq list

✅ 3. Retry DLQ Job
queuectl dlq retry <jobId>
queuectl worker start --count 1

✅ 4. Parallel Workers
queuectl enqueue --command "echo A"
queuectl enqueue --command "echo B"
queuectl worker start --count 2


Expected: Both run simultaneously.

✅ 5. Persistence Across Restart
queuectl enqueue --command "sleep 5"
# Kill terminal without stopping workers
java -jar target/queuectl-1.0.0.jar status


Expected: Job is still present in DB.

📁 Project Structure
src/
  main/java/com/example/queuectl/
    cli/          # All Picocli commands
    core/         # Job logic, repository, worker engine
    config/       # Global config, Picocli config
resources/
  schema.sql      # DB schema
  application.yml # SQLite, retry base, defaults
queue.db          # SQLite persistent file

🧩 Bonus Features (Optional)

Job priorities ✅ (already supported)

Future ideas:

Job timeouts / SIGKILL

Job logs in DB

Web dashboard

Metrics (Prometheus)

✅ Ready for Submission

This README now meets all required evaluation criteria, including:

✅ Setup instructions
✅ Usage examples
✅ Architecture overview
✅ Assumptions & trade-offs
✅ Testing instructions

If you'd like, I can format this into a pretty GitHub README.md with emojis, badges, and colorful sections.
