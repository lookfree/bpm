package org.jeecg.modules.bpm.service.assignee;

import java.util.Map;

public class ResolveContext {
    private String procInstId;
    private String nodeId;
    private Long applyUserId;
    private Long applyDeptId;
    private Map<String, Object> formData;
    private Map<String, Object> strategyPayload;
    private Map<String, Object> processVars;

    private ResolveContext() {}

    // --- getters ---

    public String getProcInstId() { return procInstId; }
    public String getNodeId() { return nodeId; }
    public Long getApplyUserId() { return applyUserId; }
    public Long getApplyDeptId() { return applyDeptId; }
    public Map<String, Object> getFormData() { return formData; }
    public Map<String, Object> getStrategyPayload() { return strategyPayload; }
    public Map<String, Object> getProcessVars() { return processVars; }

    // --- setters ---

    public void setProcInstId(String procInstId) { this.procInstId = procInstId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }
    public void setApplyUserId(Long applyUserId) { this.applyUserId = applyUserId; }
    public void setApplyDeptId(Long applyDeptId) { this.applyDeptId = applyDeptId; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }
    public void setStrategyPayload(Map<String, Object> strategyPayload) { this.strategyPayload = strategyPayload; }
    public void setProcessVars(Map<String, Object> processVars) { this.processVars = processVars; }

    // --- builder ---

    public static Builder builder() { return new Builder(); }

    /** Returns a new Builder pre-populated with all fields of this instance (toBuilder equivalent). */
    public Builder toBuilder() {
        return new Builder()
                .procInstId(this.procInstId)
                .nodeId(this.nodeId)
                .applyUserId(this.applyUserId)
                .applyDeptId(this.applyDeptId)
                .formData(this.formData)
                .strategyPayload(this.strategyPayload)
                .processVars(this.processVars);
    }

    public static class Builder {
        private final ResolveContext ctx = new ResolveContext();

        public Builder procInstId(String v) { ctx.procInstId = v; return this; }
        public Builder nodeId(String v) { ctx.nodeId = v; return this; }
        public Builder applyUserId(Long v) { ctx.applyUserId = v; return this; }
        public Builder applyDeptId(Long v) { ctx.applyDeptId = v; return this; }
        public Builder formData(Map<String, Object> v) { ctx.formData = v; return this; }
        public Builder strategyPayload(Map<String, Object> v) { ctx.strategyPayload = v; return this; }
        public Builder processVars(Map<String, Object> v) { ctx.processVars = v; return this; }

        public ResolveContext build() { return ctx; }
    }
}
