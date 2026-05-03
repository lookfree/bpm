package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BpmFormServiceJeecgImplTest {

    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    BpmFormServiceJeecgImpl svc = new BpmFormServiceJeecgImpl(jdbc);

    @Test
    void loadFormSchemaBuildsSchemaFromTables() {
        when(jdbc.queryForObject(contains("table_txt"), eq(String.class), eq("F1")))
                .thenReturn("采购申请");
        List<Map<String, Object>> fieldRows = new ArrayList<>();
        Map<String, Object> f1 = new LinkedHashMap<>();
        f1.put("db_field_name", "amount"); f1.put("db_field_txt", "金额");
        f1.put("field_show_type", "InputNumber"); f1.put("db_is_null", "0");
        Map<String, Object> f2 = new LinkedHashMap<>();
        f2.put("db_field_name", "remark"); f2.put("db_field_txt", "备注");
        f2.put("field_show_type", "Input"); f2.put("db_is_null", "1");
        fieldRows.add(f1); fieldRows.add(f2);
        when(jdbc.queryForList(contains("onl_cgform_field"), eq("F1"))).thenReturn(fieldRows);

        BpmFormSchema schema = svc.loadFormSchema("F1");

        assertThat(schema.getFormId()).isEqualTo("F1");
        assertThat(schema.getFormName()).isEqualTo("采购申请");
        assertThat(schema.getFields()).extracting("name").containsExactly("amount", "remark");
        assertThat(schema.getFields().get(0).getType()).isEqualTo("NUMBER");
        assertThat(schema.getFields().get(0).isRequired()).isTrue();
        assertThat(schema.getFields().get(1).isRequired()).isFalse();
    }

    @Test
    void loadFormDataDelegatesToDynamicTable() {
        when(jdbc.queryForObject(contains("table_name"), eq(String.class), eq("F1"))).thenReturn("t_purchase");
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("amount", 100);
        row.put("id", "bk1");
        when(jdbc.queryForList(contains("t_purchase"), eq("bk1"))).thenReturn(List.of(row));

        assertThat(svc.loadFormData("F1", "bk1")).containsEntry("amount", 100);
    }
}
