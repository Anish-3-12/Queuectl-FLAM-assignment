package com.example.queuectl.cli;

import com.example.queuectl.core.JobRepository;
import com.example.queuectl.core.JobState;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "list", description = "List jobs, optional by state")
public class ListCommand implements Runnable {
    @CommandLine.Option(names = "--state", description = "pending|processing|completed|failed|dead")
    String state;
    private final JobRepository repo;
    public ListCommand(JobRepository repo) { this.repo = repo; }
    @Override public void run() {
        var list = state == null ? repo.findAll(100) : repo.findByState(JobState.valueOf(state));
        list.forEach(j -> System.out.println(j.brief()));
    }
}
