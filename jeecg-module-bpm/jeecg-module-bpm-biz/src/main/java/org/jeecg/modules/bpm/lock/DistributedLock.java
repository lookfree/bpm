package org.jeecg.modules.bpm.lock;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface DistributedLock {
    <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Callable<T> task);
}
