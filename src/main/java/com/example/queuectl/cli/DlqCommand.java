package com.example.queuectl.cli;

import com.example.queuectl.core.JobRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "dlq", description = "Dead Letter Queue ops", subcommands = {DlqCommand.List.class, DlqCommand.Retry.class})
public class DlqCommand {
    @Component @CommandLine.Command(name = "list", description = "List dead jobs")
    public static class List implements Runnable {
        private final JobRepository repo; public List(JobRepository repo){this.repo=repo;}
        @Override public void run(){ repo.findDead(200).forEach(j -> System.out.println(j.brief())); }
    }
    @Component @CommandLine.Command(name = "retry", description = "Retry a dead job by id (resets attempts)")
    public static class Retry implements Runnable {
        @CommandLine.Parameters(index = "0") String id; private final JobRepository repo;
        public Retry(JobRepository repo){this.repo=repo;}
        @Override public void run(){ repo.retryDead(id); System.out.println("Retried DLQ job: "+id); }
    }
}
