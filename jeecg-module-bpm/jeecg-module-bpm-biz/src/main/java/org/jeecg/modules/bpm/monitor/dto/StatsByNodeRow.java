package org.jeecg.modules.bpm.monitor.dto;

public class StatsByNodeRow {
    private String defKey;
    private String nodeId;
    private String nodeName;
    private long taskCount;
    private long overdueCount;
    private Double avgDurationMs;
    private Double overdueRate;

    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public long getTaskCount() { return taskCount; }
    public void setTaskCount(long taskCount) { this.taskCount = taskCount; }
    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
    public Double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(Double avgDurationMs) { this.avgDurationMs = avgDurationMs; }
    public Double getOverdueRate() { return overdueRate; }
    public void setOverdueRate(Double overdueRate) { this.overdueRate = overdueRate; }
}
