package org.jeecg.modules.bpm.service.assignee.impl;

import org.jeecg.modules.bpm.domain.enums.AssigneeStrategyType;
import org.jeecg.modules.bpm.service.assignee.AssigneeStrategy;
import org.jeecg.modules.bpm.service.assignee.ResolveContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FixedUserStrategy implements AssigneeStrategy {
    @Override
    public String type() { return AssigneeStrategyType.USER.name(); }

    @Override
    public List<Long> resolve(ResolveContext ctx) {
        Map<String, Object> payload = ctx.getStrategyPayload();
        if (payload == null) return Collections.emptyList();
        Object raw = payload.get("userIds");
        if (!(raw instanceof List)) return Collections.emptyList();
        List<Long> result = new ArrayList<>();
        for (Object o : (List<?>) raw) {
            if (o instanceof Number) result.add(((Number) o).longValue());
            else if (o instanceof String) try { result.add(Long.parseLong((String) o)); } catch (NumberFormatException ignored) {}
        }
        return result;
    }
}
