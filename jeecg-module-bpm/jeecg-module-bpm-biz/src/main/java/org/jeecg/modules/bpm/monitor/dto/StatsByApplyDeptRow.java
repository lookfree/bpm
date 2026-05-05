package org.jeecg.modules.bpm.monitor.dto;

public class StatsByApplyDeptRow {
    private Long applyDeptId;
    private String applyDeptName;
    private long instanceCount;

    public Long getApplyDeptId() { return applyDeptId; }
    public void setApplyDeptId(Long applyDeptId) { this.applyDeptId = applyDeptId; }
    public String getApplyDeptName() { return applyDeptName; }
    public void setApplyDeptName(String applyDeptName) { this.applyDeptName = applyDeptName; }
    public long getInstanceCount() { return instanceCount; }
    public void setInstanceCount(long instanceCount) { this.instanceCount = instanceCount; }
}
