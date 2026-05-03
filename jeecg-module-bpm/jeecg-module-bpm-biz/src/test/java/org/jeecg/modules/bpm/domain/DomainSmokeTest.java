package org.jeecg.modules.bpm.domain;

import org.jeecg.modules.bpm.domain.entity.*;
import org.jeecg.modules.bpm.mapper.*;
import org.jeecg.modules.bpm.test.BpmTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = BpmTestApplication.class)
@ActiveProfiles("test")
@Testcontainers
class DomainSmokeTest {

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

    @Autowired NodeConfigMapper nodeConfigMapper;
    @Autowired AssigneeStrategyDefMapper assigneeStrategyDefMapper;
    @Autowired FormBindingMapper formBindingMapper;
    @Autowired InstanceMetaMapper instanceMetaMapper;
    @Autowired TaskHistoryMapper taskHistoryMapper;

    @Test
    void allMappersWiredAndCanInsertAndDelete() {
        // NodeConfig
        NodeConfig nc = new NodeConfig();
        nc.setDefId("def1"); nc.setNodeId("node1");
        nodeConfigMapper.insert(nc);
        assertThat(nodeConfigMapper.selectById(nc.getId())).isNotNull();
        nodeConfigMapper.deleteById(nc.getId());

        // AssigneeStrategyDef
        AssigneeStrategyDef sd = new AssigneeStrategyDef();
        sd.setName("test"); sd.setType("USER");
        assigneeStrategyDefMapper.insert(sd);
        assertThat(assigneeStrategyDefMapper.selectById(sd.getId())).isNotNull();
        assigneeStrategyDefMapper.deleteById(sd.getId());

        // FormBinding
        FormBinding fb = new FormBinding();
        fb.setDefId("def1"); fb.setFormId("form1"); fb.setPurpose("APPLY");
        formBindingMapper.insert(fb);
        assertThat(formBindingMapper.selectById(fb.getId())).isNotNull();
        formBindingMapper.deleteById(fb.getId());

        // InstanceMeta
        InstanceMeta im = new InstanceMeta();
        im.setActInstId("act1"); im.setDefId("def1"); im.setDefVersion(1); im.setState("RUNNING");
        instanceMetaMapper.insert(im);
        assertThat(instanceMetaMapper.selectById(im.getId())).isNotNull();
        instanceMetaMapper.deleteById(im.getId());

        // TaskHistory
        TaskHistory th = new TaskHistory();
        th.setActTaskId("task1"); th.setInstId("inst1"); th.setNodeId("node1"); th.setAction("APPROVE");
        taskHistoryMapper.insert(th);
        assertThat(taskHistoryMapper.selectById(th.getId())).isNotNull();
        taskHistoryMapper.deleteById(th.getId());
    }
}
