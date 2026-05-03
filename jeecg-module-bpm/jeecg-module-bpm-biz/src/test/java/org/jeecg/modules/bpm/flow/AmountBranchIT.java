package org.jeecg.modules.bpm.flow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = { BpmTestApplication.class })
@ActiveProfiles("test")
@Testcontainers
class AmountBranchIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void p(DynamicPropertyRegistry r) {
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
    @Autowired RuntimeService    runtimeService;
    @Autowired TaskService       taskService;

    @Test
    void highAmountGoesToHighApproval() {
        when(userContext.currentUserId()).thenReturn(1L);
        when(userContext.currentDeptId()).thenReturn(1L);
        when(userContext.currentRoleCodes()).thenReturn(Collections.emptySet());
        when(formService.loadFormData(null, null)).thenReturn(Collections.emptyMap());

        repositoryService.createDeployment()
                .addClasspathResource("bpm/it/amount-branch.bpmn20.xml")
                .deploy();

        Map<String, Object> v = new HashMap<>();
        v.put("initiator", "alice");
        v.put("form.amount", 20000);
        v.put("bpm_def_key", "bpm_amount_branch");
        v.put("bpm_def_version", 1);

        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_amount_branch", v);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("High Approval");
    }

    @Test
    void normalAmountGoesToDefaultBranch() {
        when(userContext.currentUserId()).thenReturn(1L);
        when(userContext.currentDeptId()).thenReturn(1L);
        when(userContext.currentRoleCodes()).thenReturn(Collections.emptySet());
        when(formService.loadFormData(null, null)).thenReturn(Collections.emptyMap());

        repositoryService.createDeployment()
                .addClasspathResource("bpm/it/amount-branch.bpmn20.xml")
                .deploy();

        Map<String, Object> v = new HashMap<>();
        v.put("initiator", "alice");
        v.put("form.amount", 5000);
        v.put("bpm_def_key", "bpm_amount_branch");
        v.put("bpm_def_version", 1);

        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_amount_branch", v);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("Normal Approval");
    }
}
