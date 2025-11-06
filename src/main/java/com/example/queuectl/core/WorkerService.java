package com.example.queuectl.core;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
public class WorkerService {
    private final JobRepository jobs; private final JobExecutor exec; private final ConfigRepository cfg;
    public WorkerService(JobRepository jobs, JobExecutor exec, ConfigRepository cfg){ this.jobs=jobs; this.exec=exec; this.cfg=cfg; }

    public void startForeground(int count){
        System.out.printf("Starting %d worker(s). Press Ctrl+C to stop or run `queuectl worker stop`.\n", count);
        ExecutorService pool = Executors.newFixedThreadPool(count);
        List<Future<?>> futures = new ArrayList<>();
        for (int i=0;i<count;i++){
            String workerName = "w-"+i+"-"+ProcessHandle.current().pid();
            futures.add(pool.submit(() -> runLoop(workerName)));
        }
        ShutdownSignals.addHook(() -> { pool.shutdown(); try { pool.awaitTermination(30, TimeUnit.SECONDS);} catch (InterruptedException ignored) {} });
        try { for (Future<?> f: futures) f.get(); } catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
    }

    public void signalStopAll(){ cfg.setStopAll(true); }

    private void runLoop(String workerName){
        while (true){
            if (cfg.isStopAll()) { System.out.println(workerName+": stop signal received"); break; }
            jobs.heartbeat(workerName);
            JobRecord job = jobs.claimNext(workerName);
            if (job == null){ sleep(500); continue; }
            var res = exec.execute(job);
            if (res.success()){
                jobs.complete(job.id());
                System.out.println(workerName+" completed "+job.id());
            } else {
                int base = cfg.getBackoffBase();
                int delay = (int) Math.pow(base, Math.max(1, job.attempts()));
                jobs.failAndScheduleRetry(job.id(), job.attempts(), job.maxRetries(), delay, res.error());
                System.out.println(workerName+" failed "+job.id()+"; retry in "+delay+"s: "+res.error());
            }
        }
    }

    private static void sleep(long ms){ try { Thread.sleep(ms);} catch (InterruptedException ignored) {} }
}
