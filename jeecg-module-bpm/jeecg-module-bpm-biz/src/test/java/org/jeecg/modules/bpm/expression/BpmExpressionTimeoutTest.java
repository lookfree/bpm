package org.jeecg.modules.bpm.expression;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static java.time.Duration.ofMillis;

class BpmExpressionTimeoutTest {
    @Test
    void killsInfiniteLoopUnderHardTimeout() {
        // 100ms timeout; 2000ms wall-clock budget from assertTimeout
        BpmExpressionEvaluator evaluator = new BpmExpressionEvaluator(100L);
        // for-loop over a huge range — range() is a built-in Aviator function,
        // available even with ALLOWED_CLASS_SET=emptySet(). This runs far longer
        // than 100ms, so the timeout enforcer must interrupt it.
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1,
                "let s = 0; for i in range(0, 1000000000) { s = s + i }; s > 0");
        assertTimeout(ofMillis(2000), () ->
            assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                    .isInstanceOf(BpmExpressionException.class)
                    .hasMessageContaining("timeout"));
    }
}
