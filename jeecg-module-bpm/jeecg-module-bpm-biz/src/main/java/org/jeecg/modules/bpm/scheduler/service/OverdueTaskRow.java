package org.jeecg.modules.bpm.scheduler.service;

import java.util.Date;

public class OverdueTaskRow {
    private String taskId;
    private String actInstId;
    private String nodeId;
    private String assignee;
    private Date createTime;
    private Double timeoutHours;
    private String timeoutAction;
    private String instMetaId;
    private String defId;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getActInstId() { return actInstId; }
    public void setActInstId(String actInstId) { this.actInstId = actInstId; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Double getTimeoutHours() { return timeoutHours; }
    public void setTimeoutHours(Double timeoutHours) { this.timeoutHours = timeoutHours; }
    public String getTimeoutAction() { return timeoutAction; }
    public void setTimeoutAction(String timeoutAction) { this.timeoutAction = timeoutAction; }
    public String getInstMetaId() { return instMetaId; }
    public void setInstMetaId(String instMetaId) { this.instMetaId = instMetaId; }
    public String getDefId() { return defId; }
    public void setDefId(String defId) { this.defId = defId; }
}
