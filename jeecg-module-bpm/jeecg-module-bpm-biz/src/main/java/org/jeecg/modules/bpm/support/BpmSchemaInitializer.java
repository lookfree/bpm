package org.jeecg.modules.bpm.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class BpmSchemaInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(BpmSchemaInitializer.class);
    private static final String RESOURCE_PATH = "db/bpm-schema-mysql.sql";

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public BpmSchemaInitializer(JdbcTemplate jdbcTemplate,
                                @Value("${bpm.schema.auto-init:true}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initSchema() {
        if (!enabled) {
            LOG.info("[bpm-schema] auto-init disabled, skip");
            return;
        }
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            String raw = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            // Strip line comments first so a leading "-- ..." doesn't disqualify the whole
            // statement chunk after the split-by-semicolon step.
            String stripped = Arrays.stream(raw.split("\\R"))
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            Arrays.stream(stripped.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(sql -> {
                        try {
                            jdbcTemplate.execute(sql);
                        } catch (org.springframework.dao.DataAccessException e) {
                            // Tolerate "duplicate key name" (MySQL error 1061) for index creation
                            // and "Duplicate column" (1060) — idempotent re-runs
                            Throwable cause = e.getCause();
                            if (cause instanceof java.sql.SQLException) {
                                int code = ((java.sql.SQLException) cause).getErrorCode();
                                if (code == 1061 /* ER_DUP_KEYNAME */ || code == 1060 /* ER_DUP_FIELDNAME */) {
                                    LOG.debug("[bpm-schema] tolerated idempotent: {}", e.getMessage());
                                    return;
                                }
                            }
                            throw e;
                        }
                    });
            LOG.info("[bpm-schema] init complete");
        } catch (Exception ex) {
            LOG.error("[bpm-schema] init FAILED", ex);
            throw new IllegalStateException("BPM schema init failed", ex);
        }
    }
}
