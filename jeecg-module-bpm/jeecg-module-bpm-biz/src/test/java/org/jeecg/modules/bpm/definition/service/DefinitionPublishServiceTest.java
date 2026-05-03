package org.jeecg.modules.bpm.definition.service;

import org.jeecg.modules.bpm.definition.DefinitionLifecycleService;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionHistoryMapper;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
import org.jeecg.modules.bpm.mapper.NodeConfigMapper;
import org.jeecg.modules.bpm.multi.MultiInstanceXmlRewriter;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefinitionPublishServiceTest {

    BpmProcessDefinitionMapper mapper;
    BpmProcessDefinitionHistoryMapper historyMapper;
    BpmUserContext userContext;
    BpmProcessDefinitionHistoryService historyService;
    BpmnXmlValidator bpmnValidator;
    RepositoryService repositoryService;
    MultiInstanceXmlRewriter multiInstanceXmlRewriter;
    NodeConfigMapper nodeConfigMapper;
    DefinitionLifecycleService lifecycle;
    BpmProcessDefinitionServiceImpl svc;

    @BeforeEach
    void setUp() {
        mapper = mock(BpmProcessDefinitionMapper.class);
        historyMapper = mock(BpmProcessDefinitionHistoryMapper.class);
        userContext = mock(BpmUserContext.class);
        when(userContext.currentUsername()).thenReturn("alice");
        historyService = mock(BpmProcessDefinitionHistoryService.class);
        bpmnValidator = mock(BpmnXmlValidator.class);
        repositoryService = mock(RepositoryService.class);
        multiInstanceXmlRewriter = mock(MultiInstanceXmlRewriter.class);
        nodeConfigMapper = mock(NodeConfigMapper.class);
        when(nodeConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
        lifecycle = new DefinitionLifecycleService();
        svc = new BpmProcessDefinitionServiceImpl(userContext, historyService, bpmnValidator,
                repositoryService, multiInstanceXmlRewriter, nodeConfigMapper, lifecycle, historyMapper);
        try {
            java.lang.reflect.Field f = svc.getClass().getSuperclass().getDeclaredField("baseMapper");
            f.setAccessible(true);
            f.set(svc, mapper);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void publishDraft_movesToTesting_and_writesHistory() {
        BpmProcessDefinition def = defWith("id1", "DRAFT", "<bpmn/>");
        when(mapper.selectById("id1")).thenReturn(def);
        when(historyMapper.selectMaxVersion("id1")).thenReturn(null);
        when(mapper.updateById(any())).thenReturn(1);

        org.jeecg.modules.bpm.definition.dto.DefinitionVO vo = svc.publish("id1", "initial");

        assertThat(vo.getState()).isEqualTo("TESTING");
        verify(historyService).snapshot("id1", "myKey", 1, "<bpmn/>", "initial", "alice");
    }

    @Test
    void publishTesting_movesToPublished_deploysToFlowable() {
        BpmProcessDefinition def = defWith("id2", "TESTING", "<bpmn/>");
        when(mapper.selectById("id2")).thenReturn(def);
        when(mapper.updateById(any())).thenReturn(1);

        org.flowable.engine.repository.Deployment deployment = mock(org.flowable.engine.repository.Deployment.class);
        when(deployment.getId()).thenReturn("dep1");
        when(multiInstanceXmlRewriter.rewrite(any(), any())).thenReturn("<bpmn/>");
        // Use lenient mocking for the deployment chain
        org.flowable.engine.repository.DeploymentBuilder deployBuilder = mock(org.flowable.engine.repository.DeploymentBuilder.class, RETURNS_SELF);
        when(repositoryService.createDeployment()).thenReturn(deployBuilder);
        when(deployBuilder.deploy()).thenReturn(deployment);
        org.flowable.engine.repository.ProcessDefinitionQuery pdQuery = mock(org.flowable.engine.repository.ProcessDefinitionQuery.class, RETURNS_SELF);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(pdQuery);
        doReturn(null).when(pdQuery).singleResult();

        org.jeecg.modules.bpm.definition.dto.DefinitionVO vo = svc.publish("id2", null);
        assertThat(vo.getState()).isEqualTo("PUBLISHED");
        verify(historyService, never()).snapshot(any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void publishArchived_throwsIllegalStateTransition() {
        BpmProcessDefinition def = defWith("id3", "ARCHIVED", "<bpmn/>");
        when(mapper.selectById("id3")).thenReturn(def);
        assertThatThrownBy(() -> svc.publish("id3", null))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void publishWithEmptyBpmn_throwsIllegalState() {
        BpmProcessDefinition def = defWith("id4", "DRAFT", "");
        when(mapper.selectById("id4")).thenReturn(def);
        assertThatThrownBy(() -> svc.publish("id4", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bpmn_xml");
    }

    private BpmProcessDefinition defWith(String id, String state, String bpmn) {
        BpmProcessDefinition d = new BpmProcessDefinition();
        d.setId(id); d.setState(state); d.setBpmnXml(bpmn);
        d.setDefKey("myKey"); d.setName("MyName"); d.setVersion(1);
        return d;
    }
}
