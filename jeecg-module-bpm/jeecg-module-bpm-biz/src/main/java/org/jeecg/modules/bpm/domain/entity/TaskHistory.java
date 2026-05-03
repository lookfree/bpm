package org.jeecg.modules.bpm.domain.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("bpm_task_history")
public class TaskHistory {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("act_task_id")
    private String actTaskId;

    @TableField("inst_id")
    private String instId;

    @TableField("node_id")
    private String nodeId;

    @TableField("assignee_id")
    private Long assigneeId;

    private String action;

    @TableField("`comment`")
    private String comment;

    private String attachments;

    @TableField("op_time")
    private LocalDateTime opTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActTaskId() {
        return actTaskId;
    }

    public void setActTaskId(String actTaskId) {
        this.actTaskId = actTaskId;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public LocalDateTime getOpTime() {
        return opTime;
    }

    public void setOpTime(LocalDateTime opTime) {
        this.opTime = opTime;
    }
}
