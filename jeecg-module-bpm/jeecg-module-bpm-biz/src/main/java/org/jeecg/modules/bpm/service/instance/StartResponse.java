package org.jeecg.modules.bpm.service.instance;

public class StartResponse {
    private String instanceId;
    private String actInstId;
    private String businessKey;

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public String getActInstId() { return actInstId; }
    public void setActInstId(String actInstId) { this.actInstId = actInstId; }
    public String getBusinessKey() { return businessKey; }
    public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
}
