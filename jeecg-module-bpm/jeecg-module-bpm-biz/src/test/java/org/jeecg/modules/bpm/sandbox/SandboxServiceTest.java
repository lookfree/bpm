package org.jeecg.modules.bpm.sandbox;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SandboxServiceTest {

    BpmProcessDefinitionMapper defMapper;
    SandboxRunService runService;
    InstanceMetaMapper instanceMetaMapper;
    RuntimeService runtimeService;
    SandboxService svc;

    @BeforeEach
    void setUp() {
        defMapper = mock(BpmProcessDefinitionMapper.class);
        runService = mock(SandboxRunService.class);
        instanceMetaMapper = mock(InstanceMetaMapper.class);
        runtimeService = mock(RuntimeService.class);
        svc = new SandboxService(defMapper, runService, instanceMetaMapper, runtimeService);
    }

    @Test
    void start_nonSandboxDef_throws() {
        BpmProcessDefinition def = def("id1", "PROD");
        when(defMapper.selectById("id1")).thenReturn(def);
        assertThatThrownBy(() -> svc.start("id1", Collections.emptyMap(), 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a SANDBOX");
    }

    @Test
    void start_sandboxDef_startsProcessAndCreatesInstanceMeta() {
        BpmProcessDefinition def = def("id2", "SANDBOX");
        def.setDefKey("myKey");
        when(defMapper.selectById("id2")).thenReturn(def);
        when(runService.start("id2", 1L)).thenReturn(10L);

        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn("pi-1");
        when(runtimeService.startProcessInstanceByKey(eq("myKey"), anyMap())).thenReturn(pi);
        when(instanceMetaMapper.insert(any())).thenReturn(1);

        Long runId = svc.start("id2", Collections.emptyMap(), 1L);
        assertThat(runId).isEqualTo(10L);
        verify(instanceMetaMapper).insert(argThat(m -> "SANDBOX".equals(m.getState())));
    }

    @Test
    void start_processThrows_finishesWithFail() {
        BpmProcessDefinition def = def("id3", "SANDBOX");
        def.setDefKey("badKey");
        when(defMapper.selectById("id3")).thenReturn(def);
        when(runService.start("id3", 1L)).thenReturn(20L);
        when(runtimeService.startProcessInstanceByKey(anyString(), anyMap()))
            .thenThrow(new RuntimeException("engine error"));
        doNothing().when(runService).finish(any(), any());
        doNothing().when(runService).appendLog(any(), any());

        Long runId = svc.start("id3", Collections.emptyMap(), 1L);
        assertThat(runId).isEqualTo(20L);
        verify(runService).finish(20L, SandboxResult.FAIL);
    }

    private BpmProcessDefinition def(String id, String category) {
        BpmProcessDefinition d = new BpmProcessDefinition();
        d.setId(id);
        d.setCategory(category);
        d.setState("DRAFT");
        d.setVersion(1);
        return d;
    }
}
