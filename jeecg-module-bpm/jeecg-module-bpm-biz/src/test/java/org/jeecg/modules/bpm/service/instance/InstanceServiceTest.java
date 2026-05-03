package org.jeecg.modules.bpm.service.instance;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InstanceServiceTest {

    RuntimeService runtime = mock(RuntimeService.class);
    BpmFormService form = mock(BpmFormService.class);
    BpmUserContext userCtx = mock(BpmUserContext.class);
    InstanceMetaMapper instMapper = mock(InstanceMetaMapper.class);
    BpmProcessDefinitionMapper defMapper = mock(BpmProcessDefinitionMapper.class);

    InstanceService svc = new InstanceService(runtime, form, userCtx, instMapper, defMapper);

    @Test
    void startSavesFormThenStartsProcessThenWritesMeta() {
        when(userCtx.currentUserId()).thenReturn(7L);
        when(userCtx.currentDeptId()).thenReturn(100L);
        when(form.saveFormSubmission("F1", Map.of("amount", 1000))).thenReturn("biz_42");

        BpmProcessDefinition def = new BpmProcessDefinition();
        def.setId("def1");
        def.setDefKey("apply_approve");
        def.setVersion(1);
        when(defMapper.selectById("def1")).thenReturn(def);

        ProcessInstance pi = mock(ProcessInstance.class);
        when(pi.getId()).thenReturn("act_inst_99");
        when(runtime.startProcessInstanceByKey(eq("apply_approve"), eq("biz_42"), anyMap()))
                .thenReturn(pi);

        StartResponse r = svc.start(StartRequest.of("def1", "F1", Map.of("amount", 1000)));

        assertThat(r.getActInstId()).isEqualTo("act_inst_99");
        assertThat(r.getBusinessKey()).isEqualTo("biz_42");
        verify(instMapper).insert(argThat(m ->
                m.getActInstId().equals("act_inst_99") &&
                m.getDefId().equals("def1") &&
                m.getDefVersion() == 1 &&
                m.getBusinessKey().equals("biz_42") &&
                m.getApplyUserId().equals(7L) &&
                m.getApplyDeptId().equals(100L) &&
                "RUNNING".equals(m.getState())));
    }

    @Test
    void startThrowsWhenDefinitionNotFound() {
        when(defMapper.selectById("missing")).thenReturn(null);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                svc.start(StartRequest.of("missing", "F1", Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
