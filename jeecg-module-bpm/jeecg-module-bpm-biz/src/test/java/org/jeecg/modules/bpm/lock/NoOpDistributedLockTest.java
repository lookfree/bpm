package org.jeecg.modules.bpm.lock;

import org.jeecg.modules.bpm.definition.exception.ConcurrentPublishException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

class NoOpDistributedLockTest {

    @Test
    void secondCallerThrowsConcurrentPublish() throws Exception {
        NoOpDistributedLock lock = new NoOpDistributedLock();
        AtomicBoolean firstStarted = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<?> f1 = exec.submit(() -> {
            lock.executeWithLock("key1", 5, 30, TimeUnit.SECONDS, () -> {
                firstStarted.set(true);
                latch.countDown();
                Thread.sleep(2000);
                return null;
            });
        });

        latch.await(); // wait for thread1 to acquire lock
        assertThatThrownBy(() ->
            lock.executeWithLock("key1", 100, 30, TimeUnit.MILLISECONDS, () -> "ok")
        ).isInstanceOf(ConcurrentPublishException.class);

        f1.cancel(true);
        exec.shutdownNow();
    }

    @Test
    void singleCallerSucceeds() {
        NoOpDistributedLock lock = new NoOpDistributedLock();
        String result = lock.executeWithLock("key2", 1, 5, TimeUnit.SECONDS, () -> "done");
        assertThat(result).isEqualTo("done");
    }
}
