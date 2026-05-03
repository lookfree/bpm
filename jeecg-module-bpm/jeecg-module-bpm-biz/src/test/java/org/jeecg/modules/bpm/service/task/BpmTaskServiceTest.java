package org.jeecg.modules.bpm.service.task;

import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
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

    private Task setupTaskMock(String taskId, String procInstId, String nodeId) {
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn(procInstId);
        when(task.getTaskDefinitionKey()).thenReturn(nodeId);
        TaskQuery tq = mock(TaskQuery.class);
        when(flowableTs.createTaskQuery()).thenReturn(tq);
        when(tq.taskId(taskId)).thenReturn(tq);
        when(tq.singleResult()).thenReturn(task);
        when(instMapper.selectOne(any())).thenReturn(null);
        return task;
    }

    @Test void completeApproveCallsFlowableAndWritesHistory() {
        when(userCtx.currentUserId()).thenReturn(5L);
        setupTaskMock("t1", "pi1", "node1");
        HistoricProcessInstanceQuery hpiq = mock(HistoricProcessInstanceQuery.class);
        when(histSvc.createHistoricProcessInstanceQuery()).thenReturn(hpiq);
        when(hpiq.processInstanceId(any())).thenReturn(hpiq);
        when(hpiq.finished()).thenReturn(hpiq);
        when(hpiq.count()).thenReturn(0L);

        svc.complete("t1", "APPROVE", "looks good", Map.of("field1", "val1"));

        verify(flowableTs).complete(eq("t1"), argThat(m -> !m.containsKey("_rejected")));
        verify(histWriter).write(argThat(e -> "APPROVE".equals(e.getAction())));
    }

    @Test void completeUnsupportedActionThrows() {
        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("pi1");
        when(task.getTaskDefinitionKey()).thenReturn("node1");
        TaskQuery tq = mock(TaskQuery.class);
        when(flowableTs.createTaskQuery()).thenReturn(tq);
        when(tq.taskId("t1")).thenReturn(tq);
        when(tq.singleResult()).thenReturn(task);

        assertThatThrownBy(() -> svc.complete("t1", "UNKNOWN", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported_action");
        verify(flowableTs, never()).complete(any(), any());
    }

    @Test void transferReassignsTaskWithoutAdvancing() {
        when(userCtx.currentUserId()).thenReturn(5L);
        setupTaskMock("t1", "pi1", "node1");

        svc.complete("t1", "TRANSFER", "please handle", "u_b", null);

        verify(flowableTs).setAssignee("t1", "u_b");
        verify(flowableTs, never()).complete(any(), any());
        verify(histWriter).write(argThat(e -> "TRANSFER".equals(e.getAction())));
    }

    @Test void countersignCreatesSubTask() {
        when(userCtx.currentUserId()).thenReturn(5L);
        Task parentTask = setupTaskMock("t1", "pi1", "node1");
        when(parentTask.getName()).thenReturn("审批");

        Task subTask = mock(Task.class);
        when(flowableTs.newTask()).thenReturn(subTask);

        svc.complete("t1", "COUNTERSIGN", "add signer", "u_c", null);

        verify(subTask).setName("审批（加签）");
        verify(subTask).setAssignee("u_c");
        verify(subTask).setParentTaskId("t1");
        verify(subTask).setOwner("5");
        verify(flowableTs).saveTask(subTask);
        verify(flowableTs, never()).complete(any(), any());
        verify(histWriter).write(argThat(e -> "COUNTERSIGN".equals(e.getAction())));
    }
}
