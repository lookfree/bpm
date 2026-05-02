package org.jeecg.modules.bpm.spi.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BpmFormSchemaTest {

    @Test
    void equalsAndHashCodeRespectFields() {
        BpmFormField f1 = new BpmFormField();
        f1.setName("amount"); f1.setLabel("金额"); f1.setType("NUMBER"); f1.setRequired(true);

        BpmFormSchema a = new BpmFormSchema();
        a.setFormId("form_x"); a.setFormName("申请单"); a.setFields(Arrays.asList(f1));
        BpmFormSchema b = new BpmFormSchema();
        b.setFormId("form_x"); b.setFormName("申请单"); b.setFields(Arrays.asList(f1));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void fieldsDefaultsToEmptyListNotNull() {
        BpmFormSchema s = new BpmFormSchema();
        assertThat(s.getFields()).isNotNull().isEmpty();
        s.setFields(null);
        assertThat(s.getFields()).isNotNull().isEmpty();
    }
}
