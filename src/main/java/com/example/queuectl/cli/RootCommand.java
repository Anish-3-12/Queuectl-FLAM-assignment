package com.example.queuectl.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "queuectl",
        description = "CLI-based background job queue",
        mixinStandardHelpOptions = true,
        subcommands = {
                EnqueueCommand.class,
                WorkerCommand.class,
                StatusCommand.class,
                ListCommand.class,
                DlqCommand.class,
                ConfigCommand.class,
                JobsRepairCommand.class
        }
)
public class RootCommand implements Runnable {

    private final CommandLine.IFactory factory;

    public RootCommand(CommandLine.IFactory factory) {
        this.factory = factory;
    }

    @Override
    public void run() {
        new CommandLine(this, factory).usage(System.out);
    }

    public int execute(String... args) {
        return new CommandLine(this, factory).execute(args);
    }
}
