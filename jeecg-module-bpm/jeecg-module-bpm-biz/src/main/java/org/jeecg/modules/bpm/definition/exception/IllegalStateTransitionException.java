package org.jeecg.modules.bpm.definition.exception;

public class IllegalStateTransitionException extends RuntimeException {
    private final String from;
    private final String to;

    public IllegalStateTransitionException(String from, String to) {
        super(String.format("Illegal state transition: %s -> %s", from, to));
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }
}
