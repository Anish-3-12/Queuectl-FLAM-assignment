package com.example.queuectl.core;

import java.time.Instant;

public record JobRecord(
        String id,
        String command,
        JobState state,
        int attempts,
        int maxRetries,
        Instant createdAt,
        Instant updatedAt,
        Instant runAt,
        Integer priority,
        String lastError,
        String lockedBy,
        Instant lockedUntil
) {
    public static JobRecord newPending(String id, String command, int maxRetries, int priority){
        var now = Instant.now();
        return new JobRecord(id, command, JobState.pending, 0, maxRetries, now, now, now, priority, null, null, null);
    }
    public String brief(){
        return String.format("%s state=%s attempts=%d/%d cmd=\"%s\" runAt=%s", id, state, attempts, maxRetries, command, runAt);
    }
}
