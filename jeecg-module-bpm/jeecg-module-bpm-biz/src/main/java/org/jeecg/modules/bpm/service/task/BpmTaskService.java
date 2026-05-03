package org.jeecg.modules.bpm.service.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.FormBindingMapper;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.jeecg.modules.bpm.service.form.FormPermissionMerger;
import org.jeecg.modules.bpm.service.nodecfg.NodeConfigService;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BpmTaskService {

    private final org.flowable.engine.TaskService flowableTaskService;
    private final HistoryService historyService;
    private final BpmUserContext userContext;
    private final TaskHistoryWriter historyWriter;
    private final InstanceMetaMapper instanceMetaMapper;
    private final FormBindingMapper formBindingMapper;
    private final NodeConfigService nodeConfigService;
    private final FormPermissionMerger formPermissionMerger;
    private final BpmFormService formService;

    public BpmTaskService(org.flowable.engine.TaskService flowableTaskService,
                          HistoryService historyService, BpmUserContext userContext,
                          TaskHistoryWriter historyWriter, InstanceMetaMapper instanceMetaMapper,
                          FormBindingMapper formBindingMapper, NodeConfigService nodeConfigService,
                          FormPermissionMerger formPermissionMerger, BpmFormService formService) {
        this.flowableTaskService = flowableTaskService;
        this.historyService = historyService;
        this.userContext = userContext;
        this.historyWriter = historyWriter;
        this.instanceMetaMapper = instanceMetaMapper;
        this.formBindingMapper = formBindingMapper;
        this.nodeConfigService = nodeConfigService;
        this.formPermissionMerger = formPermissionMerger;
        this.formService = formService;
    }

    public List<Task> listTodo() {
        String userId = String.valueOf(userContext.currentUserId());
        return flowableTaskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime().desc()
                .list();
    }

    public List<HistoricTaskInstance> listDone() {
        String userId = String.valueOf(userContext.currentUserId());
        return historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();
    }

    public void complete(String taskId, String action, String comment, Map<String, Object> formData) {
        complete(taskId, action, comment, null, formData);
    }

    public void complete(String taskId, String action, String comment, String targetUserId, Map<String, Object> formData) {
        Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new IllegalArgumentException("task not found: " + taskId);

        if ("TRANSFER".equals(action)) {
            String assignee = (targetUserId != null && !targetUserId.isEmpty()) ? targetUserId : "";
            flowableTaskService.setAssignee(taskId, assignee);

            InstanceMeta meta = instanceMetaMapper.selectOne(
                    new LambdaQueryWrapper<InstanceMeta>()
                            .eq(InstanceMeta::getActInstId, task.getProcessInstanceId()));
            String instId = meta != null ? meta.getId() : task.getProcessInstanceId();
            historyWriter.write(new TaskHistoryWriter.Entry(
                    taskId, instId, task.getTaskDefinitionKey(),
                    userContext.currentUserId(), "TRANSFER", comment, null));
            return;
        }

        if ("COUNTERSIGN".equals(action)) {
            String assignee = (targetUserId != null && !targetUserId.isEmpty()) ? targetUserId : "";
            org.flowable.task.api.Task sub = flowableTaskService.newTask();
            sub.setName(task.getName() + "（加签）");
            sub.setAssignee(assignee);
            sub.setParentTaskId(taskId);
            sub.setOwner(String.valueOf(userContext.currentUserId()));
            flowableTaskService.saveTask(sub);

            InstanceMeta meta = instanceMetaMapper.selectOne(
                    new LambdaQueryWrapper<InstanceMeta>()
                            .eq(InstanceMeta::getActInstId, task.getProcessInstanceId()));
            String instId = meta != null ? meta.getId() : task.getProcessInstanceId();
            historyWriter.write(new TaskHistoryWriter.Entry(
                    taskId, instId, task.getTaskDefinitionKey(),
                    userContext.currentUserId(), "COUNTERSIGN", comment, null));
            return;
        }

        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new IllegalArgumentException("unsupported_action: " + action);
        }

        Map<String, Object> vars = new HashMap<>();
        if (formData != null) vars.putAll(formData);
        if ("REJECT".equals(action)) vars.put("_rejected", true);

        flowableTaskService.complete(taskId, vars);

        InstanceMeta meta = instanceMetaMapper.selectOne(
                new LambdaQueryWrapper<InstanceMeta>()
                        .eq(InstanceMeta::getActInstId, task.getProcessInstanceId()));

        // Check if process instance finished, update meta state
        boolean finished = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .finished()
                .count() > 0;
        if (finished && meta != null) {
            meta.setState("COMPLETED");
            meta.setEndTime(java.time.LocalDateTime.now());
            instanceMetaMapper.updateById(meta);
        }

        String instId = meta != null ? meta.getId() : task.getProcessInstanceId();
        historyWriter.write(new TaskHistoryWriter.Entry(
                taskId, instId, task.getTaskDefinitionKey(),
                userContext.currentUserId(), action, comment, null));
    }

    public Map<String, Object> getTaskForm(String taskId) {
        Task task = flowableTaskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) throw new IllegalArgumentException("task not found: " + taskId);

        InstanceMeta meta = instanceMetaMapper.selectOne(
                new LambdaQueryWrapper<InstanceMeta>()
                        .eq(InstanceMeta::getActInstId, task.getProcessInstanceId()));
        if (meta == null) return Collections.emptyMap();

        FormBinding binding = formBindingMapper.selectOne(
                new LambdaQueryWrapper<FormBinding>()
                        .eq(FormBinding::getDefId, meta.getDefId())
                        .eq(FormBinding::getPurpose, "APPROVE"));
        if (binding == null) return Collections.emptyMap();

        BpmFormSchema schema = formService.loadFormSchema(binding.getFormId());

        String formPerm = nodeConfigService.findByActDefAndNode(
                task.getProcessDefinitionId(), task.getTaskDefinitionKey())
                .map(cfg -> cfg.getFormPerm()).orElse(null);
        BpmFormSchema merged = formPermissionMerger.merge(schema, formPerm);

        Map<String, Object> data = meta.getBusinessKey() != null
                ? formService.loadFormData(binding.getFormId(), meta.getBusinessKey())
                : Collections.emptyMap();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schema", merged);
        result.put("data", data);
        return result;
    }
}
