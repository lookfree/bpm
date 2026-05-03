package org.jeecg.modules.bpm.multi;

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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { BpmTestApplication.class })
@ActiveProfiles("test")
@Testcontainers
class SequenceCountersignIT {

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
    @Autowired MultiInstanceXmlRewriter rewriter;

    @Test
    void threeSequentialApprovals() throws IOException {
        String raw = new String(
                getClass().getResourceAsStream("/bpm/it/sequence-countersign.bpmn20.xml").readAllBytes(),
                StandardCharsets.UTF_8);
        String rewritten = rewriter.rewrite(raw, Map.of("t", new MultiModeConfig("SEQUENCE")));
        repositoryService.createDeployment().addString("sc.bpmn20.xml", rewritten).deploy();

        Map<String, Object> v = new HashMap<>();
        v.put("bpm_assignees_t", List.of("u1", "u2", "u3"));
        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_seq_cs", v);

        // Only 1 task at a time (sequential)
        List<Task> tasks1 = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks1).hasSize(1);
        taskService.complete(tasks1.get(0).getId());

        List<Task> tasks2 = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks2).hasSize(1);
        taskService.complete(tasks2.get(0).getId());

        List<Task> tasks3 = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks3).hasSize(1);
        taskService.complete(tasks3.get(0).getId());

        // All 3 done — instance ends
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(inst.getId()).singleResult()).isNull();
    }
}
