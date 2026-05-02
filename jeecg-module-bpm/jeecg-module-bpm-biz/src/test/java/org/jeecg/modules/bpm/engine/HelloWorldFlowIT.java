package org.jeecg.modules.bpm.engine;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { org.jeecg.modules.bpm.test.BpmTestApplication.class })
@ActiveProfiles("test")
@Testcontainers
class HelloWorldFlowIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService runtimeService;
    @Autowired TaskService taskService;

    @Test
    void deployStartCompleteHelloWorld() {
        repositoryService.createDeployment()
                .addClasspathResource("bpm/helloworld.bpmn20.xml")
                .deploy();

        Map<String,Object> vars = new HashMap<>();
        vars.put("initiator", "alice");
        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_helloworld", vars);
        assertThat(inst).isNotNull();
        assertThat(inst.isEnded()).isFalse();

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(1);
        Task t = tasks.get(0);
        assertThat(t.getName()).isEqualTo("Say Hello");
        assertThat(t.getAssignee()).isEqualTo("alice");

        taskService.complete(t.getId());

        ProcessInstance ended = runtimeService.createProcessInstanceQuery()
                .processInstanceId(inst.getId()).singleResult();
        assertThat(ended).isNull();
    }
}
