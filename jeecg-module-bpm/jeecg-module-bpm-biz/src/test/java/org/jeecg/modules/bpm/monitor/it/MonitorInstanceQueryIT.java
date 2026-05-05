package org.jeecg.modules.bpm.monitor.it;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceQuery;
import org.jeecg.modules.bpm.monitor.service.MonitorQueryService;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BpmTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class MonitorInstanceQueryIT {

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

    @Autowired MonitorQueryService monitorQueryService;
    @Autowired BpmProcessDefinitionMapper definitionMapper;
    @Autowired InstanceMetaMapper instanceMetaMapper;
    @Autowired JdbcTemplate jdbc;

    private String defId1;
    private String defId2;

    @BeforeEach
    void setUp() {
        jdbc.execute("DELETE FROM bpm_instance_meta");
        jdbc.execute("DELETE FROM bpm_process_definition");

        BpmProcessDefinition def1 = new BpmProcessDefinition();
        def1.setDefKey("leave");
        def1.setName("请假流程");
        def1.setVersion(1);
        def1.setState("PUBLISHED");
        def1.setCategory("DEFAULT");
        def1.setTenantId("default");
        definitionMapper.insert(def1);
        defId1 = def1.getId();

        BpmProcessDefinition def2 = new BpmProcessDefinition();
        def2.setDefKey("purchase");
        def2.setName("采购流程");
        def2.setVersion(1);
        def2.setState("PUBLISHED");
        def2.setCategory("DEFAULT");
        def2.setTenantId("default");
        definitionMapper.insert(def2);
        defId2 = def2.getId();

        // inst1: leave, RUNNING, dept=1, user=10
        insertMeta("inst1", defId1, 1, "biz1", 10L, 1L, "RUNNING",
                LocalDateTime.now().minusDays(5));
        // inst2: leave, COMPLETED, dept=2, user=11
        insertMeta("inst2", defId1, 1, "biz2", 11L, 2L, "COMPLETED",
                LocalDateTime.now().minusDays(3));
        // inst3: purchase, RUNNING, dept=1, user=12
        insertMeta("inst3", defId2, 1, "biz3", 12L, 1L, "RUNNING",
                LocalDateTime.now().minusDays(1));
    }

    private void insertMeta(String actInstId, String defId, int defVersion,
                             String bizKey, Long userId, Long deptId,
                             String state, LocalDateTime startTime) {
        InstanceMeta m = new InstanceMeta();
        m.setActInstId(actInstId);
        m.setDefId(defId);
        m.setDefVersion(defVersion);
        m.setBusinessKey(bizKey);
        m.setApplyUserId(userId);
        m.setApplyDeptId(deptId);
        m.setState(state);
        m.setStartTime(startTime);
        instanceMetaMapper.insert(m);
    }

    @Test
    void noFilterReturnsAll() {
        when(orgService.findUserName(any())).thenReturn(null);
        when(orgService.findDeptName(any())).thenReturn(null);

        Map<String, Object> page = monitorQueryService.listInstances(new MonitorInstanceQuery());
        assertThat((Long) page.get("total")).isEqualTo(3L);
    }

    @Test
    void filterByDefKey() {
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        q.setDefKey("leave");
        Map<String, Object> page = monitorQueryService.listInstances(q);
        assertThat((Long) page.get("total")).isEqualTo(2L);
    }

    @Test
    void filterByStateRunning() {
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        q.setState("RUNNING");
        Map<String, Object> page = monitorQueryService.listInstances(q);
        assertThat((Long) page.get("total")).isEqualTo(2L);
    }

    @Test
    void filterByApplyDeptId() {
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        q.setApplyDeptId(2L);
        Map<String, Object> page = monitorQueryService.listInstances(q);
        assertThat((Long) page.get("total")).isEqualTo(1L);
    }

    @Test
    void filterByDateRange() {
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        q.setStartTimeFrom(LocalDateTime.now().minusDays(4));
        Map<String, Object> page = monitorQueryService.listInstances(q);
        // inst2 (minusDays 3) and inst3 (minusDays 1) fall within range
        assertThat((Long) page.get("total")).isEqualTo(2L);
    }

    @Test
    void pagination() {
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        q.setPageSize(2);
        q.setPageNo(1);
        Map<String, Object> page = monitorQueryService.listInstances(q);
        assertThat((Long) page.get("total")).isEqualTo(3L);
        @SuppressWarnings("unchecked")
        List<?> records = (List<?>) page.get("records");
        assertThat(records).hasSize(2);
    }
}
