package org.jeecg.modules.bpm.schema;

import org.jeecg.modules.bpm.test.BpmTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BpmTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class BpmSchemaP2Test {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("bpm.schema.auto-init", () -> "true");
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void allP2TablesExistAfterStartup() {
        List<String> tables = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'bpm_%'",
                String.class);
        assertThat(tables).contains(
                "bpm_node_config",
                "bpm_assignee_strategy",
                "bpm_form_binding",
                "bpm_instance_meta",
                "bpm_task_history");
    }

    @Test
    void taskHistoryHasUniqueIndexOnActTaskIdAndAction() {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bpm_task_history' " +
                "AND INDEX_NAME = 'uk_bpm_task_history_task_action' AND NON_UNIQUE = 0", Integer.class);
        assertThat(cnt).isGreaterThanOrEqualTo(2);
    }
}
