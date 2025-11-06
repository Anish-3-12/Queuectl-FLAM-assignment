package com.example.queuectl.cli;

import com.example.queuectl.core.WorkerService;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "worker", description = "Start/stop workers", subcommands = {WorkerCommand.Start.class, WorkerCommand.Stop.class})
public class WorkerCommand {

    @Component
    @CommandLine.Command(name = "start", description = "Start N workers in foreground. Uses DB signal to stop.")
    public static class Start implements Runnable {
        @CommandLine.Option(names = "--count", defaultValue = "1", description = "Number of workers")
        int count;
        private final WorkerService workers;
        public Start(WorkerService workers) { this.workers = workers; }
        @Override public void run() { workers.startForeground(count); }
    }

    @Component
    @CommandLine.Command(name = "stop", description = "Signal all workers to stop gracefully")
    public static class Stop implements Runnable {
        private final WorkerService workers;
        public Stop(WorkerService workers) { this.workers = workers; }
        @Override public void run() { workers.signalStopAll(); System.out.println("Stop signal set. Active workers will finish current job."); }
    }
}
