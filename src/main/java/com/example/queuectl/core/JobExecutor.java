package com.example.queuectl.core;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class JobExecutor {

    // Minimal: hard-code a safe timeout (seconds). Change if you want.
    private static final long TIMEOUT_SECONDS = 120;

    public record Result(boolean success, String error){}

    public Result execute(JobRecord job){
        try {
            // WINDOWS SAFE EXECUTION (you’re on Windows)
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", job.command());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture combined stdout/stderr
            StringBuilder out = new StringBuilder();
            try (var r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }

            // ⏱️ Timeout
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished){
                process.destroyForcibly();
                // still log what we captured
                persistOutput(job.id(), out.toString());
                return new Result(false, "timeout after " + TIMEOUT_SECONDS + "s");
            }

            int exit = process.exitValue();
            // 📝 Persist output to file for auditing (no DB migration)
            persistOutput(job.id(), out.toString());

            if (exit == 0) return new Result(true, null);
            return new Result(false, "exit=" + exit + " output=" + truncate(out.toString(), 2000));
        } catch (Exception e) {
            return new Result(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static void persistOutput(String jobId, String content) {
        try {
            Path dir = Path.of("job-logs");
            Files.createDirectories(dir);
            Path file = dir.resolve(jobId + ".log");
            Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
        } catch (Exception ignore) {
            // best-effort logging; never fail job because of IO
        }
    }

    private static String truncate(String s, int n){
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n);
    }
}
