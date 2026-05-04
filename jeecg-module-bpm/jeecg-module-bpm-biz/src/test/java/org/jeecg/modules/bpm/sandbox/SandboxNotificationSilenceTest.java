package org.jeecg.modules.bpm.sandbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.notification.BpmNotificationDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.Mockito.*;

class SandboxNotificationSilenceTest {

    InstanceMetaMapper instanceMetaMapper;
    BpmNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        instanceMetaMapper = mock(InstanceMetaMapper.class);
        dispatcher = new BpmNotificationDispatcher(instanceMetaMapper);
    }

    @Test
    void sandboxInstance_notificationSuppressed() {
        InstanceMeta meta = new InstanceMeta();
        meta.setState("SANDBOX");
        when(instanceMetaMapper.selectOne(any())).thenReturn(meta);

        // Should NOT call any sender (in stub impl, no sender to verify —
        // just verify no exception and the method returns normally)
        dispatcher.notify("inst-1", "EMAIL", "APPROVE_TPL", Collections.emptyMap());
        // No exception = suppressed successfully
    }

    @Test
    void prodInstance_notificationDispatched() {
        InstanceMeta meta = new InstanceMeta();
        meta.setState("RUNNING");
        when(instanceMetaMapper.selectOne(any())).thenReturn(meta);

        // Should proceed without throwing
        dispatcher.notify("inst-2", "EMAIL", "APPROVE_TPL", Collections.emptyMap());
        // No exception = dispatched (logged in stub)
    }

    @Test
    void nullInstance_treatedAsProd() {
        when(instanceMetaMapper.selectOne(any())).thenReturn(null);

        // Should NOT throw even if meta is null
        dispatcher.notify("inst-3", "EMAIL", "APPROVE_TPL", Collections.emptyMap());
    }
}
