package org.jeecg.modules.bpm.service.assignee;

import java.util.List;

public interface AssigneeStrategy {
    String type();
    List<Long> resolve(ResolveContext ctx);
}
