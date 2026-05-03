package org.jeecg.modules.bpm.lock;

import org.jeecg.modules.bpm.definition.exception.ConcurrentPublishException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "bpm.lock.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonDistributedLock implements DistributedLock {

    private final RedissonClient redisson;

    public RedissonDistributedLock(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Callable<T> task) {
        RLock lock = redisson.getLock(key);
        try {
            if (!lock.tryLock(waitTime, leaseTime, unit)) {
                throw new ConcurrentPublishException(key);
            }
            try {
                return task.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (lock.isHeldByCurrentThread()) lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
