package org.jeecg.modules.bpm.sandbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SandboxRunServiceTest {

    SandboxRunMapper mapper;
    SandboxRunService svc;

    @BeforeEach
    void setUp() {
        mapper = mock(SandboxRunMapper.class);
        svc = new SandboxRunService(mapper);
    }

    @Test
    void start_createsRunningRun() {
        when(mapper.insert(any())).thenAnswer(inv -> {
            SandboxRun r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });
        Long id = svc.start("def1", 42L);
        assertThat(id).isEqualTo(1L);
        verify(mapper).insert(argThat(r ->
            "def1".equals(r.getDefIdDraft())
            && Long.valueOf(42L).equals(r.getRunnerId())
            && "RUNNING".equals(r.getResult())
            && r.getStartTime() != null
        ));
    }

    @Test
    void appendLog_appendsToExistingLog() {
        SandboxRun existing = run(1L, "RUNNING", "old\n");
        when(mapper.selectById(1L)).thenReturn(existing);
        when(mapper.updateById(any())).thenReturn(1);

        svc.appendLog(1L, "new line");

        verify(mapper).updateById(argThat(r -> r.getLog().contains("old\n") && r.getLog().contains("new line")));
    }

    @Test
    void appendLog_finishedRun_throwsIllegalState() {
        SandboxRun existing = run(2L, "PASS", "done\n");
        when(mapper.selectById(2L)).thenReturn(existing);
        assertThatThrownBy(() -> svc.appendLog(2L, "more"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void finish_setsResultAndEndTime() {
        SandboxRun existing = run(3L, "RUNNING", "");
        when(mapper.selectById(3L)).thenReturn(existing);
        when(mapper.updateById(any())).thenReturn(1);

        svc.finish(3L, SandboxResult.PASS);

        verify(mapper).updateById(argThat(r -> "PASS".equals(r.getResult()) && r.getEndTime() != null));
    }

    @Test
    void findById_delegatesToMapper() {
        SandboxRun r = run(4L, "RUNNING", "");
        when(mapper.selectById(4L)).thenReturn(r);
        assertThat(svc.findById(4L)).isSameAs(r);
    }

    private SandboxRun run(Long id, String result, String log) {
        SandboxRun r = new SandboxRun();
        r.setId(id); r.setResult(result); r.setLog(log);
        return r;
    }
}
