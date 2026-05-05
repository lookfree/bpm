package org.jeecg.modules.bpm.monitor.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.monitor.dto.InterveneRequest;
import org.jeecg.modules.bpm.service.instance.InstanceService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class InstanceInterventionServiceTest {

    private InstanceInterventionService build(InstanceService inst, TaskService ts, RuntimeService rt) {
        return new InstanceInterventionService(inst, ts, rt);
    }

    private InstanceMeta runningMeta(String id, String actInstId) {
        InstanceMeta m = new InstanceMeta();
        m.setId(id);
        m.setActInstId(actInstId);
        m.setState("RUNNING");
        return m;
    }

    @Test
    void forceCompleteTaskCompletesAllActiveTasks() {
        InstanceService inst = mock(InstanceService.class);
        TaskService ts = mock(TaskService.class);
        RuntimeService rt = mock(RuntimeService.class);

        when(inst.findMeta("m1")).thenReturn(runningMeta("m1", "proc1"));

        Task t1 = mock(Task.class);
        when(t1.getId()).thenReturn("task1");
        Task t2 = mock(Task.class);
        when(t2.getId()).thenReturn("task2");

        TaskQuery q = mock(TaskQuery.class);
        when(ts.createTaskQuery()).thenReturn(q);
        when(q.processInstanceId("proc1")).thenReturn(q);
        when(q.list()).thenReturn(List.of(t1, t2));

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_COMPLETE_TASK");

        build(inst, ts, rt).intervene("m1", req);

        verify(ts).complete("task1");
        verify(ts).complete("task2");
    }

    @Test
    void forceCancelDeletesProcessAndMarksCancelled() {
        InstanceService inst = mock(InstanceService.class);
        TaskService ts = mock(TaskService.class);
        RuntimeService rt = mock(RuntimeService.class);

        when(inst.findMeta("m2")).thenReturn(runningMeta("m2", "proc2"));

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_CANCEL");
        req.setComment("admin cancel");

        build(inst, ts, rt).intervene("m2", req);

        verify(rt).deleteProcessInstance("proc2", "admin cancel");
        verify(inst).markCancelled("m2");
    }

    @Test
    void forceReassignSetsAssigneeOnAllActiveTasks() {
        InstanceService inst = mock(InstanceService.class);
        TaskService ts = mock(TaskService.class);
        RuntimeService rt = mock(RuntimeService.class);

        when(inst.findMeta("m3")).thenReturn(runningMeta("m3", "proc3"));

        Task t = mock(Task.class);
        when(t.getId()).thenReturn("task3");
        TaskQuery q = mock(TaskQuery.class);
        when(ts.createTaskQuery()).thenReturn(q);
        when(q.processInstanceId("proc3")).thenReturn(q);
        when(q.list()).thenReturn(List.of(t));

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_REASSIGN");
        req.setTargetUserId("user99");

        build(inst, ts, rt).intervene("m3", req);

        verify(ts).setAssignee("task3", "user99");
    }

    @Test
    void throwsWhenInstanceNotFound() {
        InstanceService inst = mock(InstanceService.class);
        when(inst.findMeta("bad")).thenReturn(null);

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_CANCEL");

        assertThatThrownBy(() -> build(inst, mock(TaskService.class), mock(RuntimeService.class))
                .intervene("bad", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void throwsWhenNotRunning() {
        InstanceService inst = mock(InstanceService.class);
        InstanceMeta meta = new InstanceMeta();
        meta.setId("m4");
        meta.setState("COMPLETED");
        when(inst.findMeta("m4")).thenReturn(meta);

        InterveneRequest req = new InterveneRequest();
        req.setAction("FORCE_COMPLETE_TASK");

        assertThatThrownBy(() -> build(inst, mock(TaskService.class), mock(RuntimeService.class))
                .intervene("m4", req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void throwsOnUnknownAction() {
        InstanceService inst = mock(InstanceService.class);
        when(inst.findMeta("m5")).thenReturn(runningMeta("m5", "proc5"));

        InterveneRequest req = new InterveneRequest();
        req.setAction("UNKNOWN");

        assertThatThrownBy(() -> build(inst, mock(TaskService.class), mock(RuntimeService.class))
                .intervene("m5", req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
