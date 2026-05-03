package org.jeecg.modules.bpm.service.assignee.impl;

import org.jeecg.modules.bpm.domain.enums.AssigneeStrategyType;
import org.jeecg.modules.bpm.service.assignee.AssigneeStrategy;
import org.jeecg.modules.bpm.service.assignee.ResolveContext;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DeptLeaderStrategy implements AssigneeStrategy {
    private final BpmOrgService org;

    public DeptLeaderStrategy(BpmOrgService org) { this.org = org; }

    @Override
    public String type() { return AssigneeStrategyType.DEPT_LEADER.name(); }

    @Override
    public List<Long> resolve(ResolveContext ctx) {
        Long deptId = ctx.getApplyDeptId();
        if (deptId == null) return Collections.emptyList();
        return org.findDeptLeaders(deptId);
    }
}
