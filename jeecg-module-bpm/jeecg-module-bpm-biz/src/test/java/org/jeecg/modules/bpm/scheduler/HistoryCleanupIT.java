package org.jeecg.modules.bpm.scheduler;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.scheduler.cleanup.HistoryCleanupMapper;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.jeecg.modules.bpm.test.BpmTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BpmTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class HistoryCleanupIT {

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

    @MockBean BpmOrgService orgService;
    @MockBean BpmFormService formService;
    @MockBean BpmUserContext userContext;

    @Autowired HistoryCleanupMapper cleanupMapper;
    @Autowired BpmProcessDefinitionMapper definitionMapper;
    @Autowired InstanceMetaMapper instanceMetaMapper;
    @Autowired JdbcTemplate jdbc;

    private String defMetaId;

    @BeforeEach
    void setUp() {
        jdbc.execute("DELETE FROM bpm_task_history");
        jdbc.execute("DELETE FROM bpm_instance_meta");
        jdbc.execute("DELETE FROM bpm_process_definition");

        BpmProcessDefinition def = new BpmProcessDefinition();
        def.setDefKey("cleanup_test");
        def.setName("Cleanup Test");
        def.setVersion(1);
        def.setState("PUBLISHED");
        def.setCategory("DEFAULT");
        def.setTenantId("default");
        def.setActDefId("act_cleanup_test:1:1");
        definitionMapper.insert(def);
        defMetaId = def.getId();
    }

    private InstanceMeta insertCompletedInstance(LocalDateTime endTime) {
        InstanceMeta meta = new InstanceMeta();
        meta.setActInstId("proc_" + System.nanoTime());
        meta.setDefId(defMetaId);
        meta.setDefVersion(1);
        meta.setApplyUserId(1L);
        meta.setState("COMPLETED");
        meta.setStartTime(endTime.minusHours(1));
        meta.setEndTime(endTime);
        instanceMetaMapper.insert(meta);
        return meta;
    }

    @Test
    void deletesBeyondRetentionAndKeepsRecent() {
        LocalDateTime old = LocalDateTime.now().minusDays(200);
        LocalDateTime recent = LocalDateTime.now().minusDays(30);

        InstanceMeta oldMeta = insertCompletedInstance(old);
        InstanceMeta recentMeta = insertCompletedInstance(recent);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(180);
        int deleted = cleanupMapper.deleteInstanceMeta(cutoff);

        assertThat(deleted).isEqualTo(1);
        assertThat(instanceMetaMapper.selectById(oldMeta.getId())).isNull();
        assertThat(instanceMetaMapper.selectById(recentMeta.getId())).isNotNull();
    }

    @Test
    void runningInstancesAreNotDeleted() {
        InstanceMeta meta = new InstanceMeta();
        meta.setActInstId("proc_running");
        meta.setDefId(defMetaId);
        meta.setDefVersion(1);
        meta.setApplyUserId(1L);
        meta.setState("RUNNING");
        meta.setStartTime(LocalDateTime.now().minusDays(400));
        instanceMetaMapper.insert(meta);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        int deleted = cleanupMapper.deleteInstanceMeta(cutoff);

        assertThat(deleted).isEqualTo(0);
        assertThat(instanceMetaMapper.selectById(meta.getId())).isNotNull();
    }
}
