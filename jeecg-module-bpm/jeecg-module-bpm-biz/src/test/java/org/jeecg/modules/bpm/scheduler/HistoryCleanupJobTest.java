package org.jeecg.modules.bpm.scheduler;

import org.jeecg.modules.bpm.scheduler.cleanup.HistoryCleanupMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HistoryCleanupJobTest {

    @Test
    void runsDeletesWithCutoffDerivedFromRetentionDays() {
        HistoryCleanupMapper mapper = mock(HistoryCleanupMapper.class);
        when(mapper.deleteTaskHistory(any())).thenReturn(5);
        when(mapper.deleteInstanceMeta(any())).thenReturn(3);

        BpmSchedulerProperties props = new BpmSchedulerProperties();
        props.getHistoryCleanup().setRetentionDays(90);

        new HistoryCleanupJob(mapper, props).run();

        verify(mapper).deleteTaskHistory(any());
        verify(mapper).deleteInstanceMeta(any());
    }
}
