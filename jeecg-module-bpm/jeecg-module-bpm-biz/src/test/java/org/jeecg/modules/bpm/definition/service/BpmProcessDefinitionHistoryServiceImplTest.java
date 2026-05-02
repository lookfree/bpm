package org.jeecg.modules.bpm.definition.service;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionHistoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmProcessDefinitionHistoryServiceImplTest {

    @Test
    void snapshotPersistsAllFields() throws Exception {
        BpmProcessDefinitionHistoryMapper mapper = mock(BpmProcessDefinitionHistoryMapper.class);
        when(mapper.insert(any(BpmProcessDefinitionHistory.class))).thenReturn(1);
        BpmProcessDefinitionHistoryServiceImpl svc = new BpmProcessDefinitionHistoryServiceImpl();
        java.lang.reflect.Field f = svc.getClass().getSuperclass().getDeclaredField("baseMapper");
        f.setAccessible(true); f.set(svc, mapper);

        String id = svc.snapshot("d1", "key1", 3, "<xml/>", "fix typo", "alice");
        ArgumentCaptor<BpmProcessDefinitionHistory> cap =
                ArgumentCaptor.forClass(BpmProcessDefinitionHistory.class);
        verify(mapper).insert(cap.capture());
        BpmProcessDefinitionHistory saved = cap.getValue();
        assertThat(saved.getDefId()).isEqualTo("d1");
        assertThat(saved.getDefKey()).isEqualTo("key1");
        assertThat(saved.getVersion()).isEqualTo(3);
        assertThat(saved.getBpmnXml()).isEqualTo("<xml/>");
        assertThat(saved.getChangeNote()).isEqualTo("fix typo");
        assertThat(saved.getPublishedBy()).isEqualTo("alice");
        assertThat(saved.getPublishedTime()).isNotNull();
    }
}
