package org.jeecg.modules.bpm.service.assignee.impl;

import org.jeecg.modules.bpm.domain.enums.AssigneeStrategyType;
import org.jeecg.modules.bpm.service.assignee.AssigneeStrategy;
import org.jeecg.modules.bpm.service.assignee.ResolveContext;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ScriptStrategy implements AssigneeStrategy {
    @Override
    public String type() { return AssigneeStrategyType.SCRIPT.name(); }

    @Override
    public List<Long> resolve(ResolveContext ctx) {
        // P3 will integrate Aviator with sandbox + 200ms timeout
        return Collections.emptyList();
    }
}
