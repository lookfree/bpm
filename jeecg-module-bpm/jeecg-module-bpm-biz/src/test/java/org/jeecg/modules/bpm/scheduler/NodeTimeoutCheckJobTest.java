package org.jeecg.modules.bpm.scheduler;

import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.scheduler.service.NodeTimeoutHandler;
import org.jeecg.modules.bpm.scheduler.service.OverdueTaskRow;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

class NodeTimeoutCheckJobTest {

    @Test
    void noOverdueTasksMeansNoHandlerCalls() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        NodeTimeoutHandler handler = mock(NodeTimeoutHandler.class);
        when(mapper.selectOverdueRunningTasks()).thenReturn(Collections.emptyList());

        new NodeTimeoutCheckJob(mapper, handler).run();

        verify(handler, never()).handle(any());
    }

    @Test
    void eachOverdueTaskIsHandled() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        NodeTimeoutHandler handler = mock(NodeTimeoutHandler.class);

        OverdueTaskRow r1 = new OverdueTaskRow();
        r1.setTaskId("t1");
        OverdueTaskRow r2 = new OverdueTaskRow();
        r2.setTaskId("t2");
        when(mapper.selectOverdueRunningTasks()).thenReturn(List.of(r1, r2));

        new NodeTimeoutCheckJob(mapper, handler).run();

        verify(handler).handle(r1);
        verify(handler).handle(r2);
    }

    @Test
    void handlerExceptionDoesNotAbortRemainingTasks() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        NodeTimeoutHandler handler = mock(NodeTimeoutHandler.class);

        OverdueTaskRow r1 = new OverdueTaskRow();
        r1.setTaskId("t1");
        OverdueTaskRow r2 = new OverdueTaskRow();
        r2.setTaskId("t2");
        when(mapper.selectOverdueRunningTasks()).thenReturn(List.of(r1, r2));
        doThrow(new RuntimeException("boom")).when(handler).handle(r1);

        new NodeTimeoutCheckJob(mapper, handler).run();

        verify(handler).handle(r2);
    }
}
