package org.jeecg.modules.bpm.adapter.jeecg;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BpmOrgServiceJeecgImplTest {

    static JdbcTemplate jdbc;
    static BpmOrgServiceJeecgImpl svc;

    @BeforeAll
    static void setupDb() {
        EmbeddedDatabase db = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:db/sys-tables.sql")
                .build();
        jdbc = new JdbcTemplate(db);
        svc = new BpmOrgServiceJeecgImpl(jdbc);
    }

    @Test
    void findUsersByRole() {
        assertThat(svc.findUsersByRole("admin")).containsExactlyInAnyOrder(1L, 2L);
        assertThat(svc.findUsersByRole("auditor")).containsExactly(3L);
        assertThat(svc.findUsersByRole("ghost")).isEmpty();
    }

    @Test
    void findDeptLeaders() {
        assertThat(svc.findDeptLeaders(100L)).containsExactly(9L);
    }

    @Test
    void findUpperDeptLeaders() {
        assertThat(svc.findUpperDeptLeaders(100L)).containsExactly(4L);
    }

    @Test
    void findUsersByPosition() {
        assertThat(svc.findUsersByPosition("PM")).containsExactly(7L);
    }

    @Test
    void isUserActive() {
        assertThat(svc.isUserActive(1L)).isTrue();
        assertThat(svc.isUserActive(99999L)).isFalse();
    }
}
