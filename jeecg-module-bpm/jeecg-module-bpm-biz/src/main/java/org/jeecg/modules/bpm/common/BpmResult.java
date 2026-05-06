package org.jeecg.modules.bpm.common;

/**
 * Matches jeecg-boot's Result<T> JSON shape so the jeecg frontend axios hook can parse it.
 * Keeps bpm-biz free of a jeecg compile-time dependency.
 */
public class BpmResult<T> {

    private boolean success;
    private String message = "";
    private int code;
    private T result;
    private long timestamp = System.currentTimeMillis();

    private BpmResult() {}

    public static <T> BpmResult<T> ok(T data) {
        BpmResult<T> r = new BpmResult<>();
        r.success = true;
        r.code = 200;
        r.result = data;
        return r;
    }

    public static <T> BpmResult<T> ok() {
        BpmResult<T> r = new BpmResult<>();
        r.success = true;
        r.code = 200;
        return r;
    }

    public static <T> BpmResult<T> error(String msg) {
        BpmResult<T> r = new BpmResult<>();
        r.success = false;
        r.code = 500;
        r.message = msg;
        return r;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public int getCode() { return code; }
    public T getResult() { return result; }
    public long getTimestamp() { return timestamp; }
}
