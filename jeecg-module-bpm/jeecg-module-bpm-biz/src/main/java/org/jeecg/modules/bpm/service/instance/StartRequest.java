package org.jeecg.modules.bpm.service.instance;

import java.util.Map;

public class StartRequest {
    private String defId;
    private String formId;
    private Map<String, Object> formData;

    public static StartRequest of(String defId, String formId, Map<String, Object> formData) {
        StartRequest r = new StartRequest();
        r.defId = defId;
        r.formId = formId;
        r.formData = formData;
        return r;
    }

    public String getDefId() { return defId; }
    public void setDefId(String defId) { this.defId = defId; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }
}
