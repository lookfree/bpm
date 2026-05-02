package org.jeecg.modules.bpm.support;

import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpBpmUserContextTest {

    @Test
    void allMethodsReturnNullOrEmpty() {
        BpmUserContext ctx = new NoOpBpmUserContext();
        assertThat(ctx.currentUserId()).isNull();
        assertThat(ctx.currentUsername()).isNull();
        assertThat(ctx.currentDeptId()).isNull();
        assertThat(ctx.currentRoleCodes()).isEmpty();
    }
}
