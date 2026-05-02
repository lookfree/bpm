package org.jeecg.modules.bpm.spi.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 与具体宿主表单引擎解耦的统一表单结构 */
public class BpmFormSchema {
    private String formId;
    private String formName;
    private List<BpmFormField> fields = new ArrayList<>();

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getFormName() { return formName; }
    public void setFormName(String formName) { this.formName = formName; }
    public List<BpmFormField> getFields() { return fields; }
    public void setFields(List<BpmFormField> fields) { this.fields = fields == null ? new ArrayList<>() : fields; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmFormSchema)) return false;
        BpmFormSchema that = (BpmFormSchema) o;
        return Objects.equals(formId, that.formId)
                && Objects.equals(formName, that.formName)
                && Objects.equals(fields, that.fields);
    }
    @Override public int hashCode() { return Objects.hash(formId, formName, fields); }
}
