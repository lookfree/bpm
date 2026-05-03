package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefinitionArchiveServiceTest {

    BpmProcessDefinitionMapper definitionMapper;
    DefinitionLifecycleService lifecycle;
    DefinitionArchiveService svc;

    @BeforeEach void setUp() {
        definitionMapper = mock(BpmProcessDefinitionMapper.class);
        lifecycle = new DefinitionLifecycleService();
        svc = new DefinitionArchiveService(definitionMapper, lifecycle);
    }

    @Test void archive_publishedDef_setsArchived() {
        BpmProcessDefinition def = def("id1", "PUBLISHED");
        when(definitionMapper.selectById("id1")).thenReturn(def);
        when(definitionMapper.updateById(any())).thenReturn(1);

        svc.archive("id1", "admin");

        verify(definitionMapper).updateById(argThat(d -> "ARCHIVED".equals(d.getState())));
    }

    @Test void archive_draftDef_throws() {
        BpmProcessDefinition def = def("id2", "DRAFT");
        when(definitionMapper.selectById("id2")).thenReturn(def);

        assertThatThrownBy(() -> svc.archive("id2", "admin"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test void archive_archivedDef_throws() {
        BpmProcessDefinition def = def("id3", "ARCHIVED");
        when(definitionMapper.selectById("id3")).thenReturn(def);

        assertThatThrownBy(() -> svc.archive("id3", "admin"))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    private BpmProcessDefinition def(String id, String state) {
        BpmProcessDefinition d = new BpmProcessDefinition();
        d.setId(id);
        d.setState(state);
        return d;
    }
}
