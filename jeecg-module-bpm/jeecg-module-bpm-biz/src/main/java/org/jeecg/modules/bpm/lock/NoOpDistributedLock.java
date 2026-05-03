package org.jeecg.modules.bpm.lock;

import org.jeecg.modules.bpm.definition.exception.ConcurrentPublishException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ConditionalOnProperty(name = "bpm.lock.enabled", havingValue = "false")
public class NoOpDistributedLock implements DistributedLock {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Callable<T> task) {
        ReentrantLock lk = locks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            if (!lk.tryLock(waitTime, unit)) {
                throw new ConcurrentPublishException(key);
            }
            try {
                return task.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lk.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
