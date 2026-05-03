package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.service.assignee.impl.ScriptStrategy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScriptStrategyStubTest {
    ScriptStrategy s = new ScriptStrategy();

    @Test
    void typeIsSCRIPT() {
        assertThat(s.type()).isEqualTo("SCRIPT");
    }

    @Test
    void p2StubAlwaysReturnsEmpty() {
        assertThat(s.resolve(ResolveContext.builder().strategyPayload(Map.of("script", "x")).build())).isEmpty();
    }
}
