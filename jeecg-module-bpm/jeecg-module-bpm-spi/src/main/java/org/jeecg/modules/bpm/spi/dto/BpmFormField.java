package org.jeecg.modules.bpm.spi.dto;

import java.util.Objects;

public class BpmFormField {
    private String name;
    private String label;
    /** STRING / NUMBER / DATE / BOOLEAN / FILE / USER */
    private String type;
    private boolean required;
    private boolean readonly;
    private String defaultValue;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public boolean isReadonly() { return readonly; }
    public void setReadonly(boolean readonly) { this.readonly = readonly; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public BpmFormField copy() {
        BpmFormField f = new BpmFormField();
        f.name = this.name;
        f.label = this.label;
        f.type = this.type;
        f.required = this.required;
        f.readonly = this.readonly;
        f.defaultValue = this.defaultValue;
        return f;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmFormField)) return false;
        BpmFormField that = (BpmFormField) o;
        return required == that.required
                && readonly == that.readonly
                && Objects.equals(name, that.name)
                && Objects.equals(label, that.label)
                && Objects.equals(type, that.type)
                && Objects.equals(defaultValue, that.defaultValue);
    }
    @Override public int hashCode() { return Objects.hash(name, label, type, required, readonly, defaultValue); }
}
