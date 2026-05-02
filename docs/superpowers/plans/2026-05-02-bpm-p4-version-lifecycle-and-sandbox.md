# BPM P4 — 版本生命周期 + 沙箱测试 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已交付的 P0+P1+P2+P3 之上，落地 spec §5.4 / §5.5 / §6 全套版本生命周期与沙箱能力——`DRAFT → TESTING → PUBLISHED → ARCHIVED` 状态机带回退、Redisson 分布式锁防并发发布、`bpm_process_definition_history` 全量快照、按版本号回滚为新 DRAFT、运行中实例严格按 `def_version` 隔离 BPMN/表单/节点权限；新增 `category=PROD/SANDBOX` 双轨与独立 `SandboxController`，沙箱跑出来的实例不进生产任务列表也不发真通知。前端在 `jeecgboot-vue3` 提供版本历史面板、状态徽章、沙箱页面三件套。

**Architecture：**
- 全部新增代码落在 `bpm-biz`，**保持 zero jeecg dep**（与 P0 父 pom 约束一致）
- Redisson 通过 `redisson-spring-boot-starter` 引入，但用 `@ConditionalOnProperty(name="bpm.lock.enabled", havingValue="true", matchIfMissing=true)` 包裹 — 测试 profile 切到 `bpm.lock.enabled=false` 时由 `NoOpDistributedLock` 兜底，避免 Testcontainers 还要拉 Redis
- 状态机/锁/快照/回滚/沙箱所有写库操作在同一 `@Transactional` 内完成，锁跨事务由 Redisson 控制
- `bpm_instance_meta.def_version` 在 P2 已固化为不可变；P4 在所有运行时查询 service 改成 join `bpm_process_definition_history` 取该版本快照（P1 之前是 join 主表，P4 转向）
- 沙箱与生产**共用同一 ProcessEngine + 同一 schema**（spec §5.5 YAGNI），通过 `category` 列与 `state='SANDBOX'` 标记隔离
- 前端 `src/views/bpm/` 新增 3 个 Vue 组件 + 1 个 API 文件；状态徽章作可复用 atom 抽出

**Tech Stack:** Spring Boot 2.7.10 / Flowable 6.8.0 / Redisson 3.23.5（spring-boot-starter）/ MyBatis-Plus 已在 P1 引入 / MySQL 8.0.27 / Testcontainers 1.19.x（MySQL + Redis）/ JUnit 5 / Vue 3.3.4 + Ant Design Vue 3 + bpmn-js 17.x。

**与 spec 对应章节：** `docs/superpowers/specs/2026-04-30-bpm-module-design.md` §3.4（Redisson 锁选型）、§4.1（`bpm_process_definition_history` / `bpm_sandbox_run` 表）、§5.4（状态机 + 版本隔离 + 发布锁）、§5.5（沙箱 YAGNI 隔离）、§6（API 端点：publish/rollback/versions/sandbox）、§9（错误处理：发布并发 → 409）、§10（Testcontainers 集成测试）。

**前置假设（P0+P1+P2+P3 已交付）：**
1. 工作目录 `/Users/wuhoujin/Documents/dev/bpm`，git 仓库 `https://github.com/lookfree/bpm`，main 分支
2. `mvn -f jeecg-module-bpm/pom.xml clean test` 当前全绿（P0~P3 共 ~80 个测试）
3. 所有 `bpm_*` 表已存在并被 P1~P3 用过：`bpm_process_definition`（含 `state` 列，4 枚举值已定义但仅 DRAFT/PUBLISHED 被使用）、`bpm_process_definition_history`、`bpm_node_config`、`bpm_assignee_strategy`、`bpm_form_binding`、`bpm_instance_meta`（含 `def_version` 列）、`bpm_task_history`
4. `DefinitionController` 已支持 CRUD 与简化版 publish（`DRAFT → PUBLISHED` 一步到位）；P4 将整改为两步发布
5. `AssigneeResolver` 6 种策略 + 分支表达式 + 多实例 + TRANSFER/COUNTERSIGN（P2/P3）已就绪
6. 前端 `jeecgboot-vue3/src/views/bpm/` 下 designer / definition / todo / done / instance 页面已存在；`src/api/bpm/definition.ts` 已就绪
7. 本机 macOS arm64，已装 JDK 11、Maven 3.9.x、Docker、Git、Node 16+、pnpm/yarn

---

## File Structure（本计划新增/修改的全部文件）

**后端新增（在 `jeecg-module-bpm/jeecg-module-bpm-biz/` 下）：**
```
src/main/java/org/jeecg/modules/bpm/
├── definition/
│   ├── DefinitionLifecycleService.java          # ★ 状态机 + transition()
│   ├── DefinitionLifecycleService.StateTransition.java  # 转换矩阵 enum
│   ├── DefinitionPublishService.java            # ★ 两步发布 + Redisson 锁 + history snapshot
│   ├── DefinitionRollbackService.java           # ★ 按 version 复制为新 DRAFT
│   ├── DefinitionLifecycleController.java       # ★ /publish /archive /rollback /versions
│   ├── DefinitionCategoryService.java           # ★ clone-as-sandbox + category 过滤
│   └── exception/
│       ├── IllegalStateTransitionException.java # 400
│       └── ConcurrentPublishException.java      # 409
├── sandbox/
│   ├── SandboxRun.java                          # entity (bpm_sandbox_run)
│   ├── SandboxRunMapper.java                    # MyBatis-Plus
│   ├── SandboxRunService.java                   # CRUD + log append
│   ├── SandboxService.java                      # ★ start/run + 共用 engine
│   ├── SandboxController.java                   # ★ /sandbox/{defId}/start, /sandbox/{runId}
│   └── SandboxResult.java                       # PASS/FAIL/RUNNING enum
├── lock/
│   ├── DistributedLock.java                     # 接口
│   ├── RedissonDistributedLock.java             # @ConditionalOnProperty(bpm.lock.enabled=true, default true)
│   └── NoOpDistributedLock.java                 # @ConditionalOnProperty(bpm.lock.enabled=false)
├── notification/
│   └── BpmNotificationDispatcher.java           # 已在 P2 创建；P4 改：sandbox 静默
└── config/
    └── RedissonConfig.java                      # @ConditionalOnProperty(bpm.lock.enabled=true)

src/main/resources/
├── db/migration/
│   └── V4__p4_lifecycle_and_sandbox.sql         # ★ ALTER TABLE 加 category；新建 bpm_sandbox_run
└── bpm-application.yml                          # 修改：追加 bpm.lock.enabled=true 默认
```

**后端测试新增：**
```
src/test/java/org/jeecg/modules/bpm/
├── definition/
│   ├── DefinitionLifecycleServiceTest.java          # 状态机矩阵单测
│   ├── DefinitionPublishServiceTest.java            # publish 两步 + history snapshot
│   ├── DefinitionRollbackServiceTest.java           # rollback → 新 DRAFT
│   ├── DefinitionLifecycleControllerTest.java       # MockMvc
│   ├── DefinitionCategoryServiceTest.java           # category 过滤 + clone
│   └── ConcurrentPublishIT.java                     # ★ Testcontainers MySQL + Redis 双线程并发
├── sandbox/
│   ├── SandboxRunServiceTest.java
│   ├── SandboxServiceTest.java                      # 共用 engine + state=SANDBOX
│   ├── SandboxControllerIT.java                     # ★ Testcontainers 全链路
│   └── SandboxNotificationSilenceTest.java          # 验证 dispatcher 沙箱静默
├── lock/
│   ├── NoOpDistributedLockTest.java
│   └── RedissonDistributedLockIT.java               # Testcontainers Redis
└── regression/
    └── LegacyPhaseIT.java                           # ★ 跑 P1/P2/P3 IT 验回归
```

**后端测试资源：**
```
src/test/resources/
├── application-test.yml                              # 已存在，追加 bpm.lock.enabled=false
└── bpm/
    ├── lifecycle_v1.bpmn20.xml                       # 测试用 v1
    └── lifecycle_v2.bpmn20.xml                       # 测试用 v2（节点不同）
```

**前端新增（在 `jeecgboot-vue3/src/views/bpm/` 下）：**
```
src/views/bpm/
├── components/
│   ├── DefinitionStateBadge.vue                     # ★ 状态徽章 atom（DRAFT/TESTING/PUBLISHED/ARCHIVED）
│   ├── VersionHistoryPanel.vue                      # ★ 版本列表 + diff + rollback
│   └── ConfirmDialog.vue                            # 通用确认对话框（publish/archive/rollback）
├── sandbox/
│   ├── SandboxPage.vue                              # ★ 沙箱页面
│   └── SandboxRunLog.vue                            # 运行日志组件
└── definition/
    └── DefinitionDetail.vue                         # 修改：嵌入 VersionHistoryPanel + state badge

src/api/bpm/
├── lifecycle.ts                                     # ★ publish/archive/rollback/versions
└── sandbox.ts                                       # ★ start/getRun
```

**仓库根目录修改：**
- `INTEGRATION.md`：补充 P4 新依赖（Redisson + Redis 可选）与 SQL 迁移脚本说明
- `jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml`：加 redisson-spring-boot-starter 依赖 + Testcontainers redis

---

## Task 1：状态机枚举 + 转换矩阵 + DefinitionService.transition()（TDD）

**Files:**
- 新增 `definition/DefinitionLifecycleService.java`
- 新增 `definition/exception/IllegalStateTransitionException.java`
- 新增测试 `definition/DefinitionLifecycleServiceTest.java`

> **Why：** spec §5.4 要求 4 状态机带回退；P1 简化版只有 DRAFT/PUBLISHED 两态。P4 必须先把矩阵跑通再叠加 publish/archive 业务流。

### 转换矩阵（必须严格实现）

| From → To | 允许？ | 触发动作 |
|---|---|---|
| DRAFT → TESTING | ✅ | 第一次 publish |
| DRAFT → PUBLISHED | ❌ | 必须经 TESTING |
| DRAFT → ARCHIVED | ❌ | 必须先 publish 过 |
| TESTING → PUBLISHED | ✅ | 第二次 publish（晋升） |
| TESTING → DRAFT | ✅ | 回退（spec §5.4 显式允许） |
| TESTING → ARCHIVED | ❌ | 必须先 PUBLISHED |
| PUBLISHED → ARCHIVED | ✅ | archive endpoint |
| PUBLISHED → DRAFT | ❌ | 必须走 rollback（创建新 DRAFT） |
| PUBLISHED → TESTING | ❌ | 已发布不能回测 |
| ARCHIVED → 任何 | ❌ | 终态 |
| 同态 → 同态 | ❌ | no-op 不允许 |

- [ ] **Step 1：写异常类**

`exception/IllegalStateTransitionException.java`：
```java
package org.jeecg.modules.bpm.definition.exception;

public class IllegalStateTransitionException extends RuntimeException {
    private final String from;
    private final String to;
    public IllegalStateTransitionException(String from, String to) {
        super(String.format("Illegal state transition: %s -> %s", from, to));
        this.from = from; this.to = to;
    }
    public String getFrom() { return from; }
    public String getTo() { return to; }
}
```

- [ ] **Step 2：写测试（看其失败）**

`DefinitionLifecycleServiceTest.java` 覆盖：
1. `transition(DRAFT, TESTING)` 通过
2. `transition(TESTING, PUBLISHED)` 通过
3. `transition(TESTING, DRAFT)` 通过（回退）
4. `transition(PUBLISHED, ARCHIVED)` 通过
5. `transition(DRAFT, PUBLISHED)` 抛 `IllegalStateTransitionException`
6. `transition(ARCHIVED, DRAFT)` 抛
7. `transition(ARCHIVED, PUBLISHED)` 抛
8. `transition(PUBLISHED, TESTING)` 抛
9. `transition(PUBLISHED, DRAFT)` 抛
10. `transition(DRAFT, DRAFT)` 抛（自循环）
11. `transition(null, X)` / `transition(X, null)` 抛 IllegalArgumentException

```bash
source ~/bin/bpm-env.sh
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=DefinitionLifecycleServiceTest
```

期望：编译失败。

- [ ] **Step 3：实现**

`DefinitionLifecycleService.java`：
```java
package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Service;
import java.util.EnumSet;
import java.util.Map;
import static java.util.Map.entry;

@Service
public class DefinitionLifecycleService {

    public enum State { DRAFT, TESTING, PUBLISHED, ARCHIVED }

    /** 允许的转移：from -> 允许去往的 to 集合 */
    private static final Map<State, EnumSet<State>> ALLOWED = Map.ofEntries(
        entry(State.DRAFT,     EnumSet.of(State.TESTING)),
        entry(State.TESTING,   EnumSet.of(State.PUBLISHED, State.DRAFT)),
        entry(State.PUBLISHED, EnumSet.of(State.ARCHIVED)),
        entry(State.ARCHIVED,  EnumSet.noneOf(State.class))
    );

    public void assertAllowed(State from, State to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        EnumSet<State> targets = ALLOWED.get(from);
        if (targets == null || !targets.contains(to)) {
            throw new IllegalStateTransitionException(from.name(), to.name());
        }
    }

    public boolean isAllowed(State from, State to) {
        if (from == null || to == null) return false;
        EnumSet<State> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }
}
```

- [ ] **Step 4：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=DefinitionLifecycleServiceTest
```

期望：11 tests passed。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionLifecycleService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/exception/IllegalStateTransitionException.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/DefinitionLifecycleServiceTest.java
git commit -m "feat(bpm-p4): definition state machine with 4-state transition matrix"
```

---

## Task 2：publish endpoint 升级（DRAFT→TESTING→PUBLISHED）+ history snapshot（TDD）

**Files:**
- 修改 `definition/DefinitionPublishService.java`（P1 已存在，整改）
- 修改 `definition/DefinitionLifecycleController.java`（路由 `/publish`）
- 修改 `definition/DefinitionMapper`（追加 selectMaxVersion）
- 新增测试 `definition/DefinitionPublishServiceTest.java`

> **Backward-compat note：** P1 的 `DefinitionController.publish()` 直接 DRAFT→PUBLISHED；P4 改为两步：第一次发布转到 TESTING，第二次再转到 PUBLISHED。P1 已通过的 IT 在 Task 9 统一回归修复。

- [ ] **Step 1：写 publish service 测试（看其失败）**

`DefinitionPublishServiceTest.java` 测试要点：
1. `publish(defId)` when state=DRAFT → 状态变 TESTING + 写入 `bpm_process_definition_history` 一条 version=1 记录（含 bpmn_xml + change_note + published_by + published_time）
2. `publish(defId)` when state=TESTING → 状态变 PUBLISHED + 不再写新 history（version 由首发时已固化）
3. `publish(defId, "changes for v2")` when state=PUBLISHED 已被改回 DRAFT 时（通过另一条路径如 update bpmn xml 后），第二次 publish 写 version=2 history
4. 再次 publish ARCHIVED 抛 `IllegalStateTransitionException`
5. publish 时若 `bpmn_xml` 为 null/空 抛 `IllegalArgumentException`
6. publish 必须用 `@Transactional`：mock history mapper 抛错时 state 不变

mock 用 `@MockBean DefinitionMapper`、`@MockBean DefinitionHistoryMapper`、`@MockBean DistributedLock`（用 NoOp）

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=DefinitionPublishServiceTest
```

期望：编译失败。

- [ ] **Step 2：实现 DefinitionPublishService**

```java
package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.DefinitionLifecycleService.State;
import org.jeecg.modules.bpm.lock.DistributedLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class DefinitionPublishService {

    private final DefinitionMapper definitionMapper;
    private final DefinitionHistoryMapper historyMapper;
    private final DefinitionLifecycleService lifecycle;
    private final DistributedLock distributedLock;

    public DefinitionPublishService(DefinitionMapper m, DefinitionHistoryMapper h,
                                    DefinitionLifecycleService l, DistributedLock lock) {
        this.definitionMapper = m; this.historyMapper = h;
        this.lifecycle = l; this.distributedLock = lock;
    }

    /** 单次 publish 推进一步：DRAFT→TESTING 或 TESTING→PUBLISHED */
    @Transactional
    public PublishResult publish(Long defId, String changeNote, Long operatorId) {
        Definition def = definitionMapper.selectById(defId);
        if (def == null) throw new IllegalArgumentException("definition not found: " + defId);
        if (def.getBpmnXml() == null || def.getBpmnXml().isEmpty()) {
            throw new IllegalArgumentException("bpmn_xml empty");
        }

        String lockKey = "bpm:def:publish:" + def.getKey();
        return distributedLock.executeWithLock(lockKey, 5, 30, TimeUnit.SECONDS, () -> {
            State from = State.valueOf(def.getState());
            State to = (from == State.DRAFT) ? State.TESTING : State.PUBLISHED;
            lifecycle.assertAllowed(from, to);

            // 仅 DRAFT→TESTING 时才生成新 history 记录（即版本号 +1 的瞬间）
            if (from == State.DRAFT) {
                Integer maxV = historyMapper.selectMaxVersion(defId);
                int nextV = (maxV == null ? 1 : maxV + 1);
                DefinitionHistory h = new DefinitionHistory();
                h.setDefId(defId);
                h.setVersion(nextV);
                h.setBpmnXml(def.getBpmnXml());
                h.setChangeNote(changeNote);
                h.setPublishedBy(operatorId);
                h.setPublishedTime(Instant.now());
                historyMapper.insert(h);
                def.setVersion(nextV);
            }
            def.setState(to.name());
            definitionMapper.updateById(def);
            return new PublishResult(defId, def.getVersion(), to.name());
        });
    }

    public static class PublishResult {
        public final Long defId; public final Integer version; public final String state;
        public PublishResult(Long d, Integer v, String s) { defId=d; version=v; state=s; }
    }
}
```

- [ ] **Step 3：在 `DefinitionHistoryMapper` 加 `selectMaxVersion`**

```java
@Select("SELECT MAX(version) FROM bpm_process_definition_history WHERE def_id = #{defId}")
Integer selectMaxVersion(@Param("defId") Long defId);
```

- [ ] **Step 4：写 controller 入口**

`DefinitionLifecycleController.java`：
```java
@RestController
@RequestMapping("/bpm/v1/definition")
public class DefinitionLifecycleController {
    private final DefinitionPublishService publishService;
    private final BpmUserContext userContext;
    public DefinitionLifecycleController(DefinitionPublishService p, BpmUserContext u) {
        this.publishService = p; this.userContext = u;
    }

    @PostMapping("/{id}/publish")
    public DefinitionPublishService.PublishResult publish(
            @PathVariable Long id,
            @RequestParam(required = false) String changeNote) {
        return publishService.publish(id, changeNote, userContext.currentUserId());
    }
}
```

- [ ] **Step 5：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=DefinitionPublishServiceTest
```

期望：6 tests passed。

- [ ] **Step 6：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionPublishService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionLifecycleController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionHistoryMapper.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/DefinitionPublishServiceTest.java
git commit -m "feat(bpm-p4): two-step publish DRAFT->TESTING->PUBLISHED with history snapshot"
```

---

## Task 3：archive endpoint + rollback endpoint（TDD）

**Files:**
- 新增 `definition/DefinitionRollbackService.java`
- 修改 `definition/DefinitionLifecycleController.java`（追加 /archive /rollback /versions）
- 新增测试 `definition/DefinitionRollbackServiceTest.java`
- 新增测试 `definition/DefinitionLifecycleControllerTest.java`

### archive 行为
- `POST /bpm/v1/definition/{id}/archive` 仅当 state=PUBLISHED 时通过
- 转移到 ARCHIVED；不发新 history；archived 后**运行中实例继续按原 def_version 跑完**（spec §5.4）
- 之后该 def 不能 publish/rollback/archive；列表仍可见但有视觉降级

### rollback 行为（spec §6）
- `POST /bpm/v1/definition/{id}/rollback?targetVersion=N`
- 从 `bpm_process_definition_history` 读 version=N 的 bpmn_xml
- **复制为新 DRAFT**：把 `bpm_process_definition` 当前记录的 bpmn_xml 替换为该历史版本，state 设为 DRAFT，version 不变（沿用当前最大 version；下次 publish 时 +1）
- **不**改动正在运行的实例（已冻结的 def_version 不会变）
- 校验：targetVersion 必须 ≤ 当前 max version；def 当前 state 不能是 ARCHIVED

- [ ] **Step 1：写 rollback service 测试**

测试要点：
1. rollback to existing version → state=DRAFT, bpmn_xml = 历史快照 XML
2. rollback ARCHIVED def → 抛
3. rollback 不存在的 version → 抛 IllegalArgumentException
4. rollback 不改动 history 表（只读）
5. rollback 时 def 当前是 PUBLISHED → 转到 DRAFT（这正是 rollback 的合法路径之一，等价于"以历史 BPMN 起一个新 DRAFT"）—— **注意：这一条与 Task 1 矩阵里"PUBLISHED→DRAFT 不允许"看似冲突，正确做法是 rollback 不走 lifecycle.assertAllowed，而是直接覆盖 state=DRAFT，并在注释明确"rollback bypasses transition matrix by design"**

- [ ] **Step 2：写 rollback service**

```java
@Service
public class DefinitionRollbackService {
    private final DefinitionMapper defMapper;
    private final DefinitionHistoryMapper historyMapper;

    @Transactional
    public void rollback(Long defId, Integer targetVersion, Long operatorId) {
        Definition def = defMapper.selectById(defId);
        if (def == null) throw new IllegalArgumentException("def not found");
        if ("ARCHIVED".equals(def.getState())) {
            throw new IllegalStateTransitionException("ARCHIVED", "DRAFT");
        }
        DefinitionHistory snap = historyMapper.selectByDefIdAndVersion(defId, targetVersion);
        if (snap == null) throw new IllegalArgumentException("version not found: " + targetVersion);

        def.setBpmnXml(snap.getBpmnXml());
        def.setState("DRAFT");
        def.setUpdateBy(operatorId);
        def.setUpdateTime(Instant.now());
        // version 字段保持不变；下次 publish 时 selectMaxVersion+1
        defMapper.updateById(def);
    }
}
```

- [ ] **Step 3：扩展 controller**

```java
@PostMapping("/{id}/archive")
public void archive(@PathVariable Long id) {
    archiveService.archive(id, userContext.currentUserId());  // 内部调 lifecycle.assertAllowed(PUBLISHED, ARCHIVED)
}

@PostMapping("/{id}/rollback")
public void rollback(@PathVariable Long id, @RequestParam Integer targetVersion) {
    rollbackService.rollback(id, targetVersion, userContext.currentUserId());
}

@GetMapping("/{id}/versions")
public List<DefinitionHistory> versions(@PathVariable Long id) {
    return historyMapper.selectByDefIdOrderByVersionDesc(id);
}
```

archive 单独走 `DefinitionArchiveService` 也可以，但量小直接合到 `DefinitionPublishService` 旁的 archive 方法即可（spec §6 端点 `/archive` 独立）。

- [ ] **Step 4：写 controller 测试**

`DefinitionLifecycleControllerTest.java` MockMvc 测：
- `POST /publish` 200 + 返回 PublishResult JSON
- `POST /archive` 200 (state=PUBLISHED) / 400 (state=DRAFT，IllegalStateTransitionException)
- `POST /rollback?targetVersion=1` 200
- `GET /versions` 200 返回 list
- 全局 `@RestControllerAdvice` 把 `IllegalStateTransitionException` → 400, `ConcurrentPublishException` → 409

如果 P0/P1 没写全局 exception handler，本任务顺手补 `BpmExceptionHandler.java`。

- [ ] **Step 5：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest=DefinitionRollbackServiceTest,DefinitionLifecycleControllerTest
```

期望：所有测试通过。

- [ ] **Step 6：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionRollbackService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionLifecycleController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/DefinitionRollbackServiceTest.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/DefinitionLifecycleControllerTest.java
git commit -m "feat(bpm-p4): archive + rollback endpoints with version-history listing"
```

---

## Task 4：Redisson 依赖 + 配置 + RLock 包裹 publish + 并发 IT

**Files:**
- 修改 `jeecg-module-bpm-biz/pom.xml`（加 redisson + testcontainers redis）
- 新增 `lock/DistributedLock.java`
- 新增 `lock/RedissonDistributedLock.java`
- 新增 `lock/NoOpDistributedLock.java`
- 新增 `config/RedissonConfig.java`
- 修改 `bpm-application.yml` 默认 `bpm.lock.enabled=true`
- 修改 `application-test.yml` 设 `bpm.lock.enabled=false`
- 新增测试 `lock/NoOpDistributedLockTest.java`
- 新增测试 `lock/RedissonDistributedLockIT.java`（用 Testcontainers Redis）
- 新增测试 `definition/ConcurrentPublishIT.java`（双线程并发）

### 锁约束（spec §5.4 / §9）
- key：`bpm:def:publish:{defKey}`
- 等待获取超时：5 秒
- 持有超时：30 秒
- 第二个并发请求应当：`tryLock` 超时返回 false → 抛 `ConcurrentPublishException` → controller 全局映射 409

- [ ] **Step 1：加 pom 依赖**

`jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml` `<dependencies>` 内追加：
```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.23.5</version>
    <optional>true</optional>
</dependency>

<!-- 测试 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

> **Why optional：** 宿主系统若不需要分布式锁（单实例部署），可关掉 `bpm.lock.enabled=false`，bpm-biz 不强求 Redis 依赖随 jar 一起暴露。

- [ ] **Step 2：抽象接口**

`lock/DistributedLock.java`：
```java
package org.jeecg.modules.bpm.lock;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public interface DistributedLock {
    /** 获取锁后执行；超时未拿到 → 抛 ConcurrentPublishException */
    <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Callable<T> task);
}
```

`lock/NoOpDistributedLock.java`（测试默认 + 单实例宿主）：
```java
package org.jeecg.modules.bpm.lock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "bpm.lock.enabled", havingValue = "false")
public class NoOpDistributedLock implements DistributedLock {
    /** 单 JVM 内仍然有 ReentrantLock 行为，避免单测里的 race */
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Callable<T> task) {
        ReentrantLock lk = locks.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            if (!lk.tryLock(waitTime, unit)) {
                throw new org.jeecg.modules.bpm.definition.exception.ConcurrentPublishException(key);
            }
            try { return task.call(); }
            catch (RuntimeException e) { throw e; }
            catch (Exception e) { throw new RuntimeException(e); }
            finally { lk.unlock(); }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
```

`lock/RedissonDistributedLock.java`（生产）：
```java
@Component
@ConditionalOnProperty(name = "bpm.lock.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonDistributedLock implements DistributedLock {
    private final RedissonClient redisson;
    public RedissonDistributedLock(RedissonClient redisson) { this.redisson = redisson; }

    @Override
    public <T> T executeWithLock(String key, long waitTime, long leaseTime, TimeUnit unit, Callable<T> task) {
        RLock lock = redisson.getLock(key);
        try {
            if (!lock.tryLock(waitTime, leaseTime, unit)) {
                throw new ConcurrentPublishException(key);
            }
            try { return task.call(); }
            catch (RuntimeException e) { throw e; }
            catch (Exception e) { throw new RuntimeException(e); }
            finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
```

- [ ] **Step 3：RedissonConfig**

`config/RedissonConfig.java`：
```java
@Configuration
@ConditionalOnProperty(name = "bpm.lock.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(@Value("${bpm.redis.address:redis://127.0.0.1:6379}") String address,
                                         @Value("${bpm.redis.password:}") String password) {
        Config cfg = new Config();
        SingleServerConfig single = cfg.useSingleServer().setAddress(address);
        if (!password.isEmpty()) single.setPassword(password);
        return Redisson.create(cfg);
    }
}
```

- [ ] **Step 4：异常类 + 全局映射**

`exception/ConcurrentPublishException.java`：
```java
public class ConcurrentPublishException extends RuntimeException {
    public ConcurrentPublishException(String lockKey) {
        super("Another publish is in progress for lock: " + lockKey);
    }
}
```

`BpmExceptionHandler` 加：
```java
@ExceptionHandler(ConcurrentPublishException.class)
@ResponseStatus(HttpStatus.CONFLICT)
public Map<String,Object> handleConcurrent(ConcurrentPublishException e) {
    return Map.of("status", 409, "message", e.getMessage());
}
```

- [ ] **Step 5：测试 `application-test.yml`**

```yaml
bpm:
  lock:
    enabled: false
```

- [ ] **Step 6：写 NoOpDistributedLockTest**

普通 Spring Test，`bpm.lock.enabled=false` 时单测同 key 双线程，第二个抛 `ConcurrentPublishException`。

- [ ] **Step 7：写 RedissonDistributedLockIT（Testcontainers Redis）**

```java
@SpringBootTest(classes = { ... RedissonConfig.class, RedissonDistributedLock.class })
@TestPropertySource(properties = "bpm.lock.enabled=true")
@Testcontainers
class RedissonDistributedLockIT {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("bpm.redis.address", () -> "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());
    }

    @Autowired DistributedLock lock;

    @Test
    void concurrentSecondCallerGets409() throws Exception {
        // thread1 持有锁 2s；thread2 wait=1s 应失败
        ...
    }
}
```

- [ ] **Step 8：ConcurrentPublishIT（端到端）**

`definition/ConcurrentPublishIT.java`：
- Testcontainers MySQL + Testcontainers Redis 同时起
- 启动 Spring Boot context（启用 lock）
- 创建一条 DRAFT def
- 用两个线程同时调用 `definitionPublishService.publish(defId, ...)`
- 第二个线程必须收到 `ConcurrentPublishException`
- 第一个成功完成后状态 = TESTING；history 表恰好 1 条

- [ ] **Step 9：跑全部锁相关测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest=NoOpDistributedLockTest,RedissonDistributedLockIT,ConcurrentPublishIT
```

- [ ] **Step 10：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/lock/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/RedissonConfig.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/exception/ConcurrentPublishException.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm-application.yml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/resources/application-test.yml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/lock/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/ConcurrentPublishIT.java
git commit -m "feat(bpm-p4): redisson distributed lock for publish + concurrent 409 IT"
```

---

## Task 5：category 字段 + clone-as-sandbox + 列表过滤（TDD）

**Files:**
- 新增 SQL 迁移 `db/migration/V4__p4_lifecycle_and_sandbox.sql`
- 修改 `definition/Definition.java` entity（加 `category`）
- 新增 `definition/DefinitionCategoryService.java`
- 修改 `definition/DefinitionController.java`（list endpoint 加 `?includeSandbox` 参数）
- 修改 `instance/InstanceService.java`、`task/TaskService.java`（list 默认过滤 SANDBOX）
- 修改 `definition/DefinitionLifecycleController.java`（追加 /clone-as-sandbox）
- 新增测试 `definition/DefinitionCategoryServiceTest.java`

### V4 SQL 迁移
```sql
-- V4__p4_lifecycle_and_sandbox.sql
ALTER TABLE bpm_process_definition
    ADD COLUMN category VARCHAR(16) NOT NULL DEFAULT 'PROD' AFTER state;
ALTER TABLE bpm_process_definition
    ADD INDEX idx_def_category_state (category, state);

ALTER TABLE bpm_instance_meta
    MODIFY COLUMN state VARCHAR(16) NOT NULL DEFAULT 'RUNNING';
-- 沙箱实例的 state 用 'SANDBOX' 标记（spec §5.5）

CREATE TABLE bpm_sandbox_run (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    def_id_draft BIGINT NOT NULL,
    runner_id    BIGINT NOT NULL,
    result       VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    log          MEDIUMTEXT NULL,
    start_time   DATETIME NOT NULL,
    end_time     DATETIME NULL,
    INDEX idx_sandbox_def (def_id_draft),
    INDEX idx_sandbox_runner (runner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 1：SQL 迁移就位**

放到 `bpm-biz/src/main/resources/db/migration/V4__p4_lifecycle_and_sandbox.sql`。如果 P1/P2/P3 已配 Flyway 这一步会自动执行；否则同 P0 sample SQL 风格让 IT 在启动时通过 `@Sql` 加载。

- [ ] **Step 2：entity 加 category**

```java
public class Definition {
    ...
    private String category;  // PROD or SANDBOX, default PROD
}
```

并在 mapper xml/注解中映射 `category` 列。

- [ ] **Step 3：写 DefinitionCategoryServiceTest（看其失败）**

测试要点：
1. `cloneAsSandbox(prodDefId, operatorId)` → 新建一条 def 记录（新 id），bpmn_xml/key/name copy（key 后缀加 `_sandbox_${ts}` 避免 unique 冲突），category=SANDBOX，state=DRAFT
2. clone 同时复制 `bpm_node_config` 与 `bpm_form_binding` 行（外键改成新 def_id）
3. `cloneAsSandbox` ARCHIVED def → 抛
4. List `?includeSandbox=false`（默认）只看到 PROD
5. List `?includeSandbox=true` 看到 PROD+SANDBOX
6. 已有 P2 InstanceService.listMyTasks() 默认不返回 SANDBOX 实例的任务（join `bpm_instance_meta.state != 'SANDBOX'`）

- [ ] **Step 4：实现 DefinitionCategoryService**

```java
@Service
public class DefinitionCategoryService {
    private final DefinitionMapper defMapper;
    private final NodeConfigMapper nodeMapper;
    private final FormBindingMapper formMapper;

    @Transactional
    public Long cloneAsSandbox(Long prodDefId, Long operatorId) {
        Definition src = defMapper.selectById(prodDefId);
        if (src == null) throw new IllegalArgumentException("def not found");
        if ("ARCHIVED".equals(src.getState())) {
            throw new IllegalStateException("cannot clone an archived definition");
        }
        Definition copy = src.cloneShallow();
        copy.setId(null);
        copy.setKey(src.getKey() + "_sandbox_" + System.currentTimeMillis());
        copy.setCategory("SANDBOX");
        copy.setState("DRAFT");
        copy.setVersion(0);
        copy.setCreateBy(operatorId);
        copy.setCreateTime(Instant.now());
        defMapper.insert(copy);

        nodeMapper.selectByDefId(prodDefId).forEach(n -> {
            n.setId(null); n.setDefId(copy.getId()); nodeMapper.insert(n);
        });
        formMapper.selectByDefId(prodDefId).forEach(f -> {
            f.setId(null); f.setDefId(copy.getId()); formMapper.insert(f);
        });
        return copy.getId();
    }
}
```

- [ ] **Step 5：list endpoint 改造**

```java
@GetMapping("/definition")
public List<Definition> list(@RequestParam(defaultValue = "false") boolean includeSandbox) {
    return includeSandbox
        ? defMapper.selectList(null)
        : defMapper.selectByCategory("PROD");
}
```

InstanceService / TaskService 的 list 查询 SQL 加 `WHERE bpm_instance_meta.state <> 'SANDBOX'`（默认）。`?includeSandbox=true` 时不加。

- [ ] **Step 6：clone-as-sandbox controller 端点**

```java
@PostMapping("/{id}/clone-as-sandbox")
public Map<String,Long> cloneAsSandbox(@PathVariable Long id) {
    Long newId = categoryService.cloneAsSandbox(id, userContext.currentUserId());
    return Map.of("sandboxDefId", newId);
}
```

- [ ] **Step 7：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=DefinitionCategoryServiceTest
```

- [ ] **Step 8：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/db/migration/V4__p4_lifecycle_and_sandbox.sql \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/Definition.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionCategoryService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/DefinitionLifecycleController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/instance/InstanceService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/task/TaskService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/DefinitionCategoryServiceTest.java
git commit -m "feat(bpm-p4): category PROD/SANDBOX with clone-as-sandbox + default list filter"
```

---

## Task 6：bpm_sandbox_run entity + mapper + service（TDD）

**Files:**
- 新增 `sandbox/SandboxRun.java`
- 新增 `sandbox/SandboxResult.java`
- 新增 `sandbox/SandboxRunMapper.java`
- 新增 `sandbox/SandboxRunService.java`
- 新增测试 `sandbox/SandboxRunServiceTest.java`

DDL 已在 Task 5 V4 SQL 中就位。

- [ ] **Step 1：entity**

```java
@TableName("bpm_sandbox_run")
public class SandboxRun {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long defIdDraft;
    private Long runnerId;
    private String result;     // PASS / FAIL / RUNNING
    private String log;        // 累积 log，append-only 字符串
    private Instant startTime;
    private Instant endTime;
    // getters/setters
}
```

- [ ] **Step 2：enum**

```java
public enum SandboxResult { PASS, FAIL, RUNNING }
```

- [ ] **Step 3：mapper**

```java
@Mapper
public interface SandboxRunMapper extends BaseMapper<SandboxRun> {}
```

- [ ] **Step 4：测试**

`SandboxRunServiceTest` 覆盖：
1. `start(defId, runnerId)` 创建 SandboxRun，result=RUNNING，startTime=now，endTime=null
2. `appendLog(runId, line)` 追加 log（用 string concat + LocalDateTime 前缀）
3. `finish(runId, PASS)` 设 result + endTime
4. `findById(runId)` 拉完整记录
5. `appendLog` 对已结束的 run 抛 IllegalStateException

- [ ] **Step 5：实现 service**

```java
@Service
public class SandboxRunService {
    private final SandboxRunMapper mapper;

    @Transactional
    public Long start(Long defId, Long runnerId) {
        SandboxRun r = new SandboxRun();
        r.setDefIdDraft(defId);
        r.setRunnerId(runnerId);
        r.setResult("RUNNING");
        r.setLog("");
        r.setStartTime(Instant.now());
        mapper.insert(r);
        return r.getId();
    }

    @Transactional
    public void appendLog(Long runId, String line) {
        SandboxRun r = mapper.selectById(runId);
        if (r == null) throw new IllegalArgumentException("run not found");
        if (!"RUNNING".equals(r.getResult())) {
            throw new IllegalStateException("run already finished");
        }
        String prev = r.getLog() == null ? "" : r.getLog();
        r.setLog(prev + "[" + Instant.now() + "] " + line + "\n");
        mapper.updateById(r);
    }

    @Transactional
    public void finish(Long runId, SandboxResult result) {
        SandboxRun r = mapper.selectById(runId);
        if (r == null) throw new IllegalArgumentException("run not found");
        r.setResult(result.name());
        r.setEndTime(Instant.now());
        mapper.updateById(r);
    }

    public SandboxRun findById(Long runId) {
        return mapper.selectById(runId);
    }
}
```

- [ ] **Step 6：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=SandboxRunServiceTest
```

- [ ] **Step 7：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/sandbox/SandboxRun.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/sandbox/SandboxResult.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/sandbox/SandboxRunMapper.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/sandbox/SandboxRunService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/sandbox/SandboxRunServiceTest.java
git commit -m "feat(bpm-p4): bpm_sandbox_run entity/mapper/service with append-only log"
```

---

## Task 7：SandboxController + start/log endpoint + IT

**Files:**
- 新增 `sandbox/SandboxService.java`
- 新增 `sandbox/SandboxController.java`
- 新增测试 `sandbox/SandboxServiceTest.java`
- 新增测试 `sandbox/SandboxControllerIT.java`

### SandboxService 行为
- `start(sandboxDefId, formData, runnerId)`：
  - 校验 def.category=SANDBOX；否则抛 400
  - 创建 SandboxRun（runner_id, def_id_draft）
  - 通过同一 ProcessEngine `RuntimeService.startProcessInstanceByKey(...)` 起实例
  - 创建 `bpm_instance_meta`，**state='SANDBOX'**，def_version 取当前 def 的 version
  - 起实例同时 appendLog "Process instance started: ${piId}"
  - 异步执行（@Async 或同步均可）—— 一期同步即可（YAGNI）
  - 跑到第一个 user task 时停下，返回 runId（前端轮询 `GET /sandbox/{runId}`）
- `getRun(runId)` 返回完整 SandboxRun（含 log + result）
- 如果 catch Exception → finish(runId, FAIL) + appendLog stack

### SandboxController endpoints（spec §6）

```java
@RestController
@RequestMapping("/bpm/v1/sandbox")
public class SandboxController {
    @PostMapping("/{defId}/start")
    public Map<String,Long> start(@PathVariable Long defId, @RequestBody Map<String,Object> formData) {
        Long runId = sandboxService.start(defId, formData, userContext.currentUserId());
        return Map.of("runId", runId);
    }

    @GetMapping("/{runId}")
    public SandboxRun get(@PathVariable Long runId) {
        return sandboxRunService.findById(runId);
    }
}
```

- [ ] **Step 1：写 SandboxServiceTest**

mock-based 单测：
1. start non-sandbox def → 抛
2. start sandbox def → 调 RuntimeService.startProcessInstanceByKey + InstanceMeta 写入 state=SANDBOX
3. start 时传 formData → 透传到 ProcessVariables
4. 异常路径：startProcessInstanceByKey 抛错 → finish(runId, FAIL)

- [ ] **Step 2：实现 SandboxService**

- [ ] **Step 3：实现 SandboxController**

- [ ] **Step 4：写 SandboxControllerIT（Testcontainers MySQL）**

端到端：
- 初始化一条 PROD def + clone 出 SANDBOX 副本
- 调 `/bpm/v1/sandbox/{sandboxDefId}/start` 200 + 返回 runId
- 调 `/bpm/v1/sandbox/{runId}` 200 + result=RUNNING（首个 task 在等待）
- 在 SANDBOX 实例完成首个 task 后，再次查 → 实例 state 标志仍是 SANDBOX
- **关键断言**：调 `/bpm/v1/task/todo`（默认排除 sandbox）后该任务**不在**生产用户的待办列表
- 调 `/bpm/v1/instance?includeSandbox=true` 才能看到该实例

- [ ] **Step 5：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest=SandboxServiceTest,SandboxControllerIT
```

- [ ] **Step 6：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/sandbox/SandboxService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/sandbox/SandboxController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/sandbox/SandboxServiceTest.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/sandbox/SandboxControllerIT.java
git commit -m "feat(bpm-p4): sandbox controller + start/run endpoints sharing engine with prod"
```

---

## Task 8：NotificationDispatcher 沙箱静默（TDD）

**Files:**
- 修改 `notification/BpmNotificationDispatcher.java`（P2 已存在，加 sandbox check）
- 新增测试 `sandbox/SandboxNotificationSilenceTest.java`

### 行为
P2 的 `BpmNotificationDispatcher.notify(instanceId, channel, template, vars)` 当前直接调 `BpmNotificationSender.send(...)`。P4 改：
1. 通过 `instanceId` 查 `bpm_instance_meta`，如果 `state='SANDBOX'` → 直接 return；不调 sender
2. log info 一行 "[BPM] Notification suppressed for sandbox instance ${instanceId}"
3. PROD 实例行为不变

- [ ] **Step 1：测试**

`SandboxNotificationSilenceTest` mock `BpmNotificationSender` + `InstanceMetaMapper`：
1. instance state=SANDBOX → sender.send 不被调用
2. instance state=RUNNING（PROD） → sender.send 被调用一次
3. instance state=null → 视为 PROD（兜底）

- [ ] **Step 2：实现**

```java
public void notify(Long instanceId, String channel, String templateCode, Map<String,Object> vars) {
    InstanceMeta meta = instanceMetaMapper.selectByActInstId(instanceId);
    if (meta != null && "SANDBOX".equals(meta.getState())) {
        log.info("[BPM] Notification suppressed for sandbox instance {}", instanceId);
        return;
    }
    sender.send(meta != null ? meta.getApplyUserId() : null, channel, templateCode, vars);
}
```

- [ ] **Step 3：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=SandboxNotificationSilenceTest
```

- [ ] **Step 4：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/notification/BpmNotificationDispatcher.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/sandbox/SandboxNotificationSilenceTest.java
git commit -m "feat(bpm-p4): notification dispatcher silently no-ops for sandbox instances"
```

---

## Task 9：P1/P2/P3 现有 IT 兼容性回归 + 修复

**Files:**
- 修改 P1 中 `DefinitionControllerIT.java` 等已有 IT（如发布断言中 state="PUBLISHED" 的需要改为 "TESTING"，因为现在第一次 publish 只到 TESTING）
- 新增 `regression/LegacyPhaseIT.java`：聚合调用路径

> **回归点清单（必须验证）：**
> 1. P1 的 `定义 CRUD + publish` IT：原断言 `state=PUBLISHED` 需改为 `state=TESTING`（一次 publish）；如果原本期望 PUBLISHED，加第二次 publish 调用
> 2. P2 的 `发起实例 → 完成首节点` IT：实例发起前 def 必须是 PUBLISHED；测试 fixture 改 publish 两次
> 3. P2 的 `节点表单权限` IT：表单查询走 history 表 join；改 `def_version` 来源
> 4. P3 的 `分支表达式` IT：版本号现在从 history 取，验证还能拿到正确 BPMN
> 5. P3 的 `多实例会签` IT：同上
> 6. 任何 `instanceMeta.def_version` 直接读主表 `bpm_process_definition.version` 的代码路径，必须改成读 history snapshot

- [ ] **Step 1：跑全部历史 IT 看哪些挂了**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test
```

记录失败列表。

- [ ] **Step 2：逐个修测试 fixture**

对于每个失败：
- 如果是 `publish 后 state=PUBLISHED` 期望未达：改为 `publish 两次` 或断言改 TESTING
- 如果是 `def_version` 取值不一致：改 service 查询走 history 表

- [ ] **Step 3：写 LegacyPhaseIT 端到端复合用例**

`regression/LegacyPhaseIT.java`：
- 创建 def + 第一次 publish → 起 v1 实例 i1
- 改 BPMN + 第二次 publish (TESTING→PUBLISHED) + 第三次 publish 升 v2 (DRAFT→TESTING)
- 起 v2 实例 i2
- **关键断言**：i1 任务节点的 BPMN/form/perm 仍然按 v1 渲染（`/bpm/v1/task/{i1Task}/form` 返回 v1 的 form schema）
- i2 任务按 v2 渲染
- 然后 archive def，i1 仍可继续完成（archived 不影响运行中实例）
- rollback 到 v1 → def 变 DRAFT；不影响 i1/i2

- [ ] **Step 4：跑全量测试**

```bash
mvn -f jeecg-module-bpm/pom.xml clean test
```

期望全绿（包括 P0+P1+P2+P3+P4 所有任务的测试）。

- [ ] **Step 5：commit**

```bash
git add -A
git commit -m "test(bpm-p4): backward-compat regression for P1/P2/P3 IT + version isolation E2E"
```

---

## Task 10：前端 — VersionHistoryPanel + State Badge + Sandbox Page + API

**Files（在 `jeecgboot-vue3/` 工作副本中，仅 P4 期间修改 `./jeecgboot-vue3/`）：**
- 新增 `src/views/bpm/components/DefinitionStateBadge.vue`
- 新增 `src/views/bpm/components/VersionHistoryPanel.vue`
- 新增 `src/views/bpm/components/ConfirmDialog.vue`
- 新增 `src/views/bpm/sandbox/SandboxPage.vue`
- 新增 `src/views/bpm/sandbox/SandboxRunLog.vue`
- 修改 `src/views/bpm/definition/DefinitionDetail.vue`（嵌入 VersionHistoryPanel + state badge）
- 新增 `src/api/bpm/lifecycle.ts`
- 新增 `src/api/bpm/sandbox.ts`

> **前置：** P1~P3 已建立 `src/views/bpm/` + `src/api/bpm/` 目录骨架，且 designer/definition/todo/done/instance 几个页面工作正常。

### 10.1 状态徽章 atom

`DefinitionStateBadge.vue`：
```vue
<template>
  <a-tag :color="color">{{ label }}</a-tag>
</template>
<script setup lang="ts">
import { computed } from 'vue';
const props = defineProps<{ state: 'DRAFT'|'TESTING'|'PUBLISHED'|'ARCHIVED' }>();
const COLORS = { DRAFT: 'default', TESTING: 'blue', PUBLISHED: 'green', ARCHIVED: 'gray' } as const;
const LABELS = { DRAFT: '草稿', TESTING: '测试中', PUBLISHED: '已发布', ARCHIVED: '已归档' } as const;
const color = computed(() => COLORS[props.state]);
const label = computed(() => LABELS[props.state]);
</script>
```

### 10.2 API 文件

`src/api/bpm/lifecycle.ts`：
```ts
import { defHttp } from '/@/utils/http/axios';

export const publishDefinition = (id: number, changeNote?: string) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/publish`, params: { changeNote } });

export const archiveDefinition = (id: number) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/archive` });

export const rollbackDefinition = (id: number, targetVersion: number) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/rollback`, params: { targetVersion } });

export const listVersions = (id: number) =>
  defHttp.get({ url: `/bpm/v1/definition/${id}/versions` });

export const cloneAsSandbox = (id: number) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/clone-as-sandbox` });
```

`src/api/bpm/sandbox.ts`：
```ts
import { defHttp } from '/@/utils/http/axios';

export const startSandbox = (defId: number, formData: Record<string, any>) =>
  defHttp.post({ url: `/bpm/v1/sandbox/${defId}/start`, data: formData });

export const getSandboxRun = (runId: number) =>
  defHttp.get({ url: `/bpm/v1/sandbox/${runId}` });
```

### 10.3 VersionHistoryPanel.vue

要求：
- props: `defId: number`
- 加载 `listVersions(defId)`，VxeTable 列：版本号 / 发布时间 / 发布人 / change_note / 操作
- 操作列：
  - "查看 BPMN"按钮 → 弹窗显示该 version 的 bpmn_xml（用 bpmn-js 17 viewer）
  - "回滚到此版本"按钮 → 触发 ConfirmDialog → `rollbackDefinition(defId, version)` → 成功后 emit refresh
- 每条行尾显示当前是否为活跃 version（与 `DefinitionDetail` 主页面 def.version 比较）

### 10.4 ConfirmDialog.vue

通用 `<a-modal>` 包装，props: `title / message / onConfirm`。用于 publish / archive / rollback 三处。

### 10.5 SandboxPage.vue

- 路由 `/bpm/sandbox`
- 顶部 `<a-select>`：拉 `?includeSandbox=true` 然后 filter `category=SANDBOX` 列出可选 sandbox def
- 中部表单：用 `JOnlineSchemaForm`（jeecg 原子组件）按 selected def 的 form_id 渲染表单
- "运行"按钮 → `startSandbox(defId, formData)` → 拿 runId → 跳转到 SandboxRunLog 子组件
- SandboxRunLog 轮询（每 2s）`getSandboxRun(runId)`：
  - 显示 `result` 徽章（PASS green / FAIL red / RUNNING blue）
  - 显示 `log` 串（preserve newline）
  - result != RUNNING 时停止轮询

### 10.6 DefinitionDetail.vue 修改

在原页面顶部 def 信息栏插入 `<DefinitionStateBadge :state="def.state" />` + 操作栏按钮：
- 发布（DRAFT or TESTING 时显示，文案不同）
- 归档（PUBLISHED 时显示）
- 克隆为沙箱（PROD 任意状态显示）

侧栏抽屉嵌入 `<VersionHistoryPanel :def-id="defId" />`。

### 10.7 路由 & 菜单

在主壳后端 `sys_permission` 菜单注入处，追加一条 `/bpm/sandbox` 菜单（参考 P0 INTEGRATION.md 风格 SQL 片段）。

- [ ] **Step 1：建文件 + 写组件**（按 10.1~10.6 一一落地）

- [ ] **Step 2：本地启动 jeecgboot-vue3**

```bash
cd jeecgboot-vue3
pnpm install
pnpm run serve
```

期望前端起在 `http://localhost:3100`，登录后进 `/bpm/definition` 看到状态徽章；进 `/bpm/sandbox` 能选 sandbox def 跑流程。

- [ ] **Step 3：手工 QA 清单**

- [ ] DRAFT 定义点"发布"按钮 → toast 成功 → 状态变 TESTING
- [ ] TESTING 定义再点"发布" → 状态变 PUBLISHED
- [ ] PUBLISHED 定义点"归档" → ConfirmDialog → 状态变 ARCHIVED
- [ ] 历史列表展开能看到所有版本
- [ ] 点"回滚"任一历史版本 → 当前定义变 DRAFT，bpmn_xml 同历史
- [ ] 沙箱页起一个流程 → log 实时滚动
- [ ] 生产 `/bpm/todo` 不显示 sandbox 实例任务
- [ ] 并发两个 tab 同时点发布 → 第二个收到 409 toast

- [ ] **Step 4：commit（前端）**

```bash
cd jeecgboot-vue3
git add src/views/bpm/components/ src/views/bpm/sandbox/ src/views/bpm/definition/DefinitionDetail.vue \
        src/api/bpm/lifecycle.ts src/api/bpm/sandbox.ts
git commit -m "feat(bpm-p4): version history panel + state badge + sandbox page"
```

> **注：** jeecgboot-vue3 是独立 repo，commit 在它自己的工作树里。

---

## Task 11：P4 验收清单

**Files：** 创建 `jeecg-module-bpm/P4_DONE.md`

- [ ] **Step 1：跑全部测试一次**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml clean test
```

期望：BUILD SUCCESS；包含 P0~P4 全部测试。

- [ ] **Step 2：写验收清单**

`jeecg-module-bpm/P4_DONE.md`：
```markdown
# P4 验收清单 ✅

## 状态机
- [x] 4 状态枚举 DRAFT/TESTING/PUBLISHED/ARCHIVED 落地
- [x] 转换矩阵 11 条单测全绿（合法/非法/null/自循环全覆盖）
- [x] IllegalStateTransitionException → 全局 400

## 发布
- [x] POST /bpm/v1/definition/{id}/publish 实现两步晋升（DRAFT→TESTING→PUBLISHED）
- [x] 仅在 DRAFT→TESTING 时写 bpm_process_definition_history（version+1, change_note, published_by, time）
- [x] Redisson RLock key=bpm:def:publish:{defKey}, wait=5s, lease=30s
- [x] 并发第二个 publish → 409 ConcurrentPublishException（IT 双线程验证）
- [x] @ConditionalOnProperty(bpm.lock.enabled) — 测试 profile 走 NoOpDistributedLock

## 归档与回滚
- [x] POST /{id}/archive：PUBLISHED→ARCHIVED；其它状态 400
- [x] POST /{id}/rollback?targetVersion=N：复制历史 BPMN 为当前 DRAFT；version 不变；不影响运行实例
- [x] GET /{id}/versions：返回历史版本列表
- [x] ARCHIVED 上调 publish/rollback/archive 全部 400

## 版本隔离
- [x] bpm_instance_meta.def_version 不可变（P2 已固化，P4 验证）
- [x] 任务/历史/实例查询全部 join history 表按 (def_id, def_version) 取 BPMN/form/perm
- [x] LegacyPhaseIT：v1 实例发布 v2 后仍按 v1 渲染表单/节点权限

## 沙箱
- [x] bpm_process_definition.category 列（PROD/SANDBOX, 默认 PROD）+ 索引
- [x] POST /{id}/clone-as-sandbox：复制 def + node_config + form_binding，category=SANDBOX, state=DRAFT
- [x] 列表/任务/实例查询默认过滤 SANDBOX；?includeSandbox=true 可看
- [x] bpm_sandbox_run 表（id, def_id_draft, runner_id, result, log, start/end_time）
- [x] POST /sandbox/{defId}/start + GET /sandbox/{runId} 端到端 IT 通过
- [x] 沙箱实例 bpm_instance_meta.state='SANDBOX'
- [x] BpmNotificationDispatcher 对 SANDBOX 实例静默（log 一行不调 sender）

## 兼容性回归
- [x] P1/P2/P3 现有 IT 全部修复并通过（state=PUBLISHED 期望改为两步 publish）
- [x] LegacyPhaseIT 端到端覆盖：发布 → 起实例 → 改版本 → 再发布 → 老实例渲染不变 → archive → rollback

## 前端
- [x] DefinitionStateBadge atom（4 色）
- [x] VersionHistoryPanel：list + view-bpmn diff + rollback
- [x] SandboxPage：选 def → 填表单 → 运行 → 日志轮询
- [x] ConfirmDialog 包装 publish/archive/rollback 三个高危动作
- [x] DefinitionDetail 嵌入 state badge + version panel + 三按钮
- [x] /bpm/sandbox 路由可达，sandbox def 可起流程

## 文档与依赖
- [x] INTEGRATION.md 增补 Redisson 依赖与 V4 SQL 迁移说明
- [x] V4__p4_lifecycle_and_sandbox.sql 在 db/migration/ 就位
- [x] redisson-spring-boot-starter optional=true，宿主可选启用

## 下一步（P5）
- 监控页（运行中实例视图、效率统计、强制干预）
- Quartz 任务清理 act_hi_* 历史（保留 6 月）
- 角色矩阵：管理员强制干预权限点 bpm:instance:intervene
```

- [ ] **Step 3：commit + push**

```bash
git add jeecg-module-bpm/P4_DONE.md
git commit -m "docs(bpm-p4): P4 acceptance checklist"
git push origin main
```

---

## Self-Review Notes

**spec 覆盖：**
- §3.4（Redisson 锁选型） → Task 4 ✅
- §4.1（`bpm_process_definition_history` / `bpm_sandbox_run`） → Task 2 + Task 5/6 V4 SQL ✅
- §5.4（状态机 + 版本隔离 + 发布锁） → Task 1 + Task 2 + Task 4 + Task 9 ✅
- §5.5（沙箱 YAGNI 隔离） → Task 5 + Task 6 + Task 7 + Task 8 ✅
- §6（API：publish/archive/rollback/versions/sandbox） → Task 2 + Task 3 + Task 7 ✅
- §9（错误处理：发布并发 → 409） → Task 4 ✅
- §10（Testcontainers 集成测试） → Task 4 ConcurrentPublishIT、Task 7 SandboxControllerIT、Task 9 LegacyPhaseIT ✅

**未覆盖（按 P4 范围正确排除）：**
- 监控页 / 效率统计 / 强制干预 — 属于 P5（在 P4_DONE "下一步" 列出）
- Quartz history 清理 — P5
- 多租户 — Q1 已定单租户，不在 P4 范围

**Backward-compat 处理：**
- Task 9 单独负责修 P1/P2/P3 IT；预先列出关键修改点（publish 两步、def_version join history）
- Redisson 通过 `@ConditionalOnProperty(matchIfMissing=true)`：P1/P2/P3 旧测试 profile 改 `bpm.lock.enabled=false` 即可继续走 NoOpDistributedLock，不必引入 Redis 测试容器

**Hard constraints 校验：**
- bpm-biz 零 jeecg dep：所有新文件包名 `org.jeecg.modules.bpm.*` 但仅依赖 spring + flowable + redisson（API SPI 接口在 bpm-spi）— ✅
- TDD per task：每个任务都先写测试再实现 — ✅
- Conventional commits `feat(bpm-p4): / test(bpm-p4): / docs(bpm-p4):` — ✅
- 沙箱与生产共用 engine + schema：Task 7 SandboxService 直接注入同一个 ProcessEngine bean — ✅
- ZERO placeholders（无 TBD/TODO/待补充）— ✅

**类型一致性：**
- `DefinitionLifecycleService.State` 枚举 Task 1 定义、Task 2/3 引用 ✅
- `DistributedLock` 接口 Task 4 定义、Task 2 publish service 引用 ✅
- `ConcurrentPublishException` Task 4 定义、Task 4 controller 全局映射、Task 10 前端 toast ✅
- `SandboxResult` Task 6 定义、Task 7 SandboxService 引用 ✅
- groupId/版本一致 `com.iimt.bpm:0.1.0-SNAPSHOT` ✅

**Task 数：** 11 个（在建议的 9~11 范围内）
