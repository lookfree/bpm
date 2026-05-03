package org.jeecg.modules.bpm.service.form;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.spi.dto.BpmFormField;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FormPermissionMerger {

    private final ObjectMapper json;

    public FormPermissionMerger(ObjectMapper json) {
        this.json = json;
    }

    public BpmFormSchema merge(BpmFormSchema schema, String formPermJson) {
        if (formPermJson == null || formPermJson.isBlank()) return schema;
        Map<String, Object> doc;
        try {
            doc = json.readValue(formPermJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return schema;
        }
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> fields =
                (Map<String, Map<String, Object>>) doc.getOrDefault("fields", Collections.emptyMap());

        BpmFormSchema out = new BpmFormSchema();
        out.setFormId(schema.getFormId());
        out.setFormName(schema.getFormName());
        List<BpmFormField> kept = new ArrayList<>();
        for (BpmFormField f : schema.getFields()) {
            Map<String, Object> override = fields.get(f.getName());
            if (override == null) { kept.add(f); continue; }
            String perm = (String) override.get("perm");
            if ("HIDDEN".equals(perm)) continue;
            BpmFormField clone = f.copy();
            if ("READ_ONLY".equals(perm)) clone.setReadonly(true);
            if (Boolean.TRUE.equals(override.get("required"))) clone.setRequired(true);
            kept.add(clone);
        }
        out.setFields(kept);
        return out;
    }
}
