package org.jeecg.modules.bpm.monitor.dto;

import java.util.List;

public class InstanceDiagramVO {
    private String bpmnXml;
    private List<String> currentNodeIds;

    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public List<String> getCurrentNodeIds() { return currentNodeIds; }
    public void setCurrentNodeIds(List<String> currentNodeIds) { this.currentNodeIds = currentNodeIds; }
}
