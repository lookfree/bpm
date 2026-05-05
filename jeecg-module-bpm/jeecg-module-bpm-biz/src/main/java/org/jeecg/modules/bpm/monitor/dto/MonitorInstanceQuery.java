package org.jeecg.modules.bpm.monitor.dto;

import java.time.LocalDateTime;

public class MonitorInstanceQuery {
    private String defKey;
    private Integer defVersion;
    private Long applyDeptId;
    private Long applyUserId;
    private String state;
    private LocalDateTime startTimeFrom;
    private LocalDateTime startTimeTo;
    private int pageNo = 1;
    private int pageSize = 20;

    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public Integer getDefVersion() { return defVersion; }
    public void setDefVersion(Integer defVersion) { this.defVersion = defVersion; }
    public Long getApplyDeptId() { return applyDeptId; }
    public void setApplyDeptId(Long applyDeptId) { this.applyDeptId = applyDeptId; }
    public Long getApplyUserId() { return applyUserId; }
    public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public LocalDateTime getStartTimeFrom() { return startTimeFrom; }
    public void setStartTimeFrom(LocalDateTime startTimeFrom) { this.startTimeFrom = startTimeFrom; }
    public LocalDateTime getStartTimeTo() { return startTimeTo; }
    public void setStartTimeTo(LocalDateTime startTimeTo) { this.startTimeTo = startTimeTo; }
    public int getPageNo() { return pageNo; }
    public void setPageNo(int pageNo) { this.pageNo = pageNo; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
}
