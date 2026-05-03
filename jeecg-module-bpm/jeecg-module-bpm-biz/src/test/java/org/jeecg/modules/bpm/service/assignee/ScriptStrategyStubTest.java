package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.jeecg.modules.bpm.service.assignee.impl.ScriptStrategy;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScriptStrategyStubTest {
    @Test
    void typeIsSCRIPT() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        assertThat(s.type()).isEqualTo("SCRIPT");
    }

    @Test
    void noScriptReturnsEmpty() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        assertThat(s.resolve(ResolveContext.builder().strategyPayload(Map.of("script", "")).build())).isEmpty();
    }
}
