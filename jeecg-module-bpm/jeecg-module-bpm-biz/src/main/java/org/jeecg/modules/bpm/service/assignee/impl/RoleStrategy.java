package org.jeecg.modules.bpm.service.assignee.impl;

import org.jeecg.modules.bpm.domain.enums.AssigneeStrategyType;
import org.jeecg.modules.bpm.service.assignee.AssigneeStrategy;
import org.jeecg.modules.bpm.service.assignee.ResolveContext;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RoleStrategy implements AssigneeStrategy {
    private final BpmOrgService org;

    public RoleStrategy(BpmOrgService org) { this.org = org; }

    @Override
    public String type() { return AssigneeStrategyType.ROLE.name(); }

    @Override
    public List<Long> resolve(ResolveContext ctx) {
        if (ctx.getStrategyPayload() == null) return Collections.emptyList();
        Object code = ctx.getStrategyPayload().get("roleCode");
        if (!(code instanceof String) || ((String) code).isEmpty()) return Collections.emptyList();
        return org.findUsersByRole((String) code);
    }
}
