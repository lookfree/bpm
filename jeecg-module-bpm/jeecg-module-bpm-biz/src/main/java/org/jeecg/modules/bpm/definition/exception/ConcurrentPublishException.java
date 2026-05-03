package org.jeecg.modules.bpm.definition.exception;

public class ConcurrentPublishException extends RuntimeException {
    public ConcurrentPublishException(String lockKey) {
        super("Another publish is in progress for lock: " + lockKey);
    }
}
