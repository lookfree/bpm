package org.jeecg.modules.bpm.monitor.it;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.monitor.dto.InterveneRequest;
import org.jeecg.modules.bpm.monitor.service.InstanceInterventionService;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BpmTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class InstanceInterventionIT {

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

    @Autowired InstanceInterventionService interventionService;
    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService runtimeService;
    @Autowired TaskService taskService;
    @Autowired BpmProcessDefinitionMapper definitionMapper;
    @Autowired InstanceMetaMapper instanceMetaMapper;
    @Autowired JdbcTemplate jdbc;

    private String actDefId;
    private String defMetaId;

    @BeforeEach
    void setUp() {
        jdbc.execute("DELETE FROM bpm_task_history");
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
    }

    private InstanceMeta startInstance() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("initiator", "99");
        org.flowable.engine.runtime.ProcessInstance pi =
                runtimeService.startProcessInstanceByKey("bpm_helloworld", vars);

        InstanceMeta meta = new InstanceMeta();
        meta.setActInstId(pi.getId());
        meta.setDefId(defMetaId);
        meta.setDefVersion(1);
        meta.setApplyUserId(99L);
        meta.setState("RUNNING");
        meta.setStartTime(LocalDateTime.now());
        instanceMetaMapper.insert(meta);
        return meta;
    }

    @Test
    void forceCompleteTaskAdvancesProcess() {
        InstanceMeta meta = startInstance();

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_COMPLETE_TASK");

        interventionService.intervene(meta.getId(), req);

        long activeTasks = taskService.createTaskQuery()
                .processInstanceId(meta.getActInstId())
                .count();
        assertThat(activeTasks).isEqualTo(0L);
    }

    @Test
    void forceCancelTerminatesProcessAndMarksMeta() {
        InstanceMeta meta = startInstance();

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_CANCEL");
        req.setComment("test cancel");

        interventionService.intervene(meta.getId(), req);

        InstanceMeta updated = instanceMetaMapper.selectById(meta.getId());
        assertThat(updated.getState()).isEqualTo("CANCELLED");

        long runningInstances = runtimeService.createProcessInstanceQuery()
                .processInstanceId(meta.getActInstId())
                .count();
        assertThat(runningInstances).isEqualTo(0L);
    }

    @Test
    void forceReassignChangesTaskAssignee() {
        InstanceMeta meta = startInstance();

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_REASSIGN");
        req.setTargetUserId("user42");

        interventionService.intervene(meta.getId(), req);

        Task task = taskService.createTaskQuery()
                .processInstanceId(meta.getActInstId())
                .singleResult();
        assertThat(task.getAssignee()).isEqualTo("user42");
    }
}
