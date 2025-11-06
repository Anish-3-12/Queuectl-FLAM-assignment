# queuectl — CLI Background Job Queue (Java + Spring Boot + Picocli + SQLite)

A production-grade background job queue system inspired by Sidekiq, Celery, and Resque — but built entirely using Java, Spring Boot, Picocli, and SQLite.

---

## Features

* Persistent job storage (SQLite)
* Multiple worker processes
* Exponential backoff retries
* Dead Letter Queue (DLQ)
* Job priority + scheduled `run_at` support
* Job output logging (`job-logs/<jobId>.log`)
* Metrics: completed jobs, retries today, avg attempts, etc.
* Clean CLI interface via Picocli
* Graceful worker shutdown
* Fully restart-safe

---

## 1. Setup Instructions

### Requirements

* Java 17+
* Maven 3.9+
* Windows / macOS / Linux supported

### Build the application

```
./mvnw clean package -DskipTests
```

### Create a shortcut (optional)

Linux/macOS:

```
alias queuectl='java -jar target/queuectl-1.0.0.jar'
```

Windows PowerShell: use a function instead of `alias`.

---

## 2. How to Run queuectl Using Two Terminals (IMPORTANT)

`queuectl` works like real job queues (Sidekiq, Celery), so **workers must run continuously**.

### Terminal A → Worker Terminal (leave running)

Start workers:

```
queuectl worker start --count 2
```

Worker output:

```
Starting 2 worker(s)...
w-0 completed <jobId>
w-1 scheduled retry for <jobId> in 4 seconds
```

Keep this terminal open. Do NOT run other commands here.

### Terminal B → Command Terminal

Use this terminal for:

* Enqueue jobs
* Check status
* List jobs
* Retry DLQ jobs
* View logs
* Change config

Examples:

```
queuectl enqueue --command "echo Hello" --max-retries 3
queuectl list --state pending
queuectl status
queuectl dlq list
queuectl dlq retry <jobId>
queuectl config get
queuectl config set backoff-base 3
```

Correct usage:
**Terminal A → workers**
**Terminal B → commands**

---

## 3. Usage Examples

### Enqueue jobs

```
queuectl enqueue --command "echo Hi"
queuectl enqueue --command "exit 1" --max-retries 2
queuectl enqueue '{"command":"echo JSON style","priority":10}'
```

### Start / Stop Workers

```
queuectl worker start --count 3
queuectl worker stop
```

### Status

```
queuectl status
```

Example:

```
Jobs: {completed=4, pending=1, dead=1}
Active workers: 2
Config: base=2, defaultMaxRetries=3
Metrics: completedLast5m=1, avgAttemptsCompleted=0.00, retriesToday=3
Logs: outputs are written to job-logs/<jobId>.log
```

### List jobs

```
queuectl list
queuectl list --state pending
queuectl list --state dead
```

### Dead Letter Queue

```
queuectl dlq list
queuectl dlq retry <jobId>
```

### Config

```
queuectl config get
queuectl config set max-retries 5
queuectl config set backoff-base 4
```

### View job logs

```
Get-Content job-logs/<jobId>.log       # Windows
cat job-logs/<jobId>.log               # Linux/macOS
```

---

## 4. Architecture Overview

### Core Components

* SQLite database (`queue.db`)
* Picocli CLI commands
* Spring Boot DI + repositories
* Workers execute shell commands asynchronously
* JobRepository handles:

  * row-mapping
  * timestamps
  * job claiming
  * retry mechanics
  * DLQ
  * repairing stuck jobs

### Job Lifecycle

```
pending
   | (worker claims)
processing
   | success
completed

   | fail + retry allowed
pending (scheduled with backoff)

   | fail + attempts exceeded
dead (DLQ)
```

### Key Features

#### 1. Exponential Backoff

```
delay = base ^ attempts (seconds)
```

Default base = 2

#### 2. Job Priority

```
ORDER BY priority DESC, created_at ASC
```

#### 3. Scheduled Jobs (`run_at`)

Workers pick:

```
run_at <= now
```

#### 4. Output Logging

```
job-logs/<jobId>.log
```

#### 5. Graceful Shutdown

`queuectl worker stop` sets a DB flag.
Workers finish the current job, then exit.

#### 6. Metrics

* `completedLast5m`
* `avgAttemptsCompleted`
* `retriesToday`

Displayed via:

```
queuectl status
```

---

## 5. Assumptions & Trade-offs

### Strengths

* Single-node safe (SQLite WAL mode)
* Durable without external dependencies
* Workers run in foreground — simple deployment

### Trade-offs

* Multi-node setups require PostgreSQL
* Job timeout behavior is minimal (expandable)
* Logs stored in files (lightweight, not centralized)

---

## 6. Testing Instructions (Step-by-Step)

### 1. Clean old data

```
rm queue.db
rm -rf job-logs
```

### 2. Start workers in Terminal A

```
queuectl worker start --count 2
```

### 3. Enqueue jobs in Terminal B

```
queuectl enqueue --command "echo A"
queuectl enqueue --command "echo B"
queuectl enqueue --command "exit 1" --max-retries 2
```

### 4. Check status

```
queuectl status
```

### 5. DLQ test

```
queuectl dlq list
queuectl dlq retry <deadJobId>
```

### 6. Check job logs

```
cat job-logs/<jobId>.log
```

---

## Bonus Features Implemented

* ✅ Job priority
* ✅ Scheduled jobs (`run_at`)
* ✅ Job output logging
* ✅ Metrics (`status` command)
