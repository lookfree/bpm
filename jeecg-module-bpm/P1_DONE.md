# P1 验收清单 ✅

## SPI 与适配器
- [x] `bpm-spi` 子模块发布 4 接口（BpmUserContext / BpmOrgService / BpmFormService / BpmNotificationSender）+ 2 DTO（BpmFormSchema / BpmFormField），spec §3.3 全部覆盖
- [x] `bpm-adapter-jeecg` 子模块发布；`JeecgBpmUserContext` 调 jeecg `JwtUtil` 取 username 后委托 `ISysBaseAPI` 取角色与部门
- [x] 其余 3 个 SPI 接口在 adapter 中是 NoOp（P2 替换为真正实现）
- [x] `bpm-biz` 仅依赖 `bpm-spi`，依赖树扫描（`mvn dependency:tree`）无 `org.jeecgframework.boot:*` artifact
- [x] `NoOpUserContextConfiguration` `@ConditionalOnMissingBean` 在测试与非 jeecg 环境下兜底

## 数据层
- [x] `bpm_process_definition` + `bpm_process_definition_history` 两表，spec §4.1 字段全覆盖；varchar(32) UUID id；`(def_key, tenant_id, version)` 唯一索引
- [x] `BpmSchemaInitializer` `@EventListener(ApplicationReadyEvent)` + `JdbcTemplate` 跑 `db/bpm-schema-mysql.sql`，`bpm.schema.auto-init=false` 可关；Testcontainers IT 验证两表创建
- [x] MyBatis-Plus 3.5.3.1 与 jeecg 同版本接入；`MybatisPlusConfig` 注册分页插件；`BpmModuleAutoConfiguration` 加 `@MapperScan`

## API
- [x] `GET /bpm/v1/definition`（分页 + 多条件筛选）
- [x] `POST /bpm/v1/definition`（创建草稿，必填 defKey + name；提交时如带 bpmnXml 立即校验）
- [x] `GET /bpm/v1/definition/{id}`（detail；带 bpmnXml）
- [x] `PUT /bpm/v1/definition/{id}`（拒绝 ARCHIVED 更新）
- [x] `DELETE /bpm/v1/definition/{id}`（拒绝 PUBLISHED 删除）
- [x] `POST /bpm/v1/definition/{id}/publish`（DRAFT→PUBLISHED；写一份 history 快照；二次 publish 返回 409；P4 补全状态机）
- [x] `GET /bpm/v1/definition/{id}/versions`
- [x] BPMN XML 经 Flowable `BpmnXMLConverter` 校验，非法返 400

## 前端
- [x] `bpmn-js@^17` 加入 `jeecgboot-vue3` 工程依赖；归档副本在本仓库 `jeecg-module-bpm/frontend-snapshot/`
- [x] `BpmnDesigner.vue` v-model 双向绑定 BPMN XML，`destroy()` 在 `onBeforeUnmount`
- [x] 列表页 `DefinitionList.vue` 用 VxeTable + Ant Design Vue 3 + 分页
- [x] `src/api/bpm/definition.ts` 7 个方法对齐后端 7 个 endpoint
- [x] 路由 `/bpm/definition`（菜单）+ `/bpm/designer`（隐藏，由列表"设计"按钮跳）
- [x] 菜单 SQL `bpm-p1-menu.sql`（顶级 + 2 子 + 4 权限点 + admin 角色授权）

## 测试
- [x] 单测 ≥ 12 个（DTO equality / NoOp / Validator / Service / Controller MockMvc）
- [x] IT ≥ 3 个（BpmSchemaInitializerIT / DefinitionControllerPublishIT 含两条用例）；全部用 Testcontainers MySQL 8.0.27
- [x] `mvn -f jeecg-module-bpm/pom.xml clean install` 全绿

## 下一步（P2）
- 实现 `BpmOrgService` / `BpmFormService` / `BpmNotificationSender`（替换 adapter 内 NoOp）
- `bpm_node_config` 节点配置 + 6 种 AssigneeStrategy
- 表单绑定 `bpm_form_binding`
- 流程实例发起 + 审批闭环
