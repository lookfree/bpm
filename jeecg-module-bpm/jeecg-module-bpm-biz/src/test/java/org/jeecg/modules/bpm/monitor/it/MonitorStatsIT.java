package org.jeecg.modules.bpm.monitor.it;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.mapper.NodeConfigMapper;
import org.jeecg.modules.bpm.domain.entity.NodeConfig;
import org.jeecg.modules.bpm.monitor.dto.StatsQuery;
import org.jeecg.modules.bpm.monitor.dto.StatsResponse;
import org.jeecg.modules.bpm.monitor.service.MonitorStatsService;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BpmTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class MonitorStatsIT {

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

    @Autowired MonitorStatsService statsService;
    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService runtimeService;
    @Autowired TaskService taskService;
    @Autowired BpmProcessDefinitionMapper definitionMapper;
    @Autowired InstanceMetaMapper instanceMetaMapper;
    @Autowired NodeConfigMapper nodeConfigMapper;
    @Autowired JdbcTemplate jdbc;

    private String actDefId;
    private String defMetaId;

    @BeforeEach
    void setUp() {
        jdbc.execute("DELETE FROM bpm_task_history");
        jdbc.execute("DELETE FROM bpm_node_config");
        jdbc.execute("DELETE FROM bpm_instance_meta");
        jdbc.execute("DELETE FROM bpm_process_definition");

        repositoryService.createDeployment()
                .addClasspathResource("bpm/helloworld.bpmn20.xml")
                .deploy();

        actDefId = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("bpm_helloworld")
                .latestVersion()
                .singleResult()
                .getId();

        BpmProcessDefinition def = new BpmProcessDefinition();
        def.setDefKey("bpm_helloworld");
        def.setName("Hello World");
        def.setVersion(1);
        def.setState("PUBLISHED");
        def.setCategory("DEFAULT");
        def.setTenantId("default");
        def.setActDefId(actDefId);
        definitionMapper.insert(def);
        defMetaId = def.getId();

        when(orgService.findDeptName(1L)).thenReturn("研发部");
    }

    private void startAndCompleteInstance(Long deptId) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("initiator", "99");
        org.flowable.engine.runtime.ProcessInstance pi =
                runtimeService.startProcessInstanceByKey("bpm_helloworld", vars);

        InstanceMeta meta = new InstanceMeta();
        meta.setActInstId(pi.getId());
        meta.setDefId(defMetaId);
        meta.setDefVersion(1);
        meta.setApplyUserId(99L);
        meta.setApplyDeptId(deptId);
        meta.setState("RUNNING");
        meta.setStartTime(LocalDateTime.now().minusHours(2));
        instanceMetaMapper.insert(meta);

        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        taskService.complete(task.getId());

        meta.setState("COMPLETED");
        meta.setEndTime(LocalDateTime.now());
        instanceMetaMapper.updateById(meta);
    }

    @Test
    void byDefinitionShowsInstanceCount() {
        startAndCompleteInstance(1L);
        startAndCompleteInstance(1L);
        startAndCompleteInstance(1L);

        StatsResponse r = statsService.compute(new StatsQuery(), Set.of("byDefinition"));
        assertThat(r.getByDefinition()).hasSize(1);
        assertThat(r.getByDefinition().get(0).getInstanceCount()).isEqualTo(3L);
        assertThat(r.getByDefinition().get(0).getCompletedCount()).isEqualTo(3L);
        assertThat(r.getByDefinition().get(0).getCompletionRate()).isEqualTo(1.0);
    }

    @Test
    void byDefinitionWithNodeConfigShowsResults() {
        NodeConfig nc = new NodeConfig();
        nc.setDefId(defMetaId);
        nc.setNodeId("task_hello");
        nc.setTimeoutHours(1);
        nodeConfigMapper.insert(nc);

        startAndCompleteInstance(1L);

        StatsResponse r = statsService.compute(new StatsQuery(), Set.of("byDefinition"));
        assertThat(r.getByDefinition()).hasSize(1);
        assertThat(r.getByDefinition().get(0).getInstanceCount()).isEqualTo(1L);
        assertThat(r.getByDefinition().get(0).getOverdueCount()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void byApplyDeptGroupsCorrectly() {
        startAndCompleteInstance(1L);
        startAndCompleteInstance(1L);
        startAndCompleteInstance(2L);

        StatsResponse r = statsService.compute(new StatsQuery(), Set.of("byApplyDept"));
        assertThat(r.getByApplyDept()).hasSizeGreaterThanOrEqualTo(2);
        long dept1Count = r.getByApplyDept().stream()
                .filter(row -> Long.valueOf(1L).equals(row.getApplyDeptId()))
                .mapToLong(row -> row.getInstanceCount()).sum();
        assertThat(dept1Count).isEqualTo(2L);
    }
}
