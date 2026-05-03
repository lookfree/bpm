package org.jeecg.modules.bpm.service.form;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.spi.dto.BpmFormField;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormPermissionMergerTest {

    FormPermissionMerger m = new FormPermissionMerger(new ObjectMapper());

    private BpmFormSchema schemaWith(String... names) {
        BpmFormSchema s = new BpmFormSchema();
        List<BpmFormField> fields = new java.util.ArrayList<>();
        for (String n : names) {
            BpmFormField f = new BpmFormField();
            f.setName(n);
            f.setType("STRING");
            fields.add(f);
        }
        s.setFields(fields);
        return s;
    }

    @Test void readWriteKeepsField() {
        BpmFormSchema schema = schemaWith("amount");
        String perm = "{\"fields\":{\"amount\":{\"perm\":\"READ_WRITE\"}}}";
        BpmFormSchema merged = m.merge(schema, perm);
        assertThat(merged.getFields()).extracting("name").containsExactly("amount");
        assertThat(merged.getFields().get(0).isReadonly()).isFalse();
    }

    @Test void readOnlyMarksReadonlyTrue() {
        BpmFormSchema schema = schemaWith("amount");
        String perm = "{\"fields\":{\"amount\":{\"perm\":\"READ_ONLY\"}}}";
        BpmFormSchema merged = m.merge(schema, perm);
        assertThat(merged.getFields().get(0).isReadonly()).isTrue();
    }

    @Test void hiddenRemovesField() {
        BpmFormSchema schema = schemaWith("amount", "remark");
        String perm = "{\"fields\":{\"remark\":{\"perm\":\"HIDDEN\"}}}";
        BpmFormSchema merged = m.merge(schema, perm);
        assertThat(merged.getFields()).extracting("name").containsExactly("amount");
    }

    @Test void requiredOverlayForcesTrue() {
        BpmFormSchema schema = schemaWith("amount");
        String perm = "{\"fields\":{\"amount\":{\"perm\":\"READ_WRITE\",\"required\":true}}}";
        BpmFormSchema merged = m.merge(schema, perm);
        assertThat(merged.getFields().get(0).isRequired()).isTrue();
    }

    @Test void nullPermReturnsSchemaUnchanged() {
        BpmFormSchema schema = schemaWith("amount");
        assertThat(m.merge(schema, null)).isSameAs(schema);
    }
}
