package com.example.queuectl.cli;

import com.example.queuectl.core.JobRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "jobs",
        description = "Job maintenance utilities",
        subcommands = { JobsRepairCommand.Repair.class }
)
public class JobsRepairCommand {

    @Component
    @CommandLine.Command(name = "repair", description = "Unlock all jobs stuck in processing -> set to pending")
    public static class Repair implements Runnable {
        private final JobRepository repo;
        public Repair(JobRepository repo){ this.repo = repo; }
        @Override public void run() {
            int n = repo.repairProcessing();
            System.out.println("Repaired jobs: " + n);
        }
    }
}
