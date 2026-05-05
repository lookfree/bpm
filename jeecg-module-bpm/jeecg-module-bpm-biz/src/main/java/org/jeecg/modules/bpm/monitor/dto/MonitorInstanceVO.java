package org.jeecg.modules.bpm.monitor.dto;

import java.time.LocalDateTime;

public class MonitorInstanceVO {
    private String id;
    private String actInstId;
    private String defKey;
    private String defName;
    private Integer defVersion;
    private String businessKey;
    private Long applyUserId;
    private String applyUserName;
    private Long applyDeptId;
    private String applyDeptName;
    private String state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActInstId() { return actInstId; }
    public void setActInstId(String actInstId) { this.actInstId = actInstId; }
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getDefName() { return defName; }
    public void setDefName(String defName) { this.defName = defName; }
    public Integer getDefVersion() { return defVersion; }
    public void setDefVersion(Integer defVersion) { this.defVersion = defVersion; }
    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
    public Long getApplyUserId() { return applyUserId; }
    public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
    public String getApplyUserName() { return applyUserName; }
    public void setApplyUserName(String applyUserName) { this.applyUserName = applyUserName; }
    public Long getApplyDeptId() { return applyDeptId; }
    public void setApplyDeptId(Long applyDeptId) { this.applyDeptId = applyDeptId; }
    public String getApplyDeptName() { return applyDeptName; }
    public void setApplyDeptName(String applyDeptName) { this.applyDeptName = applyDeptName; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
