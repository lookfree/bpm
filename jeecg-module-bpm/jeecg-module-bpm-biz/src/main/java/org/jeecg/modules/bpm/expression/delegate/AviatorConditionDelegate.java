package org.jeecg.modules.bpm.expression.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.jeecg.modules.bpm.expression.BpmExpressionCacheKey;
import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposed to Flowable JUEL as bean "aviatorEval".
 * BPMN usage: ${aviatorEval.eval(execution, 'form.amount > 10000')}
 */
@Component("aviatorEval")
public class AviatorConditionDelegate {

    private final BpmExpressionEvaluator evaluator;
    private final BpmExpressionContextBuilder ctxBuilder;

    public AviatorConditionDelegate(BpmExpressionEvaluator evaluator,
                                    BpmExpressionContextBuilder ctxBuilder) {
        this.evaluator = evaluator;
        this.ctxBuilder = ctxBuilder;
    }

    public Boolean eval(DelegateExecution execution, String expression) {
        String defKey = (String) execution.getVariable("bpm_def_key");
        Integer ver   = (Integer) execution.getVariable("bpm_def_version");
        String formId = (String) execution.getVariable("bpm_form_id");
        String bizKey = execution.getProcessInstanceBusinessKey();

        Map<String, Object> env = new HashMap<>(ctxBuilder.build(formId, bizKey));

        // Also merge form.* prefixed process variables for convenience
        @SuppressWarnings("unchecked")
        Map<String, Object> formMap = (Map<String, Object>) env.get("form");
        if (formMap != null) {
            execution.getVariables().forEach((k, v) -> {
                if (k.startsWith("form.")) {
                    formMap.put(k.substring(5), v);
                }
            });
        }

        BpmExpressionCacheKey key = BpmExpressionCacheKey.of(
                defKey == null ? "anonymous" : defKey,
                ver == null ? 0 : ver,
                expression);
        Object result = evaluator.evaluate(key, env);
        if (result instanceof Boolean) return (Boolean) result;
        return Boolean.parseBoolean(String.valueOf(result));
    }
}
