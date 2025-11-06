package com.example.queuectl;

import com.example.queuectl.core.JobRepository;
import com.example.queuectl.core.JobRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueueCtlSmokeTest {
    @Autowired JobRepository repo;

    @Test void enqueue_and_read(){
        var j = JobRecord.newPending("test-1","echo hello",3,0);
        repo.insert(j);
        var all = repo.findAll(10);
        assertThat(all.stream().anyMatch(x -> x.id().equals("test-1"))).isTrue();
    }
}
