# P2 验收清单 ✅

> 验证时间：2026-05-03 · Flowable 6.8.0 + Testcontainers MySQL 8.0.27

## 数据层
- [x] 5 张 P2 业务表 DDL 落库（bpm_node_config / bpm_assignee_strategy / bpm_form_binding / bpm_instance_meta / bpm_task_history）
- [x] bpm_task_history 唯一索引 uk_bpm_task_history_task_action 验证（IT BpmSchemaP2Test）

## 节点人员调度
- [x] AssigneeStrategy 接口 + ResolveContext POJO（bpm-biz 内部，不进 SPI）
- [x] FixedUserStrategy / RoleStrategy / DeptLeaderStrategy / UpperDeptStrategy / FormFieldStrategy 5 真实实现 + 单测
- [x] ScriptStrategy stub（返回空），P3 接 Aviator
- [x] AssigneeResolver 按 JSON type 路由 + 去重 + 未知 type 兜底
- [x] AssigneeAssignmentTaskListener 接 Flowable TASK_CREATED 事件全局生效

## 表单
- [x] FormBindingService + FormBindingController CRUD（POST/GET/DELETE）
- [x] FormPermissionMerger 实现 spec §5.3：READ_WRITE / READ_ONLY / HIDDEN / required overlay

## 实例 + 任务
- [x] InstanceController POST /bpm/v1/instance（saveFormSubmission → startProcessInstanceByKey）
- [x] InstanceController GET /bpm/v1/instance/{id}
- [x] TaskController GET /bpm/v1/task/todo + /done
- [x] TaskController POST /bpm/v1/task/{id}/complete（APPROVE/REJECT；TRANSFER/COUNTERSIGN 显式拒绝至 P3）
- [x] TaskController GET /bpm/v1/task/{id}/form（schema + 节点权限合并 + formData）
- [x] bpm_task_history 每次 complete 写入；唯一索引保证幂等
- [x] 流程结束后 bpm_instance_meta.state 更新为 COMPLETED

## Adapter
- [x] BpmOrgServiceJeecgImpl JdbcTemplate 直读 sys_user/sys_role/sys_user_role/sys_user_depart/sys_depart/sys_position
- [x] BpmFormServiceJeecgImpl JdbcTemplate 直读写 onl_cgform_head/onl_cgform_field + 动态表单数据表

## 前端（frontend-snapshot）
- [x] NodeAssigneePanel.vue 集成多策略类型选择（USER/ROLE/DEPT_LEADER/UPPER_DEPT/FORM_FIELD/SCRIPT）
- [x] FormBindingPage.vue + TodoListPage.vue + DoneListPage.vue（a-table）
- [x] TaskApprovePage.vue（动态表单 + 同意/拒绝）+ InstanceStartPage.vue
- [x] src/api/bpm/{instance,task,form-binding}.ts 三份 API 层
- [x] 路由 /bpm/{todo,done,form-binding,instance/start/:defId,task/approve/:taskId} 接入

## 端到端
- [x] ApplyApproveFlowIT：申请→审批 happy path 全程绿（Testcontainers MySQL 真引擎）
- [x] 任务历史幂等 IT 通过

## 非范围（已确认下沉到 P3+）
- ScriptStrategy 完整 Aviator 实现（P3）
- sequenceFlow 条件表达式求值（P3）
- 多人节点 SEQUENCE / PARALLEL / ANY（P3）
- TRANSFER / COUNTERSIGN（P3）
- DRAFT→TESTING→PUBLISHED→ARCHIVED 全状态机 + Redisson + 沙箱（P4）
- 监控统计页（P5）
