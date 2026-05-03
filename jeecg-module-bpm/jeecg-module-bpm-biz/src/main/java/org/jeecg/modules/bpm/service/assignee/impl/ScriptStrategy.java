package org.jeecg.modules.bpm.service.assignee.impl;

import org.jeecg.modules.bpm.domain.enums.AssigneeStrategyType;
import org.jeecg.modules.bpm.expression.BpmExpressionCacheKey;
import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.jeecg.modules.bpm.service.assignee.AssigneeStrategy;
import org.jeecg.modules.bpm.service.assignee.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScriptStrategy implements AssigneeStrategy {

    private static final Logger log = LoggerFactory.getLogger(ScriptStrategy.class);

    private final BpmExpressionEvaluator evaluator;
    private final BpmExpressionContextBuilder ctxBuilder;

    public ScriptStrategy(BpmExpressionEvaluator evaluator,
                          BpmExpressionContextBuilder ctxBuilder) {
        this.evaluator = evaluator;
        this.ctxBuilder = ctxBuilder;
    }

    @Override
    public String type() { return AssigneeStrategyType.SCRIPT.name(); }

    @Override
    public List<Long> resolve(ResolveContext ctx) {
        Map<String, Object> payload = ctx.getStrategyPayload();
        if (payload == null) return Collections.emptyList();
        String script = (String) payload.get("script");
        if (script == null || script.isBlank()) return Collections.emptyList();

        // Get defKey/defVersion from process vars (set by InstanceService.start())
        Map<String, Object> processVars = ctx.getProcessVars() != null ? ctx.getProcessVars() : Collections.emptyMap();
        String defKey = (String) processVars.get("bpm_def_key");
        Object verObj = processVars.get("bpm_def_version");
        int defVersion = (verObj instanceof Number) ? ((Number) verObj).intValue() : 0;
        String formId = (String) processVars.get("bpm_form_id");
        String businessKey = ctx.getProcInstId(); // use procInstId as fallback

        Map<String, Object> env = new HashMap<>(ctxBuilder.build(formId, businessKey));
        // Merge formData from ResolveContext into form namespace
        Map<String, Object> formData = ctx.getFormData();
        if (formData != null && !formData.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> formMap = (Map<String, Object>) env.get("form");
            if (formMap != null) formMap.putAll(formData);
        }

        BpmExpressionCacheKey key = BpmExpressionCacheKey.of(
                defKey != null ? defKey : "anonymous",
                defVersion,
                script);
        try {
            Object result = evaluator.evaluate(key, env);
            return toUserIds(result);
        } catch (Exception e) {
            log.warn("ScriptStrategy evaluation failed, returning empty: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Long> toUserIds(Object result) {
        if (result == null) return Collections.emptyList();
        if (result instanceof Number) return Collections.singletonList(((Number) result).longValue());
        if (result instanceof Collection) {
            List<Long> out = new ArrayList<>();
            for (Object o : (Collection<?>) result) {
                if (o instanceof Number) out.add(((Number) o).longValue());
                else if (o != null) {
                    try { out.add(Long.valueOf(o.toString())); } catch (NumberFormatException ignored) {}
                }
            }
            return out;
        }
        try { return Collections.singletonList(Long.valueOf(result.toString())); }
        catch (NumberFormatException e) { return Collections.emptyList(); }
    }
}
