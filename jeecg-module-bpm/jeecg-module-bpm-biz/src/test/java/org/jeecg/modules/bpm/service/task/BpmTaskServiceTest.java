package org.jeecg.modules.bpm.service.task;

import org.flowable.engine.HistoryService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.FormBindingMapper;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.service.form.FormPermissionMerger;
import org.jeecg.modules.bpm.service.nodecfg.NodeConfigService;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BpmTaskServiceTest {

    org.flowable.engine.TaskService flowableTs = mock(org.flowable.engine.TaskService.class);
    HistoryService histSvc = mock(HistoryService.class);
    BpmUserContext userCtx = mock(BpmUserContext.class);
    TaskHistoryWriter histWriter = mock(TaskHistoryWriter.class);
    InstanceMetaMapper instMapper = mock(InstanceMetaMapper.class);
    FormBindingMapper fbMapper = mock(FormBindingMapper.class);
    NodeConfigService nodeCfg = mock(NodeConfigService.class);
    FormPermissionMerger merger = mock(FormPermissionMerger.class);
    BpmFormService formSvc = mock(BpmFormService.class);

    BpmTaskService svc = new BpmTaskService(
            flowableTs, histSvc, userCtx, histWriter, instMapper, fbMapper, nodeCfg, merger, formSvc);

    @Test void completeApproveCallsFlowableAndWritesHistory() {
        when(userCtx.currentUserId()).thenReturn(5L);
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("pi1");
        when(task.getTaskDefinitionKey()).thenReturn("node1");
        TaskQuery tq = mock(TaskQuery.class);
        when(flowableTs.createTaskQuery()).thenReturn(tq);
        when(tq.taskId("t1")).thenReturn(tq);
        when(tq.singleResult()).thenReturn(task);
        when(instMapper.selectOne(any())).thenReturn(null);

        svc.complete("t1", "APPROVE", "looks good", Map.of("field1", "val1"));

        verify(flowableTs).complete(eq("t1"), argThat(m -> !m.containsKey("_rejected")));
        verify(histWriter).write(argThat(e -> "APPROVE".equals(e.getAction())));
    }

    @Test void completeUnsupportedActionThrows() {
        assertThatThrownBy(() -> svc.complete("t1", "TRANSFER", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported_action_in_p2");
        verifyNoInteractions(flowableTs);
    }
}
