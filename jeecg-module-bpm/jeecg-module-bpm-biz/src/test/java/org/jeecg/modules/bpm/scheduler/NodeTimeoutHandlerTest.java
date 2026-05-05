package org.jeecg.modules.bpm.scheduler;

import org.flowable.engine.TaskService;
import org.jeecg.modules.bpm.scheduler.service.NodeTimeoutHandler;
import org.jeecg.modules.bpm.scheduler.service.OverdueTaskRow;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class NodeTimeoutHandlerTest {

    private OverdueTaskRow row(String taskId, String actInstId, String assignee, String action) {
        OverdueTaskRow r = new OverdueTaskRow();
        r.setTaskId(taskId);
        r.setActInstId(actInstId);
        r.setAssignee(assignee);
        r.setTimeoutAction(action);
        return r;
    }

    @Test
    void remindLogsAndDoesNotCompleteTask() {
        TaskService ts = mock(TaskService.class);
        BpmOrgService org = mock(BpmOrgService.class);

        new NodeTimeoutHandler(ts, org).handle(row("t1", "p1", "user1", "REMIND"));

        verify(ts, never()).complete(any());
        verify(ts, never()).setAssignee(any(), any());
    }

    @Test
    void autoPassCompletesTask() {
        TaskService ts = mock(TaskService.class);
        BpmOrgService org = mock(BpmOrgService.class);

        new NodeTimeoutHandler(ts, org).handle(row("t2", "p2", "user2", "AUTO_PASS"));

        verify(ts).complete("t2");
    }

    @Test
    void escalateReassignsToDept() {
        TaskService ts = mock(TaskService.class);
        BpmOrgService org = mock(BpmOrgService.class);
        when(org.findUserMainDeptId(5L)).thenReturn(99L);

        new NodeTimeoutHandler(ts, org).handle(row("t3", "p3", "5", "ESCALATE"));

        verify(ts).setAssignee("t3", "99");
    }

    @Test
    void escalateSkipsWhenNoAssignee() {
        TaskService ts = mock(TaskService.class);
        BpmOrgService org = mock(BpmOrgService.class);

        new NodeTimeoutHandler(ts, org).handle(row("t4", "p4", null, "ESCALATE"));

        verify(ts, never()).setAssignee(any(), any());
    }

    @Test
    void escalateSkipsWhenNoDeptFound() {
        TaskService ts = mock(TaskService.class);
        BpmOrgService org = mock(BpmOrgService.class);
        when(org.findUserMainDeptId(7L)).thenReturn(null);

        new NodeTimeoutHandler(ts, org).handle(row("t5", "p5", "7", "ESCALATE"));

        verify(ts, never()).setAssignee(any(), any());
    }

    @Test
    void nullActionIsNoOp() {
        TaskService ts = mock(TaskService.class);
        BpmOrgService org = mock(BpmOrgService.class);

        new NodeTimeoutHandler(ts, org).handle(row("t6", "p6", "user6", null));

        verify(ts, never()).complete(any());
    }

    @Test
    void autoPassSwallowsException() {
        TaskService ts = mock(TaskService.class);
        BpmOrgService org = mock(BpmOrgService.class);
        doThrow(new RuntimeException("task gone")).when(ts).complete("t7");

        new NodeTimeoutHandler(ts, org).handle(row("t7", "p7", "user7", "AUTO_PASS"));
        // no exception propagated
    }
}
