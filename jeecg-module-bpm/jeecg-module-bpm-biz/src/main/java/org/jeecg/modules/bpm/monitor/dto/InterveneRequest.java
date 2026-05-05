package org.jeecg.modules.bpm.monitor.dto;

public class InterveneRequest {

    private String action;
    private String targetUserId;
    private String comment;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetUserId() { return targetUserId; }
    public void setTargetUserId(String targetUserId) { this.targetUserId = targetUserId; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
