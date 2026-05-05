package org.jeecg.modules.bpm.monitor.dto;

public class StatsByDefinitionRow {
    private String defKey;
    private String defName;
    private long instanceCount;
    private long completedCount;
    private long overdueCount;
    private Double avgDurationMs;
    private Double completionRate;
    private Double overdueRate;

    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getDefName() { return defName; }
    public void setDefName(String defName) { this.defName = defName; }
    public long getInstanceCount() { return instanceCount; }
    public void setInstanceCount(long instanceCount) { this.instanceCount = instanceCount; }
    public long getCompletedCount() { return completedCount; }
    public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
    public long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
    public Double getAvgDurationMs() { return avgDurationMs; }
    public void setAvgDurationMs(Double avgDurationMs) { this.avgDurationMs = avgDurationMs; }
    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
    public Double getOverdueRate() { return overdueRate; }
    public void setOverdueRate(Double overdueRate) { this.overdueRate = overdueRate; }
}
