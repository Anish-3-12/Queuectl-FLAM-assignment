package com.example.queuectl.cli;

import com.example.queuectl.core.JobRepository;
import com.example.queuectl.core.ConfigRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "status", description = "Show summary of job states & active workers")
public class StatusCommand implements Runnable {
    private final JobRepository jobs; private final ConfigRepository cfg;
    public StatusCommand(JobRepository jobs, ConfigRepository cfg) { this.jobs = jobs; this.cfg = cfg; }
    @Override public void run() {
        var counts = jobs.countsByState();
        System.out.println("Jobs: " + counts);
        System.out.println("Active workers (pidless count via heartbeats): " + jobs.countActiveWorkers());
        System.out.println("Config: base=" + cfg.getBackoffBase() + ", defaultMaxRetries=" + cfg.getDefaultMaxRetries());
    }
}
