package org.jeecg.modules.bpm.expression;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AviatorSandboxTest {
    private final BpmExpressionEvaluator evaluator = new BpmExpressionEvaluator(200L);

    @Test
    void rejectsReflectionAccess() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "Class.forName('java.lang.Runtime')");
        assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                .isInstanceOf(BpmExpressionException.class);
    }

    @Test
    void rejectsSystemAccess() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "System.exit(0)");
        assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                .isInstanceOf(BpmExpressionException.class);
    }

    @Test
    void rejectsRuntimeExec() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "Runtime.getRuntime().exec('id')");
        assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                .isInstanceOf(BpmExpressionException.class);
    }

    @Test
    void allowsArithmeticAndComparison() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "form.amount > 10000");
        Map<String,Object> ctx = Map.of("form", Map.of("amount", 12000));
        assertThat(evaluator.evaluate(key, ctx)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void allowsBooleanLogic() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "form.amount > 1000 && form.amount < 5000");
        Map<String,Object> ctx = Map.of("form", Map.of("amount", 2500));
        assertThat(evaluator.evaluate(key, ctx)).isEqualTo(Boolean.TRUE);
    }
}
