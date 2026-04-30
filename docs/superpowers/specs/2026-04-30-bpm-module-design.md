# 业务流程配置模块（BPM）设计文档

| 项 | 值 |
|---|---|
| 文档版本 | 0.1 (Draft) |
| 日期 | 2026-04-30 |
| 状态 | 待评审 |
| 来源需求 | `流程配置模块需求.pages` / `流程配置模块需求.docx` |
| 集成目标平台 | jeecg-boot (Spring Boot 2.7.10 / Vue 3.3.4) |
| 部署参考 | https://manage.iimt.org.cn/dashboard/analysis（"集萃智造中台"，jeecgboot-vue3 v0.1.8）|
| 后端 API 网关前缀 | `/iimt`（如 `https://manage.iimt.org.cn/iimt/sys/login`） |
| 前端框架 | jeecgboot-vue3（VBen Admin + Ant Design Vue 3.x + Vite + VxeTable） |
| 微前端 | Qiankun **已启用**（`VITE_GLOB_APP_OPEN_QIANKUN=true`），可选作为子应用接入 |
| SSO | CAS 配置已留位，当前关闭（`VITE_GLOB_APP_OPEN_SSO=false`） |

---

## 1. 目标与范围

### 1.1 模块目标
为 jeecg-boot 平台提供"所见即所得"的业务流程配置能力，让管理员/业务主管不写代码就能定义、修改、发布、停用各类审批流程，并能配套设计表单、节点人员、分支规则、版本生命周期、沙箱测试与运维监控。

### 1.2 集成原则（不破坏 jeecg 现有体系）
- **认证授权**：复用 Shiro + JWT + Shiro-Redis，不引入第二套；预留 CAS（`VITE_GLOB_APP_OPEN_SSO` 兼容）
- **API 网关前缀**：所有后端接口走 **`/iimt/bpm/v1/...`**（与现有 `/iimt/sys/...`、`/iimt/online/...` 风格一致）
- **组织架构**：节点指派直接读 `sys_user / sys_role / sys_depart / sys_user_depart / sys_user_role / sys_position`，不做镜像表
- **表单**：复用 `onl_cgform_head / onl_cgform_field`（jeecg Online Form），扩展节点级权限层而不是另建一套表单引擎
- **工作流引擎**：嵌入 Flowable 6.8，使用其 `act_*` 表，不与 jeecg 业务表混合
- **菜单/字典**：通过 jeecg 现有 `sys_permission / sys_dict` 注入；BPM 顶级菜单与"低代码开发"、"系统管理"平级
- **前端组件库**：使用 **Ant Design Vue 3.x**（与平台一致）；表格用 **VxeTable**（与现有页面一致）；图表用 **ECharts**（现有）
- **文件附件**：复用现有 OSS 与 `sys_file*` 通道；在线预览走 `fileview.jeecg.com/onlinePreview`
- **通知**：复用"消息中心"模块的 RabbitMQ + 钉钉/邮件通道
- **定时任务**：复用 jeecg 自带的 Quartz（节点超时检查、版本归档等）
- **可选微前端**：因 Qiankun 已启用，BPM 设计器（含 bpmn-js 较大的体积）可考虑作为 Qiankun 子应用按需加载，主壳零侵入

### 1.3 非目标
- ❌ 不替换 jeecg 已有的 Activiti/Online Code Generator
- ❌ 不做跨租户多版本（一期单租户）
- ❌ 不做流程的图形化运行时回放动画（仅做静态查看）
- ❌ 不做 BPMN 子流程嵌套调用外部系统（一期只支持本系统内子流程）

---

## 2. 子系统拆分与构建顺序

模块完整覆盖需求文档第三节的 7 大块，**不能一次实现**。按依赖关系拆为 6 个子项目：

| Phase | 子项目 | 依赖 | 产出可独立验证 |
|---|---|---|---|
| **P0** | 模块脚手架 + 流程引擎集成 | — | 能在 jeecg 中部署并启动 Flowable，跑 hello-world BPMN |
| **P1** | 流程定义 CRUD + BPMN 设计器（前端） | P0 | 能画图、保存、加载 BPMN，但不连人员 |
| **P2** | 节点人员调度 + 表单绑定 | P1 | 能跑通"申请→审批"两节点流程，挂表单 |
| **P3** | 分支规则 + 表达式引擎 + 会签或签 | P2 | 支持金额分支、并行会签 |
| **P4** | 版本生命周期 + 沙箱 | P3 | 草稿/测试/发布/归档；运行中实例走旧版 |
| **P5** | 运维监控 + 效率统计 | P4 | 监控页、强制干预、平均耗时统计 |

**本设计文档涵盖 P0~P5 的整体架构与关键决策**。每个 Phase 后续应有独立的 implementation plan（`docs/superpowers/plans/`）。

---

## 3. 总体架构

### 3.1 模块部署形态
作为 jeecg-boot 的一个 Maven 子模块：

```
jeecg-boot/
├── jeecg-module-system/         # 已有
├── jeecg-module-online/         # 已有 (Online Form)
├── jeecg-module-bpm/            # ★ 新增
│   ├── jeecg-module-bpm-api/    # DTO / Feign 接口
│   └── jeecg-module-bpm-biz/    # 实现 + Controller
└── jeecg-boot-module-system/    # 主启动模块（引入 bpm-biz）
```

**依赖**：`jeecg-module-bpm-biz` 依赖 `jeecg-module-system-api`、`jeecg-module-online-api`，反向不依赖。

### 3.2 分层
```
┌──────────────────────────────────────────────┐
│  Frontend (Vue 3 + bpmn-js + Online Form)    │
└─────────────────────┬────────────────────────┘
                      │ HTTP/JWT
┌─────────────────────▼────────────────────────┐
│  Controller (/bpm/v1/**) — Shiro 拦截         │
├──────────────────────────────────────────────┤
│  Service                                     │
│   ├─ DefinitionService（流程定义/版本）       │
│   ├─ InstanceService（流程实例）              │
│   ├─ TaskService（待办/已办/审批动作）        │
│   ├─ AssigneeResolver（节点人员解析）         │
│   ├─ FormBindingService（表单-节点权限）      │
│   └─ SandboxService                          │
├──────────────────────────────────────────────┤
│  Engine Layer                                │
│   ├─ Flowable ProcessEngine（BPMN 解析+执行）│
│   ├─ ExpressionEngine（条件分支求值）         │
│   └─ NotificationDispatcher（超时提醒等）     │
├──────────────────────────────────────────────┤
│  DAO (MyBatis-Plus)                          │
│   ├─ bpm_* 业务表                            │
│   └─ act_* Flowable 表（不直接 mapper）       │
└──────────────────────────────────────────────┘
```

### 3.3 关键技术选型

| 关注点 | 选型 | 备选 | 理由 |
|---|---|---|---|
| 工作流引擎 | **Flowable 6.8.0** | Activiti 7 / Camunda 7 / 自研 | BPMN 2.0 原生、Spring Boot Starter、与 Spring Boot 2.7 兼容、中文社区活跃；Activiti 已分叉、活跃度低；自研违反 YAGNI |
| BPMN 设计器（前端） | **bpmn-js 17.x**（bpmn.io） | LogicFlow / X6 | 事实标准、MIT 许可、Vue 3 包装成熟、与 Flowable 引擎规范同源 |
| 表达式引擎 | **AviatorScript 5.x** | SpEL / Groovy / Janino | 沙箱安全、性能高、中文社区文档丰富；Groovy 启动慢且需类加载隔离；SpEL 难做权限边界 |
| 表单设计器 | **复用 jeecg `onl_cgform_*`** | 自研 / form-create | 平台已有、用户已熟悉；新建会与 Online Form 形成两套，运维负担大 |
| 流程图序列化 | BPMN 2.0 XML（`act_re_procdef.RESOURCE_BYTES_`） | JSON 自定义 | 与引擎天然一致、可被任何 BPMN 工具读取 |
| 锁与去重 | **Redisson** | 数据库唯一索引 | jeecg 已引入，发布时可加分布式锁防并发 |
| 通知 | **RabbitMQ + 钉钉/邮件 channel** | 直接 HTTP | 解耦、可重试、jeecg 已配 |

---

## 4. 数据模型

所有新表前缀 `bpm_`。Flowable 自带 25 张 `act_*` 表（建表脚本由 Flowable Spring Boot Starter 启动时自动建）。

### 4.1 核心新表（一期）

| 表名 | 用途 | 关键字段 |
|---|---|---|
| `bpm_process_definition` | 流程定义元信息（与 act_re_procdef 一对一） | `id`, `key`, `name`, `category`, `version`, `state`(DRAFT/TESTING/PUBLISHED/ARCHIVED), `bpmn_xml`, `form_id`(关联 onl_cgform_head), `act_def_id`, `tenant_id`, `create_by`, `create_time` |
| `bpm_process_definition_history` | 版本快照（每次发布留底） | `id`, `def_id`, `version`, `bpmn_xml`, `change_note`, `published_by`, `published_time` |
| `bpm_node_config` | 节点附加配置（人员、表单权限、超时） | `id`, `def_id`, `node_id`(BPMN element id), `assignee_strategy`(JSON), `multi_mode`(SEQUENCE/PARALLEL/ANY), `form_perm`(JSON: 字段级读/写/必填/隐藏), `timeout_hours`, `timeout_action`(REMIND/AUTO_PASS/ESCALATE) |
| `bpm_assignee_strategy` | 人员策略词典（可复用） | `id`, `name`, `type`(USER/ROLE/DEPT_LEADER/UPPER_DEPT/FORM_FIELD/SCRIPT), `payload`(JSON) |
| `bpm_form_binding` | 流程-表单关联（一对多） | `id`, `def_id`, `form_id`, `purpose`(APPLY/APPROVE/ARCHIVE) |
| `bpm_instance_meta` | 流程实例附加信息（与 act_ru_execution 一对一） | `id`, `act_inst_id`, `def_id`, `def_version`, `business_key`, `apply_user_id`, `apply_dept_id`, `state`, `start_time`, `end_time` |
| `bpm_task_history` | 任务审批记录（含驳回/转审/加签） | `id`, `act_task_id`, `inst_id`, `node_id`, `assignee_id`, `action`(APPROVE/REJECT/TRANSFER/COUNTERSIGN), `comment`, `attachments`, `op_time` |
| `bpm_sandbox_run` | 沙箱测试运行记录 | `id`, `def_id_draft`, `runner_id`, `result`(PASS/FAIL), `log`, `start_time`, `end_time` |

### 4.2 与 jeecg 现有表的关系（只读引用，不外键）

```
bpm_node_config.assignee_strategy → sys_user / sys_role / sys_depart / sys_position
bpm_form_binding.form_id → onl_cgform_head.id
bpm_instance_meta.apply_user_id → sys_user.id
bpm_instance_meta.apply_dept_id → sys_depart.id
```

不建外键约束（与 jeecg 风格一致），通过 Service 层校验。

### 4.3 Flowable 表配置
- `flowable.database-schema-update=true`（首次启动建表，生产改 `none`）
- `flowable.history-level=full`（保留任务全量历史）
- `flowable.async-executor-activate=true`（异步任务用于超时定时器）
- 与 jeecg 共用同一个 MySQL 实例同一 schema（避免分布式事务）

---

## 5. 关键模块详细设计

### 5.1 节点人员调度（`AssigneeResolver`）

策略接口：
```java
public interface AssigneeStrategy {
    String type();  // USER / ROLE / DEPT_LEADER / UPPER_DEPT / FORM_FIELD / SCRIPT
    List<Long> resolve(ResolveContext ctx);  // 返回 user id 列表
}
```

`ResolveContext` 包含：流程实例 id、当前节点 id、申请人 id、表单数据 Map、bpmn 变量。

6 种实现：
1. `FixedUserStrategy` — 直接返回配置的 user id 列表
2. `RoleStrategy` — 从 `sys_user_role` 反查
3. `DeptLeaderStrategy` — 从申请人部门取负责人（沿 `sys_depart.parent_id` 上溯）
4. `UpperDeptStrategy` — 上一级部门负责人
5. `FormFieldStrategy` — 从表单数据中按字段名取 user id（如"项目经理"字段）
6. `ScriptStrategy` — Aviator 脚本，沙箱化执行（白名单变量、禁反射、超时 200ms）

**多人节点模式**：Flowable 多实例任务（multi-instance），通过 `bpm_node_config.multi_mode` 映射：
- `SEQUENCE` → BPMN sequential multiInstance
- `PARALLEL` → BPMN parallel multiInstance + completionCondition `nrOfCompletedInstances/nrOfInstances >= 1.0`（会签）
- `ANY` → parallel + completionCondition `nrOfCompletedInstances >= 1`（或签）

### 5.2 分支与表达式引擎

BPMN `sequenceFlow` 的 `conditionExpression` 用 Aviator 求值。引擎注入变量：
- `form.<字段>` — 表单字段
- `sys.<key>` — 系统变量（当前时间、申请人部门等）
- `user.<属性>` — 申请人属性

示例：`form.amount > 10000 && form.amount <= 50000`

**默认分支**：BPMN `default` 属性指定，无任何条件匹配时走它；无默认分支时抛 `NoMatchingFlowException` 转人工处理节点。

### 5.3 表单-节点权限

`bpm_node_config.form_perm` JSON Schema：
```json
{
  "fields": {
    "amount": {"perm": "READ_WRITE", "required": true},
    "msds_file": {"perm": "READ_ONLY"},
    "remark": {"perm": "HIDDEN"}
  },
  "extra_attachments_allowed": true
}
```

前端渲染时从 `/bpm/v1/task/{taskId}/form` 拉表单 schema + 节点权限合并后渲染。后端在提交时再次校验（防绕过）。

### 5.4 版本与生命周期

状态机：
```
DRAFT ──发布──▶ TESTING ──发布──▶ PUBLISHED ──归档──▶ ARCHIVED
                  │                    ▲
                  └────回退────────────┘
```

**版本隔离**：
- `bpm_instance_meta.def_version` 记录实例发起时的版本号
- 任务/历史查询都按 `def_version` 关联到对应版本的 BPMN XML
- 新发起实例总是走最新 `PUBLISHED` 版本

**发布加锁**：用 Redisson `RLock`（key=`bpm:def:publish:{defKey}`）防止两人同时发布同一个流程。

### 5.5 沙箱测试

沙箱与生产**共用同一引擎实例**，但通过：
- 流程定义 `category` = `SANDBOX`，正常发布的为 `PROD`
- 沙箱实例 `bpm_instance_meta.state` 标记 `SANDBOX`
- 列表查询/统计默认过滤掉 `SANDBOX`
- 沙箱审批不触发真实通知（`NotificationDispatcher` 检查 category 静默）

YAGNI：不做独立的引擎实例或独立 schema。

### 5.6 监控与运维

- **运行中实例视图**：`act_ru_execution` + `bpm_instance_meta` join，按流程定义/部门/状态过滤
- **强制干预**：管理员可调 Flowable API `RuntimeService.signalEventReceived` / `TaskService.complete`，记录到 `bpm_task_history.action='FORCE_*'`
- **效率统计**：基于 `act_hi_taskinst.duration_` 与 `act_hi_actinst.duration_`，按流程/节点 group by 算平均/超时率/驳回率
- **组织架构同步**：节点人员策略**实时解析**（不缓存解析结果），sys_user/sys_role 变动自动适配；只在分布式锁的发布瞬间快照定义

---

## 6. API 设计

经网关后路径前缀 **`/iimt/bpm/v1/`**（应用内 controller 是 `/bpm/v1/`，`/iimt` 由网关/反代加在前面）。Swagger 分组 `bpm`，Shiro 应用内路径 `/bpm/v1/**` 走 JWT 拦截。

| 模块 | Endpoint | 方法 | 说明 |
|---|---|---|---|
| 定义 | `/definition` | GET/POST | 列表/新建草稿 |
| 定义 | `/definition/{id}` | GET/PUT/DELETE | 详情/更新/删除 |
| 定义 | `/definition/{id}/publish` | POST | 发布（DRAFT→TESTING→PUBLISHED） |
| 定义 | `/definition/{id}/rollback` | POST | 回滚到指定历史版本 |
| 定义 | `/definition/{id}/versions` | GET | 历史版本列表 |
| 实例 | `/instance` | POST | 发起流程（携带表单数据） |
| 实例 | `/instance/{id}` | GET | 实例详情 |
| 实例 | `/instance/{id}/intervene` | POST | 强制干预（管理员） |
| 任务 | `/task/todo` | GET | 我的待办 |
| 任务 | `/task/done` | GET | 我的已办 |
| 任务 | `/task/{id}/complete` | POST | 完成（同意/拒绝/转审/加签） |
| 任务 | `/task/{id}/form` | GET | 节点表单 schema + 权限 |
| 表单绑定 | `/form-binding` | POST/GET/DELETE | 流程-表单绑定 CRUD |
| 沙箱 | `/sandbox/{defId}/start` | POST | 沙箱发起 |
| 沙箱 | `/sandbox/{runId}` | GET | 沙箱运行日志 |
| 监控 | `/monitor/instances` | GET | 运行中实例 |
| 监控 | `/monitor/stats` | GET | 效率统计 |

权限点：每个端点对应 `sys_permission` 一条，前缀 `bpm:` 便于按角色授权。

---

## 7. 前端架构（jeecgboot-vue3 集成）

### 7.0 集成形态选择

| 形态 | 说明 | 推荐场景 |
|---|---|---|
| **A. 主壳内置模块**（默认） | 在 `jeecgboot-vue3` 工程中新增 `src/views/bpm/`，路由通过后端动态菜单注入 | 一期推荐 — 实现最快，与现有页面风格统一 |
| **B. Qiankun 子应用** | 独立工程 `bpm-frontend`，主壳通过 `qiankun` 注册子应用 | 如果 BPMN 设计器引入 bpmn-js 17 + 自定义扩展使首屏 JS 超过 800KB，再考虑迁出；P3 后再评估 |

**默认走 A**，Phase P1 中如果设计器 bundle 过大，可在 P4 之前迁到 B（迁移代价可控因路由是动态加载的）。

### 7.1 路由层级（菜单后端注入到 `sys_permission`）
```
/bpm
├── /designer            # BPMN 流程设计器
├── /definition          # 流程定义列表/详情
├── /todo                # 我的待办
├── /done                # 我的已办
├── /instance            # 实例管理
├── /sandbox             # 沙箱
└── /monitor             # 监控
```
顶级菜单"流程配置"显示位置：与"低代码开发"、"系统管理"同级。

### 7.2 关键组件（沿用 jeecgboot-vue3 既有原子组件）
- `BpmnDesigner.vue` — 包装 `bpmn-js` 17.x；左侧元素面板用 `a-collapse`，中间画布，右侧节点属性面板用 `a-form` + 已有的 `BasicForm`
- `FormDesigner.vue` — 直接挂 jeecg `OnlCgformDesign`（来自 `jeecg-online-vendor` chunk），不重写
- `NodeAssigneePanel.vue` — 复用 jeecg 原子组件 **`JSelectUserByDept`** / **`JSelectRole`** / **`JSelectDepart`** / **`JSelectPosition`**（位于 `src/components/Form/src/jeecg/components/`）
- `TaskApprovePage.vue` — 用 `OnlCgform` 渲染表单（按节点权限） + 同意/拒绝/转审/加签按钮
- `ProcessMonitor.vue` — 实例列表用 **VxeTable**（与现有页面一致），流程图高亮当前节点（bpmn-js `viewer` 模式 + colorize overlay）
- 图表统计页用 **ECharts**（现有 vendor chunk）

### 7.3 API 调用层
- 复用 `src/utils/http/axios/` 封装；`baseURL` 取 `VITE_GLOB_API_URL` (= `/iimt`)
- 在 `src/api/` 下新增 `bpm/` 子目录，按业务对象分文件：`definition.ts`、`instance.ts`、`task.ts`、`monitor.ts` 等
- 类型定义放在 `src/api/bpm/model/` 与后端 DTO 对齐
- 错误码处理沿用现有全局 axios 拦截器

### 7.3 BPMN 扩展属性（namespace `jeecg:`)
为了让设计器存储 jeecg 特有信息，扩展 BPMN XML：
```xml
<bpmn:userTask id="approve_finance" name="财务审批"
               jeecg:assigneeStrategyId="strategy_001"
               jeecg:multiMode="PARALLEL"
               jeecg:formId="form_purchase"
               jeecg:timeoutHours="24"
               jeecg:timeoutAction="REMIND"/>
```

后端解析时按需读取这些属性写入 `bpm_node_config`。

---

## 8. 安全与权限

- **API 鉴权**：Shiro JWT 过滤器（jeecg 现有）
- **接口级权限**：注解 `@RequiresPermissions("bpm:definition:edit")` 等
- **数据级权限**：定义/实例按部门隔离（用 jeecg `DataAuthAnnotation`）
- **脚本沙箱**：Aviator 显式禁用反射、System、ProcessBuilder；CPU/时间限制 200ms
- **BPMN 上传校验**：服务端解析 BPMN XML，禁止 `<bpmn:scriptTask>` 中的非 Aviator 脚本（防注入 Groovy 等）
- **附件**：复用 jeecg OSS 上传，文件类型/大小白名单同 Online Form

---

## 9. 错误处理与幂等

| 场景 | 策略 |
|---|---|
| 发布并发 | Redisson 锁，第二个请求 409 |
| 任务重复完成 | `bpm_task_history` 唯一索引 `(act_task_id, action)`，幂等 |
| 引擎异常 | Flowable 抛 `FlowableException` → 全局 `@RestControllerAdvice` 转 `Result.error()`，详细栈写日志不返前端 |
| 节点人员解析空 | 回退到流程定义的 `fallbackAssignee`；若仍空，转管理员 |
| 表达式求值异常 | 走默认分支并日志告警；无默认分支时挂起转人工 |
| 通知投递失败 | RabbitMQ 死信队列，3 次后挂起人工 |

---

## 10. 测试策略

| 层 | 工具 | 覆盖点 |
|---|---|---|
| 单元 | JUnit 5 + Mockito | Service / Resolver / Expression |
| 集成 | SpringBootTest + H2 内存（不能 — Flowable 需 MySQL） → **Testcontainers MySQL** | Controller + Engine + DB |
| BPMN 引擎 | Flowable JUnit Rule | 流程跑通用例 |
| 前端 | Vitest + Vue Test Utils | 组件 |
| E2E | Cypress | designer → 发布 → 发起 → 审批闭环 |
| 沙箱回归 | 每次发布后自动跑预置用例集 | 用 `bpm_sandbox_run` 记录 |

---

## 11. 性能与容量目标（一期）

- 流程定义数：≤ 500
- 历史版本：≤ 5,000
- 并发实例：5,000
- 待办每用户：≤ 200
- 设计器画布元素：≤ 200 节点
- 任务完成 P95 < 300ms（不含通知投递）
- 流程发布 P95 < 1s

`act_*` 历史表配套清理任务（保留 6 个月，通过 jeecg Quartz 定时清理）。

---

## 12. 待决问题（需评审确认）

| # | 问题 | 候选 | 提议默认 |
|---|---|---|---|
| Q1 | 是否支持多租户 | 是/否 | 否（一期单租户）|
| Q2 | BPMN 设计器是否要中文化 i18n | 完全中文 / 双语 | 完全中文 |
| Q3 | 节点超时通知渠道默认 | 钉钉/邮件/系统站内 | 钉钉 + 站内（邮件可选）|
| Q4 | 沙箱与生产是否完全数据隔离 | 同库同表打 flag / 独立 schema | 同库打 flag（YAGNI）|
| Q5 | 是否支持 BPMN 外部 API 集成节点 | 是/否 | 否（一期）|
| Q6 | Flowable 表是否走独立 datasource | 是/否 | 否（与 jeecg 同库）|
| Q7 | 流程定义的 `tenant_id` 字段是否启用 | 是/否 | 启用但默认填 'default' |
| **Q8** | 前端集成形态 | A. 主壳内置 / B. Qiankun 子应用 | A（一期），P4 前根据 bundle 体积评估是否切 B |
| **Q9** | 现有部署的"低代码开发"菜单下是否已有 Activiti/工作流 | 是 → 评估迁移路径；否 → 直接接入 | 现场确认（看上去暂无 — 菜单未列出）|
| **Q10** | 是否要兼容 CAS 单点（当前禁用但已留位） | 是/否 | 兼容（不主动启用，但 Filter 链允许 CAS principal） |
| **Q11** | API 风格（restful vs jeecg 业务接口风格） | 严格 REST / jeecg 风格（POST 居多 + body 包参数）| 与 jeecg 现有 controller 风格保持一致，避免前端封装两套 |

---

## 13. 与既有 jeecg 模块的耦合点清单

| jeecg 模块/表 | 用法 | 风险 |
|---|---|---|
| `sys_user` / `sys_user_role` / `sys_user_depart` | 节点人员解析 | 低 — 只读 |
| `sys_role` | 角色策略 | 低 |
| `sys_depart` | 部门主管 / 上下级解析 | 中 — 依赖 `parent_id` 树结构正确 |
| `sys_position` | 岗位策略 | 低 |
| `onl_cgform_head` / `onl_cgform_field` | 表单元数据复用 | 中 — 字段类型支持需要梳理一遍（子表、附件、计算字段都要测） |
| `sys_dict` | 节点超时动作等下拉 | 低 |
| `sys_permission` | 模块菜单与权限点注入 | 低 |
| `sys_data_log` | 流程定义变更审计 | 低 |
| Shiro 配置 | `/bpm/v1/**` 路径放行规则 | 低 |
| Quartz | 超时检查、版本归档 | 低 |
| RabbitMQ | 通知 | 低 |
| OSS | 附件 | 低 |

---

## 14. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| Flowable 6.8 与 Spring Boot 2.7.10 兼容性 | 高 | P0 阶段先做 hello-world 验证，必要时降级到 6.6 |
| `onl_cgform` 字段类型不能完全覆盖需求（如计算字段） | 中 | 梳理表后扩展 OnlineForm 的渲染层，不动表 |
| 多人节点 + 驳回回退的 Flowable 边界 case | 中 | 用 Flowable 内置 reject pattern + 历史回滚 API；测试覆盖 |
| 发布后旧版本实例的 BPMN XML 版本一致性 | 高 | 发起时即冻结 `def_version`，所有运行时查询走 history 表 |
| Aviator 脚本性能/安全 | 中 | 全局白名单 + 200ms 超时 + 缓存编译结果 |
| 跨部门数据权限规则与 jeecg `DataAuth` 不匹配 | 中 | P5 阶段做适配层 |

---

## 15. 后续动作

1. 用户评审本设计文档（重点看第 2 节子项目拆分、第 4 节数据模型、第 12 节待决问题 Q8~Q11）
2. 评审通过后，**对 P0 单独**走 `superpowers:brainstorming → writing-plans` 产出实现 plan
3. 在新的 git worktree 上执行 P0
4. P0 完成后再依次启动 P1~P5

### 现场需要管理员协助核实的事项

- **C1**：现网 jeecgboot 的源码分支与版本号（看到的 `JeecgBootAdmin v0.1.8` 应该有对应 git tag），需要源码 access 才能精确对齐 Maven 依赖版本与目录结构
- **C2**：现网"低代码开发"菜单内是否已有任何流程相关功能（Activiti/Flowable 痕迹）；如有需要先做兼容/迁移评估
- **C3**：现有 `act_*` 表是否已存在（jeecg 标准发行版可能已带 Activiti）；若有，要决定升级到 Flowable 6.8 的迁移策略
- **C4**：现有 `iimt_*` 业务表（`iimt_business_trip_apply`、`iimt_overtime_apply` 等已有表）的审批是否目前有自定义实现 — 决定 BPM 模块上线后这些是否要切换到新引擎
- **C5**：生产 MySQL 版本与字符集（虽然需求说 8.0.27，需确认）；`act_*` 表对 InnoDB 行格式有要求

---

## 附录 A：术语表

| 术语 | 含义 |
|---|---|
| BPMN | Business Process Model and Notation 2.0 |
| 流程定义 | 一个 BPMN 模型 + 节点配置 + 表单绑定的可发布产物 |
| 流程实例 | 一次具体的审批运行 |
| 任务 | 实例在某个用户任务节点上的待办项 |
| 会签 | 全部审批通过才通过 |
| 或签 | 任意一人通过即通过 |
| 顺序签 | 按指定顺序依次审批 |
| 沙箱 | 与生产同引擎、按 category 隔离的测试运行模式 |

## 附录 B：参考链接

- jeecg-boot 文档：https://help.jeecg.com/
- jeecgboot-vue3 仓库：https://github.com/jeecgboot/jeecgboot-vue3
- VBen Admin（jeecgboot-vue3 基座）：https://vbenjs.github.io/vue-vben-admin-doc/
- Ant Design Vue 3：https://www.antdv.com/components/overview-cn
- VxeTable：https://vxetable.cn/
- Flowable 6 用户指南：https://www.flowable.com/open-source/docs/bpmn/ch01-Introduction
- bpmn-js 文档：https://bpmn.io/toolkit/bpmn-js/
- AviatorScript：https://www.yuque.com/boyan-avfmj/aviatorscript
- Qiankun 微前端：https://qiankun.umijs.org/zh

## 附录 C：现场观察的菜单清单（截至 2026-04-30）

来自实际观察 https://manage.iimt.org.cn 一级菜单，BPM 模块"流程配置"建议插在"低代码开发"之后、"系统管理"之前：

```
项目综合看板
看板 ├─ 公司级看板（全产品维度）
     ├─ 采购指标看板
     ├─ 研发采购订单进度表
     ├─ HR看板（oa审批文件上传）
     └─ 生产日常管理效率看板
企业智能门户
市场营销中心
销售订单全流程跟踪
访客管理
项目管理
物料数据
质检模块
焊接数据
人事管理
售后管理
公司官网
智能工厂
低代码开发
►【流程配置】← 新增模块的位置
系统管理
系统监控
消息中心
统计报表
组件&功能
```

现场观察的"快捷入口"业务场景（这些后续都可能用 BPM 模块）：项目立项、考勤查询、新建访客、报检申请、加工物料、资料上传。
