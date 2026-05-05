package org.jeecg.modules.bpm.monitor.dto;

public class StatsByApplyDeptTrendRow {
    private Long applyDeptId;
    private String applyDeptName;
    private String bucket;
    private long instanceCount;

    public Long getApplyDeptId() { return applyDeptId; }
    public void setApplyDeptId(Long applyDeptId) { this.applyDeptId = applyDeptId; }
    public String getApplyDeptName() { return applyDeptName; }
    public void setApplyDeptName(String applyDeptName) { this.applyDeptName = applyDeptName; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public long getInstanceCount() { return instanceCount; }
    public void setInstanceCount(long instanceCount) { this.instanceCount = instanceCount; }
}
