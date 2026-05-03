package org.jeecg.modules.bpm.service.assignee;

import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.jeecg.modules.bpm.service.assignee.impl.ScriptStrategy;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScriptStrategyTest {

    private ResolveContext buildCtx(Map<String, Object> payload, Map<String, Object> formData,
                                    Map<String, Object> processVars) {
        return ResolveContext.builder()
                .procInstId("pi_1")
                .strategyPayload(payload)
                .formData(formData)
                .processVars(processVars)
                .build();
    }

    @Test
    void resolvesSingleUserIdFromEvaluator() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        when(ctxBuilder.build(any(), any())).thenReturn(
                Map.of("form", new HashMap<>(), "sys", Map.of(), "user", Map.of()));
        when(evaluator.evaluate(any(), any())).thenReturn(42L);

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = buildCtx(
                Map.of("script", "form.managerId"),
                Map.of("managerId", 42),
                Map.of("bpm_def_key", "def_a", "bpm_def_version", 1));

        List<Long> ids = s.resolve(ctx);
        assertThat(ids).containsExactly(42L);
    }

    @Test
    void resolvesUserListFromExpression() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        when(ctxBuilder.build(any(), any())).thenReturn(
                Map.of("form", new HashMap<>(), "sys", Map.of(), "user", Map.of()));
        when(evaluator.evaluate(any(), any())).thenReturn(List.of(1L, 2L, 3L));

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = buildCtx(Map.of("script", "[1, 2, 3]"), Collections.emptyMap(), Collections.emptyMap());

        List<Long> ids = s.resolve(ctx);
        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    void returnsEmptyOnNullResult() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        when(ctxBuilder.build(any(), any())).thenReturn(
                Map.of("form", new HashMap<>(), "sys", Map.of(), "user", Map.of()));
        when(evaluator.evaluate(any(), any())).thenReturn(null);

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = buildCtx(Map.of("script", "form.managerId"), Collections.emptyMap(), Collections.emptyMap());

        assertThat(s.resolve(ctx)).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoScript() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = buildCtx(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());

        assertThat(s.resolve(ctx)).isEmpty();
    }

    @Test
    void returnsEmptyWhenEvaluationFails() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        when(ctxBuilder.build(any(), any())).thenReturn(
                Map.of("form", new HashMap<>(), "sys", Map.of(), "user", Map.of()));
        when(evaluator.evaluate(any(), any())).thenThrow(
                new org.jeecg.modules.bpm.expression.BpmExpressionException("compile failed"));

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = buildCtx(Map.of("script", "bad.expr"), Collections.emptyMap(), Collections.emptyMap());

        assertThat(s.resolve(ctx)).isEmpty();
    }
}
