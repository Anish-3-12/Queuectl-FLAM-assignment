package com.example.queuectl.cli;

import com.example.queuectl.core.ConfigRepository;
import com.example.queuectl.core.JobRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.Locale;

@Component
@CommandLine.Command(
        name = "status",
        description = "Show summary of job states & active workers"
)
public class StatusCommand implements Runnable {
    private final JobRepository jobs;
    private final ConfigRepository cfg;

    public StatusCommand(JobRepository jobs, ConfigRepository cfg) {
        this.jobs = jobs;
        this.cfg = cfg;
    }

    @Override
    public void run() {
        var counts = jobs.countsByState();
        int active = jobs.countActiveWorkers();
        int base = cfg.getBackoffBase();
        int defMax = cfg.getDefaultMaxRetries();

        System.out.println("Jobs: " + counts);
        System.out.println("Active workers (pidless count via heartbeats): " + active);
        System.out.println("Config: base=" + base + ", defaultMaxRetries=" + defMax);

        // --- lightweight metrics (no schema changes) ---
        int completed5m = jobs.completedLastMinutes(5);
        double avgAttemptsDone = jobs.avgAttemptsCompleted();
        int retriesToday = jobs.totalRetriesToday();

        System.out.println("Metrics: completedLast5m=" + completed5m
                + ", avgAttemptsCompleted=" + String.format(Locale.ROOT, "%.2f", avgAttemptsDone)
                + ", retriesToday=" + retriesToday);
        System.out.println("Logs: outputs are written to job-logs/<jobId>.log");
    }
}
