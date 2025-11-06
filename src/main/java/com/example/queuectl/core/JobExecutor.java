package com.example.queuectl.core;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class JobExecutor {

    public record Result(boolean success, String error){}

    public Result execute(JobRecord job){
        try {
            // WINDOWS SAFE EXECUTION
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", job.command());
            pb.redirectErrorStream(true);

            Process process = pb.start();

            StringBuilder out = new StringBuilder();
            try (var r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished){
                process.destroyForcibly();
                return new Result(false, "timeout");
            }

            int exit = process.exitValue();
            if (exit == 0) return new Result(true, null);

            return new Result(false, "exit=" + exit + " output=" + out);

        } catch (Exception e) {
            return new Result(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
