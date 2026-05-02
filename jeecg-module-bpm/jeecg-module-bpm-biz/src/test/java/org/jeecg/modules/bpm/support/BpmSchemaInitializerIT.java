package org.jeecg.modules.bpm.support;

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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { org.jeecg.modules.bpm.test.BpmTestApplication.class })
@ActiveProfiles("test")
@Testcontainers
class BpmSchemaInitializerIT {

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
    void createsTwoBpmTables() {
        Integer defCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema=DATABASE() AND table_name='bpm_process_definition'",
                Integer.class);
        Integer histCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema=DATABASE() AND table_name='bpm_process_definition_history'",
                Integer.class);
        assertThat(defCount).isEqualTo(1);
        assertThat(histCount).isEqualTo(1);
    }
}
