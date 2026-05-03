package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.expression.BpmExpressionCacheKey;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bpm/v1/expression")
public class ExpressionTestController {

    private final BpmExpressionEvaluator evaluator;

    public ExpressionTestController(BpmExpressionEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test(@RequestBody Map<String, Object> body) {
        String expression = (String) body.getOrDefault("expression", "");
        @SuppressWarnings("unchecked")
        Map<String, Object> formData = body.containsKey("formData")
                ? (Map<String, Object>) body.get("formData")
                : new HashMap<>();

        Map<String, Object> env = new HashMap<>();
        Map<String, Object> formEnv = new HashMap<>(formData != null ? formData : new HashMap<>());
        env.put("form", formEnv);

        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("__test__", 0, expression);

        long start = Instant.now().toEpochMilli();
        Map<String, Object> result = new HashMap<>();
        try {
            Object val = evaluator.evaluate(key, env);
            result.put("result", val);
            result.put("durationMs", Instant.now().toEpochMilli() - start);
        } catch (Exception e) {
            result.put("result", null);
            result.put("error", e.getMessage());
            result.put("durationMs", Instant.now().toEpochMilli() - start);
        }
        return ResponseEntity.ok(result);
    }
}
