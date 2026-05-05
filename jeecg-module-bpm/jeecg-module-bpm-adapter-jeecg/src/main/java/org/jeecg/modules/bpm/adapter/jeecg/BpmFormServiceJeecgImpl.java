package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.dto.BpmFormField;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

public class BpmFormServiceJeecgImpl implements BpmFormService {

    private static final Logger log = LoggerFactory.getLogger(BpmFormServiceJeecgImpl.class);

    private final JdbcOperations jdbc;

    public BpmFormServiceJeecgImpl(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public BpmFormSchema loadFormSchema(String formId) {
        // onl_cgform_head: id, table_name, table_txt (display name)
        String name = jdbc.queryForObject(
                "SELECT table_txt FROM onl_cgform_head WHERE id = ?", String.class, formId);
        // onl_cgform_field: cgform_head_id, db_field_name, db_field_txt, field_show_type, db_is_null
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT db_field_name, db_field_txt, field_show_type, db_is_null " +
                "FROM onl_cgform_field WHERE cgform_head_id = ? ORDER BY sort_no",
                formId);
        BpmFormSchema schema = new BpmFormSchema();
        schema.setFormId(formId);
        schema.setFormName(name);
        List<BpmFormField> fields = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            BpmFormField f = new BpmFormField();
            f.setName((String) row.get("db_field_name"));
            f.setLabel((String) row.get("db_field_txt"));
            f.setType(mapJeecgType((String) row.get("field_show_type")));
            f.setRequired("0".equals(String.valueOf(row.get("db_is_null"))));
            fields.add(f);
        }
        schema.setFields(fields);
        return schema;
    }

    @Override
    public String saveFormSubmission(String formId, Map<String, Object> data) {
        // Persist into the dynamic form table
        // onl_cgform_head.table_name gives us the physical table
        String tableName = jdbc.queryForObject(
                "SELECT table_name FROM onl_cgform_head WHERE id = ?", String.class, formId);
        String id = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> row = new LinkedHashMap<>(data);
        row.put("id", id);
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        List<Object> args = new ArrayList<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (cols.length() > 0) { cols.append(","); vals.append(","); }
            cols.append("`").append(e.getKey()).append("`");
            vals.append("?");
            args.add(e.getValue());
        }
        jdbc.update("INSERT INTO `" + tableName + "` (" + cols + ") VALUES (" + vals + ")", args.toArray());
        return id;
    }

    @Override
    public Map<String, Object> loadFormData(String formId, String businessKey) {
        String tableName = jdbc.queryForObject(
                "SELECT table_name FROM onl_cgform_head WHERE id = ?", String.class, formId);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT * FROM `" + tableName + "` WHERE id = ?", businessKey);
        return rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    private static String mapJeecgType(String jeecgType) {
        if (jeecgType == null) return "TEXT";
        switch (jeecgType) {
            case "InputNumber": return "NUMBER";
            case "Date": case "Datetime": return "DATE";
            case "Switch": return "BOOL";
            case "file": case "image": return "FILE";
            default: return "TEXT";
        }
    }
}
