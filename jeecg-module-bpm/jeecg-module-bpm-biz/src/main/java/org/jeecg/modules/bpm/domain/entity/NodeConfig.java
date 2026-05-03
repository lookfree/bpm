package org.jeecg.modules.bpm.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("bpm_node_config")
public class NodeConfig {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("def_id")
    private String defId;

    @TableField("node_id")
    private String nodeId;

    @TableField("assignee_strategy")
    private String assigneeStrategy;

    @TableField("multi_mode")
    private String multiMode;

    @TableField("form_perm")
    private String formPerm;

    @TableField("timeout_hours")
    private Integer timeoutHours;

    @TableField("timeout_action")
    private String timeoutAction;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDefId() {
        return defId;
    }

    public void setDefId(String defId) {
        this.defId = defId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getAssigneeStrategy() {
        return assigneeStrategy;
    }

    public void setAssigneeStrategy(String assigneeStrategy) {
        this.assigneeStrategy = assigneeStrategy;
    }

    public String getMultiMode() {
        return multiMode;
    }

    public void setMultiMode(String multiMode) {
        this.multiMode = multiMode;
    }

    public String getFormPerm() {
        return formPerm;
    }

    public void setFormPerm(String formPerm) {
        this.formPerm = formPerm;
    }

    public Integer getTimeoutHours() {
        return timeoutHours;
    }

    public void setTimeoutHours(Integer timeoutHours) {
        this.timeoutHours = timeoutHours;
    }

    public String getTimeoutAction() {
        return timeoutAction;
    }

    public void setTimeoutAction(String timeoutAction) {
        this.timeoutAction = timeoutAction;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
