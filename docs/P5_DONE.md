# P5 完成报告 — 监控运维模块

完成日期: 2026-05-05

## 后端实现

### 监控查询 API (`/bpm/v1/monitor`)
- `GET /instances` — 分页查询流程实例（支持 defKey/状态/申请人/部门/时间过滤）
- `GET /instances/{id}/diagram` — 获取流程图 XML 及当前活动节点
- `GET /stats` — 统计聚合（byDefinition/byNode/byApplyDept/byApplyDeptOverTime）
- `POST /instances/{id}/intervene` — 强制干预（FORCE_COMPLETE_TASK/FORCE_CANCEL/FORCE_REASSIGN）

### 调度任务 (`scheduler/`)
- `BpmSchedulerAutoConfiguration` — 注册 `@EnableScheduling` + `BpmSchedulerProperties` Bean
- `NodeTimeoutCheckJob` — 每 5 分钟扫描超时任务，委托 `NodeTimeoutHandler` 处理
- `NodeTimeoutHandler` — REMIND（日志）/ AUTO_PASS（自动完成）/ ESCALATE（转上级）
- `HistoryCleanupJob` — 每天凌晨 3 点清理保留期外的已完成实例

### 数据库
- `V5__monitor_index.sql` — 7 个复合索引，覆盖监控查询高频路径

## 前端实现 (`frontend-snapshot/`)

| 文件 | 说明 |
|------|------|
| `api/bpm/monitor.ts` | API 封装（listMonitorInstances / getInstanceDiagram / getStats）|
| `views/bpm/monitor/ProcessMonitor.vue` | 流程实例监控列表 + 筛选 + 分页 |
| `views/bpm/monitor/InstanceDetailDrawer.vue` | 实例详情抽屉（流程图 + 审批历史 Tabs）|
| `views/bpm/monitor/DiagramViewer.vue` | bpmn-js 流程图渲染，高亮当前节点 |
| `views/bpm/monitor/TaskHistoryTable.vue` | 审批历史表格 |
| `views/bpm/monitor/InterveneActions.vue` | 强制完成/取消操作按钮 |
| `views/bpm/monitor/StatsDashboard.vue` | ECharts 统计仪表盘（柱图+饼图）|
| `router/routes/modules/bpm.ts` | 新增 `/bpm/monitor` 和 `/bpm/stats` 路由 |

## 测试覆盖

| 类型 | 数量 |
|------|------|
| 单元测试 | ~50 |
| 集成测试 (Testcontainers MySQL) | 14 |
| **全部通过** | 199 / 199 |

## 关键设计决策

- `selectStatsByDefinition` 使用 `bpm_instance_meta.start_time/end_time` 计算耗时，不依赖 Flowable 历史表（避免测试环境问题）
- 调度任务通过 `@Scheduled(cron = "${...}")` 支持外部配置覆盖
- `HistoryCleanupMapper` 物理删除（非逻辑删除），清理完整历史
