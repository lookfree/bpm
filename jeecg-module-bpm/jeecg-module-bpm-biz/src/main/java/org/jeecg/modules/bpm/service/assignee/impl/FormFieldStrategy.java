package org.jeecg.modules.bpm.service.assignee.impl;

import org.jeecg.modules.bpm.domain.enums.AssigneeStrategyType;
import org.jeecg.modules.bpm.service.assignee.AssigneeStrategy;
import org.jeecg.modules.bpm.service.assignee.ResolveContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FormFieldStrategy implements AssigneeStrategy {
    @Override
    public String type() { return AssigneeStrategyType.FORM_FIELD.name(); }

    @Override
    public List<Long> resolve(ResolveContext ctx) {
        if (ctx.getStrategyPayload() == null || ctx.getFormData() == null) return Collections.emptyList();
        Object fn = ctx.getStrategyPayload().get("fieldName");
        if (!(fn instanceof String)) return Collections.emptyList();
        Object raw = ctx.getFormData().get(fn);
        if (raw == null) return Collections.emptyList();
        List<Long> result = new ArrayList<>();
        if (raw instanceof Iterable) {
            for (Object o : (Iterable<?>) raw) addLong(result, o);
        } else {
            addLong(result, raw);
        }
        return result;
    }

    private void addLong(List<Long> r, Object o) {
        if (o instanceof Number) r.add(((Number) o).longValue());
        else if (o instanceof String) try { r.add(Long.parseLong((String) o)); } catch (NumberFormatException ignored) {}
    }
}
