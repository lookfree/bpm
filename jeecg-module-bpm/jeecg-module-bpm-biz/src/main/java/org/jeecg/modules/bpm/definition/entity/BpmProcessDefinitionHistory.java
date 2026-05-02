package org.jeecg.modules.bpm.definition.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

@TableName("bpm_process_definition_history")
public class BpmProcessDefinitionHistory {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    @TableField("def_id")
    private String defId;
    @TableField("def_key")
    private String defKey;
    private Integer version;
    @TableField("bpmn_xml")
    private String bpmnXml;
    @TableField("change_note")
    private String changeNote;
    @TableField("published_by")
    private String publishedBy;
    @TableField(value = "published_time", fill = FieldFill.INSERT)
    private Date publishedTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDefId() { return defId; }
    public void setDefId(String defId) { this.defId = defId; }
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public String getChangeNote() { return changeNote; }
    public void setChangeNote(String changeNote) { this.changeNote = changeNote; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
    public Date getPublishedTime() { return publishedTime; }
    public void setPublishedTime(Date publishedTime) { this.publishedTime = publishedTime; }
}
