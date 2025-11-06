package com.example.queuectl.cli;

import com.example.queuectl.core.ConfigRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "config", description = "Manage configuration", subcommands = {ConfigCommand.Get.class, ConfigCommand.Set.class})
public class ConfigCommand {
    @Component @CommandLine.Command(name = "get", description = "Show config")
    public static class Get implements Runnable {
        private final ConfigRepository repo; public Get(ConfigRepository r){this.repo=r;}
        @Override public void run(){ System.out.println("base="+repo.getBackoffBase()+" defaultMaxRetries="+repo.getDefaultMaxRetries()); }
    }
    @Component @CommandLine.Command(name = "set", description = "Set config values")
    public static class Set implements Runnable {
        @CommandLine.Parameters(index="0") String key; @CommandLine.Parameters(index="1") String value;
        private final ConfigRepository repo; public Set(ConfigRepository r){this.repo=r;}
        @Override public void run(){ switch (key){
            case "max-retries" -> repo.setDefaultMaxRetries(Integer.parseInt(value));
            case "backoff-base" -> repo.setBackoffBase(Integer.parseInt(value));
            default -> throw new IllegalArgumentException("Unknown key: "+key);
        } System.out.println("ok"); }
    }
}