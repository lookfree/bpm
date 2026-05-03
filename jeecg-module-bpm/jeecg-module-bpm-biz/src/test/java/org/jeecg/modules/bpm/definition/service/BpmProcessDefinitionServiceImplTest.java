package org.jeecg.modules.bpm.definition.service;

import org.jeecg.modules.bpm.definition.DefinitionLifecycleService;
import org.jeecg.modules.bpm.definition.dto.DefinitionCreateRequest;
import org.jeecg.modules.bpm.definition.dto.DefinitionUpdateRequest;
import org.jeecg.modules.bpm.definition.dto.DefinitionVO;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionHistoryMapper;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
import org.jeecg.modules.bpm.mapper.NodeConfigMapper;
import org.jeecg.modules.bpm.multi.MultiInstanceXmlRewriter;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BpmProcessDefinitionServiceImplTest {

    BpmProcessDefinitionMapper mapper;
    BpmUserContext userContext;
    BpmProcessDefinitionHistoryService historyService;
    BpmnXmlValidator bpmnValidator;
    RepositoryService repositoryService;
    MultiInstanceXmlRewriter multiInstanceXmlRewriter;
    NodeConfigMapper nodeConfigMapper;
    BpmProcessDefinitionServiceImpl svc;

    @BeforeEach
    void setUp() {
        mapper = mock(BpmProcessDefinitionMapper.class);
        userContext = mock(BpmUserContext.class);
        when(userContext.currentUsername()).thenReturn("alice");
        historyService = mock(BpmProcessDefinitionHistoryService.class);
        bpmnValidator = mock(BpmnXmlValidator.class);
        repositoryService = mock(RepositoryService.class);
        multiInstanceXmlRewriter = mock(MultiInstanceXmlRewriter.class);
        nodeConfigMapper = mock(NodeConfigMapper.class);
        when(nodeConfigMapper.selectList(any())).thenReturn(Collections.emptyList());
        svc = new BpmProcessDefinitionServiceImpl(userContext, historyService, bpmnValidator,
                repositoryService, multiInstanceXmlRewriter, nodeConfigMapper,
                new DefinitionLifecycleService(), mock(BpmProcessDefinitionHistoryMapper.class));
        try {
            java.lang.reflect.Field f = svc.getClass().getSuperclass().getDeclaredField("baseMapper");
            f.setAccessible(true);
            f.set(svc, mapper);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void createDraftDefaultsCategoryAndState() {
        when(mapper.insert(any(BpmProcessDefinition.class))).thenReturn(1);
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k1"); req.setName("N1");
        DefinitionVO vo = svc.createDraft(req);
        assertThat(vo.getState()).isEqualTo("DRAFT");
        assertThat(vo.getVersion()).isEqualTo(1);
        assertThat(vo.getCategory()).isEqualTo("DEFAULT");
        assertThat(vo.getCreateBy()).isEqualTo("alice");
        ArgumentCaptor<BpmProcessDefinition> cap = ArgumentCaptor.forClass(BpmProcessDefinition.class);
        verify(mapper).insert(cap.capture());
        assertThat(cap.getValue().getTenantId()).isEqualTo("default");
    }

    @Test
    void deleteRefusesPublished() {
        BpmProcessDefinition e = new BpmProcessDefinition();
        e.setId("x"); e.setState("PUBLISHED");
        when(mapper.selectById("x")).thenReturn(e);
        assertThatThrownBy(() -> svc.delete("x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void updateRefusesArchived() {
        BpmProcessDefinition e = new BpmProcessDefinition();
        e.setId("x"); e.setState("ARCHIVED");
        when(mapper.selectById("x")).thenReturn(e);
        assertThatThrownBy(() -> svc.update("x", new DefinitionUpdateRequest()))
                .isInstanceOf(IllegalStateException.class);
    }
}
