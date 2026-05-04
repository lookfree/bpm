package org.jeecg.modules.bpm.sandbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("bpm_sandbox_run")
public class SandboxRun {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("def_id_draft")
    private String defIdDraft;

    @TableField("runner_id")
    private Long runnerId;

    private String result;

    @TableField("log")
    private String log;

    @TableField("start_time")
    private Date startTime;

    @TableField("end_time")
    private Date endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDefIdDraft() { return defIdDraft; }
    public void setDefIdDraft(String defIdDraft) { this.defIdDraft = defIdDraft; }
    public Long getRunnerId() { return runnerId; }
    public void setRunnerId(Long runnerId) { this.runnerId = runnerId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getLog() { return log; }
    public void setLog(String log) { this.log = log; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
