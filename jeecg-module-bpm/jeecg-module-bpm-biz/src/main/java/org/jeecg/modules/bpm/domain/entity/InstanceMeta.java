package org.jeecg.modules.bpm.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("bpm_instance_meta")
public class InstanceMeta {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("act_inst_id")
    private String actInstId;

    @TableField("def_id")
    private String defId;

    @TableField("def_version")
    private Integer defVersion;

    @TableField("business_key")
    private String businessKey;

    @TableField("apply_user_id")
    private Long applyUserId;

    @TableField("apply_dept_id")
    private Long applyDeptId;

    private String state;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActInstId() {
        return actInstId;
    }

    public void setActInstId(String actInstId) {
        this.actInstId = actInstId;
    }

    public String getDefId() {
        return defId;
    }

    public void setDefId(String defId) {
        this.defId = defId;
    }

    public Integer getDefVersion() {
        return defVersion;
    }

    public void setDefVersion(Integer defVersion) {
        this.defVersion = defVersion;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public Long getApplyUserId() {
        return applyUserId;
    }

    public void setApplyUserId(Long applyUserId) {
        this.applyUserId = applyUserId;
    }

    public Long getApplyDeptId() {
        return applyDeptId;
    }

    public void setApplyDeptId(Long applyDeptId) {
        this.applyDeptId = applyDeptId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
