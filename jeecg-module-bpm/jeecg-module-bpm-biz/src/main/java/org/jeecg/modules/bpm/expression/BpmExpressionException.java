package org.jeecg.modules.bpm.expression;

public class BpmExpressionException extends RuntimeException {
    public BpmExpressionException(String msg)              { super(msg); }
    public BpmExpressionException(String msg, Throwable t) { super(msg, t); }
}
