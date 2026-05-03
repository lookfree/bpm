package org.jeecg.modules.bpm.service.task;

import org.jeecg.modules.bpm.domain.entity.TaskHistory;
import org.jeecg.modules.bpm.mapper.TaskHistoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskHistoryWriterTest {

    TaskHistoryMapper mapper = mock(TaskHistoryMapper.class);
    TaskHistoryWriter writer = new TaskHistoryWriter(mapper);

    @Test
    void writeInsertsRecord() {
        TaskHistoryWriter.Entry e = new TaskHistoryWriter.Entry(
                "task1", "inst1", "approve", 7L, "APPROVE", "ok", null);
        writer.write(e);
        org.mockito.ArgumentCaptor<TaskHistory> cap = forClass(TaskHistory.class);
        verify(mapper).insert(cap.capture());
        assertThat(cap.getValue().getActTaskId()).isEqualTo("task1");
        assertThat(cap.getValue().getAction()).isEqualTo("APPROVE");
    }

    @Test
    void duplicateInsertSwallowsExceptionForIdempotency() {
        when(mapper.insert(any())).thenThrow(new DuplicateKeyException("uk_bpm_task_history_task_action"));
        TaskHistoryWriter.Entry e = new TaskHistoryWriter.Entry(
                "task1", "inst1", "n", 7L, "APPROVE", null, null);
        assertThatCode(() -> writer.write(e)).doesNotThrowAnyException();
    }

    @Test
    void otherSqlExceptionPropagates() {
        when(mapper.insert(any())).thenThrow(new DataIntegrityViolationException("other"));
        TaskHistoryWriter.Entry e = new TaskHistoryWriter.Entry(
                "task1", "inst1", "n", 7L, "APPROVE", null, null);
        assertThatThrownBy(() -> writer.write(e))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
