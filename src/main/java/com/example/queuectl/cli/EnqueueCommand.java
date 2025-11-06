package com.example.queuectl.cli;

import com.example.queuectl.core.JobRepository;
import com.example.queuectl.core.JobState;
import com.example.queuectl.core.JobRecord;
import com.example.queuectl.util.Jsons;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Instant;
import java.util.UUID;

@Component
@CommandLine.Command(name = "enqueue", description = "Add a new job to the queue. Accepts raw JSON or flags.")
public class EnqueueCommand implements Runnable {
    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Job JSON, e.g. '{\"id\":\"job1\",\"command\":\"echo hi\"}'")
    String jobJson;

    @CommandLine.Option(names = "--command", description = "Command to execute")
    String command;

    @CommandLine.Option(names = "--max-retries", defaultValue = "3", description = "Max retries")
    int maxRetries;

    @CommandLine.Option(names = "--priority", defaultValue = "0", description = "Priority (higher runs first)")
    int priority;

    private final JobRepository repo;

    public EnqueueCommand(JobRepository repo) { this.repo = repo; }

    @Override public void run() {
        JobRecord job;
        if (jobJson != null) {
            JsonNode n = Jsons.read(jobJson);
            String id = n.hasNonNull("id") ? n.get("id").asText() : UUID.randomUUID().toString();
            String cmd = n.hasNonNull("command") ? n.get("command").asText() : command;
            int mr = n.hasNonNull("max_retries") ? n.get("max_retries").asInt() : maxRetries;
            int pr = n.hasNonNull("priority") ? n.get("priority").asInt() : priority;
            job = JobRecord.newPending(id, cmd, mr, pr);
        } else {
            if (command == null || command.isBlank()) throw new CommandLine.ParameterException(new CommandLine(this), "--command required if no JSON");
            job = JobRecord.newPending(UUID.randomUUID().toString(), command, maxRetries, priority);
        }
        repo.insert(job);
        System.out.println("Enqueued job: " + job.id());
    }
}
