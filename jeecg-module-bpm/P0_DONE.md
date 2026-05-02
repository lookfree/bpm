# P0 验收清单 ✅

> 验证时间：2026-05-02 · jeecg-boot v3.5.5 实测

## 独立性

- [x] BPM 父 pom parent = `spring-boot-starter-parent 2.7.10`，不继承 `jeecg-boot-parent`
- [x] `bpm-biz` pom 依赖中无任何 `org.jeecgframework.boot:*` artifact
- [x] BPM 工程独立 `mvn install` 可发布到本地 m2（`com.iimt.bpm:jeecg-module-bpm{,-api,-biz}:0.1.0-SNAPSHOT`）

## 引擎集成

- [x] Flowable 6.8.0 在 Testcontainers MySQL 8.0.27 中启动成功
- [x] `act_*` 表（实测 39 张，含 history+identity 子引擎）自动创建
- [x] `StrongUuidGenerator` 通过单测
- [x] hello-world BPMN：deploy → start → complete 链路通过 `HelloWorldFlowIT` 验证

## 健康检查

- [x] `BpmHealthController` 单测通过
- [x] 真 jeecg-boot v3.5.5 启动后：
  - `/jeecg-boot/bpm/v1/healthz` 不带 token = **401**
  - 带 token = **200**，返回 `{"status":"UP","engine":"flowable","version":"6.8.0.0","name":"default"}`
- [x] 启动 6.5 秒，无 ERROR 级别栈

## 测试

- [x] `BpmModuleContextTest` × 2 用例 PASS
- [x] `HelloWorldFlowIT` × 1 用例 PASS（Testcontainers MySQL 8.0.27）
- [x] `mvn install` 全工程 BUILD SUCCESS（5/5 测试通过）

## 文档

- [x] `INTEGRATION.md` 包含已验证的集成片段（pom/yml/Shiro/数据源/Redis 前置条件）
- [x] `COMPATIBILITY.md` 记录工具链与版本决策
- [x] `docs/superpowers/specs/2026-04-30-bpm-module-design.md` 总体设计 spec
- [x] `docs/superpowers/plans/2026-04-30-bpm-p0-scaffold-and-engine.md` P0 12 项实施计划
- [x] `docs/superpowers/plans/2026-05-02-bpm-p1~p5-*.md` P1-P5 五份实施计划已就绪

## 已知留账

- `jeecg-module-bpm/jeecg-module-bpm-api/target/jeecg-module-bpm-api-0.1.0-SNAPSHOT.jar` 误入仓库追踪（早期 commit），需在后续 cleanup commit 中 `git rm --cached` 清理。
- 测试环境 `application-test.yml` 中 `flowable.async-executor-activate=false`（避免测试期间异步 timer 干扰）；jar 内 `bpm-application.yml` 默认 `true`，生产正常。

## 下一步（P1）

- 补 `bpm-spi` 接口（BpmUserContext / BpmOrgService / BpmFormService / BpmNotificationSender）
- 写 `bpm-adapter-jeecg` 实现，让 bpm-biz 通过 SPI 拿用户/部门数据
- 流程定义 CRUD + 前端 BPMN 设计器（bpmn-js 17.x）

详细 P1 任务清单见 `docs/superpowers/plans/2026-05-02-bpm-p1-definition-crud-and-designer.md`。
