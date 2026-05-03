package org.jeecg.modules.bpm.expression;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class BpmExpressionEvaluatorTest {
    private final BpmExpressionEvaluator evaluator = new BpmExpressionEvaluator(200L);

    @Test
    void cachesCompiledExpressionByKey() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("def_a", 1, "form.amount > 0");
        Map<String,Object> ctx = Map.of("form", Map.of("amount", 5));
        Object first  = evaluator.evaluate(key, ctx);
        Object second = evaluator.evaluate(key, ctx);
        assertThat(first).isEqualTo(Boolean.TRUE);
        assertThat(second).isEqualTo(Boolean.TRUE);
        assertThat(evaluator.cacheSize()).isEqualTo(1);
    }

    @Test
    void differentVersionsKeepSeparateCacheEntries() {
        BpmExpressionCacheKey k1 = BpmExpressionCacheKey.of("def_a", 1, "form.amount > 0");
        BpmExpressionCacheKey k2 = BpmExpressionCacheKey.of("def_a", 2, "form.amount > 0");
        evaluator.evaluate(k1, Map.of("form", Map.of("amount", 1)));
        evaluator.evaluate(k2, Map.of("form", Map.of("amount", 1)));
        assertThat(evaluator.cacheSize()).isEqualTo(2);
    }
}
