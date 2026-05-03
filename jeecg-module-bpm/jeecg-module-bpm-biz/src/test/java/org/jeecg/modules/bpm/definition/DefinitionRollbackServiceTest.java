package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefinitionRollbackServiceTest {

    BpmProcessDefinitionMapper defMapper;
    BpmProcessDefinitionHistoryService historyService;
    DefinitionRollbackService svc;

    @BeforeEach void setUp() {
        defMapper = mock(BpmProcessDefinitionMapper.class);
        historyService = mock(BpmProcessDefinitionHistoryService.class);
        svc = new DefinitionRollbackService(defMapper, historyService);
    }

    @Test void rollback_toExistingVersion_setsDraft() {
        BpmProcessDefinition def = def("id1", "PUBLISHED", "<v2/>");
        when(defMapper.selectById("id1")).thenReturn(def);
        BpmProcessDefinitionHistory snap = snap("id1", 1, "<v1/>");
        when(historyService.getByVersion("id1", 1)).thenReturn(snap);
        when(defMapper.updateById(any())).thenReturn(1);

        svc.rollback("id1", 1, "alice");

        verify(defMapper).updateById(argThat(d -> "DRAFT".equals(d.getState()) && "<v1/>".equals(d.getBpmnXml())));
    }

    @Test void rollback_archivedDef_throws() {
        BpmProcessDefinition def = def("id2", "ARCHIVED", "<xml/>");
        when(defMapper.selectById("id2")).thenReturn(def);
        assertThatThrownBy(() -> svc.rollback("id2", 1, "alice"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test void rollback_missingVersion_throws() {
        BpmProcessDefinition def = def("id3", "PUBLISHED", "<xml/>");
        when(defMapper.selectById("id3")).thenReturn(def);
        when(historyService.getByVersion("id3", 99)).thenReturn(null);
        assertThatThrownBy(() -> svc.rollback("id3", 99, "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version not found");
    }

    @Test void rollback_doesNotWriteHistory() {
        BpmProcessDefinition def = def("id4", "TESTING", "<xml/>");
        when(defMapper.selectById("id4")).thenReturn(def);
        when(historyService.getByVersion("id4", 1)).thenReturn(snap("id4", 1, "<v1/>"));
        when(defMapper.updateById(any())).thenReturn(1);

        svc.rollback("id4", 1, "alice");

        verify(historyService, never()).snapshot(any(), any(), anyInt(), any(), any(), any());
    }

    @Test void rollback_publishedDef_setsDraftDirectly() {
        // rollback bypasses transition matrix, even from PUBLISHED
        BpmProcessDefinition def = def("id5", "PUBLISHED", "<v2/>");
        when(defMapper.selectById("id5")).thenReturn(def);
        when(historyService.getByVersion("id5", 1)).thenReturn(snap("id5", 1, "<v1/>"));
        when(defMapper.updateById(any())).thenReturn(1);

        assertThatCode(() -> svc.rollback("id5", 1, "alice")).doesNotThrowAnyException();
        verify(defMapper).updateById(argThat(d -> "DRAFT".equals(d.getState())));
    }

    private BpmProcessDefinition def(String id, String state, String bpmn) {
        BpmProcessDefinition d = new BpmProcessDefinition();
        d.setId(id); d.setState(state); d.setBpmnXml(bpmn); d.setVersion(2);
        return d;
    }

    private BpmProcessDefinitionHistory snap(String defId, int version, String bpmn) {
        BpmProcessDefinitionHistory h = new BpmProcessDefinitionHistory();
        h.setDefId(defId); h.setVersion(version); h.setBpmnXml(bpmn);
        return h;
    }
}
