package org.jeecg.modules.bpm.engine;

import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.jeecg.modules.bpm.domain.entity.NodeConfig;
import org.jeecg.modules.bpm.service.assignee.AssigneeResolver;
import org.jeecg.modules.bpm.service.assignee.ResolveContext;
import org.jeecg.modules.bpm.service.nodecfg.NodeConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AssigneeAssignmentTaskListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(AssigneeAssignmentTaskListener.class);

    private final NodeConfigService nodeCfg;
    private final AssigneeResolver resolver;

    public AssigneeAssignmentTaskListener(NodeConfigService nodeCfg, AssigneeResolver resolver) {
        this.nodeCfg = nodeCfg;
        this.resolver = resolver;
    }

    @Override
    public void notify(DelegateTask task) {
        String defId = task.getProcessDefinitionId();
        String nodeId = task.getTaskDefinitionKey();
        Optional<NodeConfig> cfgOpt = nodeCfg.findByActDefAndNode(defId, nodeId);
        if (!cfgOpt.isPresent() || cfgOpt.get().getAssigneeStrategy() == null) return;

        ResolveContext ctx = ResolveContext.builder()
                .procInstId(task.getProcessInstanceId())
                .nodeId(nodeId)
                .formData(safeMap(task.getVariable("formData")))
                .processVars(task.getVariables())
                .applyUserId(asLong(task.getVariable("applyUserId")))
                .applyDeptId(asLong(task.getVariable("applyDeptId")))
                .build();

        List<Long> users = resolver.resolve(cfgOpt.get().getAssigneeStrategy(), ctx);
        if (users.isEmpty()) {
            log.warn("AssigneeResolver returned empty for procInst={} node={}", task.getProcessInstanceId(), nodeId);
            return;
        }
        if (users.size() == 1) {
            task.setAssignee(String.valueOf(users.get(0)));
        } else {
            for (Long uid : users) {
                task.addCandidateUser(String.valueOf(uid));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : java.util.Collections.emptyMap();
    }

    private static Long asLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o instanceof String) {
            try { return Long.parseLong((String) o); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
