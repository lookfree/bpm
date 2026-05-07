package org.jeecg.modules.bpm.e2e;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.*;
import org.jeecg.modules.bpm.mapper.*;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.jeecg.modules.bpm.service.instance.StartRequest;
import org.jeecg.modules.bpm.service.instance.StartResponse;
import org.jeecg.modules.bpm.service.task.BpmTaskService;
import org.jeecg.modules.bpm.service.task.TaskHistoryWriter;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.jeecg.modules.bpm.spi.dto.BpmFormField;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import org.jeecg.modules.bpm.test.BpmTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { BpmTestApplication.class })
@ActiveProfiles("test")
@Testcontainers
class ApplyApproveFlowIT {

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

    @Autowired RepositoryService repositoryService;
    @Autowired TaskService flowableTaskService;
    @Autowired BpmProcessDefinitionMapper definitionMapper;
    @Autowired NodeConfigMapper nodeConfigMapper;
    @Autowired FormBindingMapper formBindingMapper;
    @Autowired InstanceMetaMapper instanceMetaMapper;
    @Autowired TaskHistoryMapper taskHistoryMapper;
    @Autowired InstanceService instanceService;
    @Autowired BpmTaskService bpmTaskService;

    @Test
    void applyApproveHappyPath() {
        // 1. Deploy BPMN
        repositoryService.createDeployment()
                .addClasspathResource("bpm/test/apply_approve.bpmn20.xml")
                .deploy();

        // 2. Insert process definition row (BpmTaskService reads def by id)
        BpmProcessDefinition def = new BpmProcessDefinition();
        def.setDefKey("apply_approve");
        def.setName("申请-审批");
        def.setVersion(1);
        def.setState("PUBLISHED");
        definitionMapper.insert(def);
        String defRowId = def.getId();

        // 3. Insert node config: approve node → role=FINANCE → user 42
        NodeConfig nodeConfig = new NodeConfig();
        nodeConfig.setDefId(defRowId);
        nodeConfig.setNodeId("approve");
        nodeConfig.setAssigneeStrategy("{\"type\":\"ROLE\",\"payload\":{\"roleCode\":\"FINANCE\"}}");
        nodeConfigMapper.insert(nodeConfig);

        // 4. Insert form bindings
        FormBinding applyBinding = new FormBinding();
        applyBinding.setDefId(defRowId);
        applyBinding.setFormId("F1");
        applyBinding.setPurpose("APPLY");
        formBindingMapper.insert(applyBinding);

        FormBinding approveBinding = new FormBinding();
        approveBinding.setDefId(defRowId);
        approveBinding.setFormId("F1");
        approveBinding.setPurpose("APPROVE");
        formBindingMapper.insert(approveBinding);

        // 5. Mock SPI: applicant=7L, dept=100L; approver=42L via FINANCE role
        when(userContext.currentUserId()).thenReturn(7L);
        when(userContext.currentDeptId()).thenReturn(100L);
        when(formService.saveFormSubmission("F1", Map.of("amount", 1000))).thenReturn("biz_42");
        when(formService.loadFormData("F1", "biz_42")).thenReturn(Map.of("amount", 1000));

        BpmFormField amountField = new BpmFormField();
        amountField.setName("amount");
        amountField.setLabel("金额");
        amountField.setType("NUMBER");
        amountField.setRequired(true);

        BpmFormSchema schema = new BpmFormSchema();
        schema.setFormId("F1");
        schema.setFields(List.of(amountField));
        when(formService.loadFormSchema("F1")).thenReturn(schema);
        when(orgService.findUsersByRole("FINANCE")).thenReturn(List.of(42L));

        // 6. Start instance as user 7L
        StartResponse resp = instanceService.start(StartRequest.of(defRowId, "F1", Map.of("amount", 1000)));
        assertThat(resp.getActInstId()).isNotBlank();

        // 7. As user 7L: complete the apply task
        List<Task> applyTasks = flowableTaskService.createTaskQuery()
                .processInstanceId(resp.getActInstId()).list();
        assertThat(applyTasks).hasSize(1);
        Task applyTask = applyTasks.get(0);
        assertThat(applyTask.getTaskDefinitionKey()).isEqualTo("apply");

        bpmTaskService.complete(applyTask.getId(), "APPROVE", "提交申请", Map.of("amount", 1000));

        // 8. As user 42L: verify todo list has approve task
        when(userContext.currentUserId()).thenReturn(42L);
        List<Map<String, Object>> todos = bpmTaskService.listTodo();
        assertThat(todos).hasSize(1);
        assertThat(todos.get(0).get("taskDefKey")).isEqualTo("approve");

        // 9. Approve
        bpmTaskService.complete((String) todos.get(0).get("taskId"), "APPROVE", "同意", Map.of());

        // 10. Verify: no more active tasks
        long remaining = flowableTaskService.createTaskQuery()
                .processInstanceId(resp.getActInstId()).count();
        assertThat(remaining).isZero();

        // 11. Verify: 2 task history entries for this instance
        List<TaskHistory> hist = taskHistoryMapper.selectList(
                new LambdaQueryWrapper<TaskHistory>()
                        .eq(TaskHistory::getInstId, resp.getInstanceId()));
        assertThat(hist).hasSize(2);
        List<String> nodeIds = new ArrayList<>();
        for (TaskHistory h : hist) nodeIds.add(h.getNodeId());
        assertThat(nodeIds).containsExactlyInAnyOrder("apply", "approve");

        // 12. Verify: instance meta state=COMPLETED
        InstanceMeta meta = instanceMetaMapper.selectById(resp.getInstanceId());
        assertThat(meta.getState()).isEqualTo("COMPLETED");
    }

    @Test
    void taskHistoryIdempotentOnDuplicate() {
        TaskHistoryWriter writer = new TaskHistoryWriter(taskHistoryMapper);

        // Insert first entry
        TaskHistory h = new TaskHistory();
        h.setActTaskId("t_idem");
        h.setInstId("i_idem");
        h.setNodeId("n_idem");
        h.setAssigneeId(1L);
        h.setAction("APPROVE");
        h.setOpTime(java.time.LocalDateTime.now());
        taskHistoryMapper.insert(h);

        // Write duplicate — should not throw
        assertThatCode(() -> writer.write(new TaskHistoryWriter.Entry(
                "t_idem", "i_idem", "n_idem", 1L, "APPROVE", null, null)))
                .doesNotThrowAnyException();
    }
}
