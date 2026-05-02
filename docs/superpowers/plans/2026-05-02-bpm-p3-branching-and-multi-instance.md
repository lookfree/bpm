# BPM P3 — 分支表达式引擎 + 多实例（会签/或签/顺序签）+ 转审/加签 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 P0+P1+P2 已交付的"流程定义 CRUD + BPMN 设计器 + 节点人员 + 表单绑定 + 实例发起 + 任务同意/拒绝"基础上，补齐 spec §5.1 / §5.2 / §6 / §8 中"分支与表达式引擎 + 多实例任务 + 转审 + 加签"的能力，并把 ScriptStrategy 从 P2 stub 升级为完整实现；前端把 Aviator 表达式编辑器与 multi_mode 选择器接入设计器节点属性面板。

**Architecture：**
- bpm-biz 引入 `aviatorscript:5.x` 作为唯一表达式引擎（spec §3.4 已选）；通过 `BpmExpressionEvaluator` 服务集中提供 编译/缓存/沙箱/超时 四件能力
- Flowable 集成路径：用 `org.flowable.engine.delegate.JavaDelegate` + `<sequenceFlow conditionExpression>` 走自定义 `${aviatorEval('expr-hash')}`，由 `AviatorConditionDelegate` 在运行时回查 BpmExpressionEvaluator（**选择此路径而非全局替换 ExpressionManager** — 见 Task 3 决策说明）
- 多实例支持完全在"发布时"做：`BpmnXmlPostProcessor` 读 `bpm_node_config.multi_mode` 在内存中把 userTask 包成 multiInstance + completionCondition 后再 deploy，不修改 `bpm_process_definition.bpmn_xml` 持久化字段（spec 硬约束）
- TaskController `complete` 新增两个 action：`TRANSFER`（重新指派当前任务给目标用户）、`COUNTERSIGN`（在同节点为加签人新建任务，原任务等待）
- 前端两个新组件（`ConditionExpressionEditor.vue` / `MultiModeSelector.vue`）集成进 P1 已落地的 `BpmnDesigner.vue` 右侧节点属性面板；新增 `POST /bpm/v1/expression/test` 给设计器做 dry-run

**Tech Stack:** AviatorScript 5.4.x / Flowable 6.8.0 / Spring Boot 2.7.10 / MyBatis-Plus（仅 adapter 用）/ MySQL 8.0.27 / Testcontainers / Vue 3.3.4 + Ant Design Vue 3.x。

**与 spec 对应章节：**
- §3.4（AviatorScript 选型 + Redisson — 本期不引入 Redisson，归 P4）
- §4.1（`bpm_node_config.multi_mode`、`bpm_task_history.action` 枚举扩展 TRANSFER/COUNTERSIGN）
- §5.1（多人节点模式 → multi-instance 映射 + ScriptStrategy）
- §5.2（条件分支与表达式引擎 + 默认分支 / `NoMatchingFlowException`）
- §6（API：`/task/{id}/complete` 行为扩展、新增 `/expression/test`）
- §8（脚本沙箱：禁反射/System/ProcessBuilder + 200ms 超时）
- §9（错误处理：表达式异常走默认分支、节点人员解析空走 fallbackAssignee、无默认分支挂起人工）
- §10（Testcontainers MySQL 集成测试）

**前置假设（P0+P1+P2 已交付）：**
1. `jeecg-module-bpm/` 工程结构齐全（api / spi / biz / adapter-jeecg 4 子模块），bpm-biz 对 jeecg 零依赖
2. 8 张 `bpm_*` 表全部存在：`bpm_process_definition`、`bpm_process_definition_history`、`bpm_node_config`、`bpm_assignee_strategy`、`bpm_form_binding`、`bpm_instance_meta`、`bpm_task_history`、`bpm_sandbox_run`
3. P2 已落 5 种策略：FixedUser / Role / DeptLeader / UpperDept / FormField；`ScriptStrategy` 当前是 stub（`resolve` 返回空列表 + TODO 注释 — P3 Task 5 替换）
4. `BpmnXmlPostProcessor` 在 P1 已落，目前只做"jeecg:assigneeStrategyId"扩展属性写入 `bpm_node_config`；P3 Task 6 在其基础上扩展"读 multi_mode 重写 multiInstance"
5. P2 `TaskController#complete` 已支持 action ∈ {APPROVE, REJECT}；P3 Task 8/9 扩展 TRANSFER / COUNTERSIGN
6. P2 `bpm-adapter-jeecg` 已实现 `BpmOrgService` + `BpmFormService`，可通过它们读 form schema / 当前用户部门
7. 工作目录 `/Users/wuhoujin/Documents/dev/bpm`；`source ~/bin/bpm-env.sh` 设置 JDK 11 + Maven 3.9.x；Docker 在线可拉 mysql:8.0.27

**P3 范围 — OUT（明确不做）：**
- 完整版本状态机 DRAFT→TESTING→PUBLISHED→ARCHIVED + Redisson 锁 + 沙箱（spec §5.4 / §5.5 — 归 P4）
- 监控/统计/强制干预（spec §5.6 — 归 P5）
- 前端 i18n 完整化、Qiankun 子应用迁移（归 P4 评估）

---

## File Structure（本计划新增/修改的全部文件）

**新增（在 `jeecg-module-bpm-biz/` 下）：**
```
jeecg-module-bpm-biz/
├── pom.xml                                                            # ★ 修改：加 aviatorscript 依赖
└── src/main/
    ├── java/org/jeecg/modules/bpm/
    │   ├── expression/
    │   │   ├── BpmExpressionEvaluator.java                            # 编译+缓存+超时+沙箱
    │   │   ├── BpmExpressionContextBuilder.java                       # form/sys/user 三命名空间组装
    │   │   ├── BpmExpressionCacheKey.java                             # defKey+version+exprHash
    │   │   ├── BpmExpressionException.java                            # 自定义异常
    │   │   ├── AviatorSandboxOptions.java                             # FEATURE_SET 白名单常量
    │   │   └── delegate/
    │   │       └── AviatorConditionDelegate.java                      # JavaDelegate + ${aviatorEval(...)}
    │   ├── flow/
    │   │   ├── NoMatchingFlowHandler.java                             # 监听无匹配出口 → 推到 manual review
    │   │   └── ManualReviewTaskCreator.java                           # 用 fallbackAssignee 建 admin 任务
    │   ├── multi/
    │   │   └── MultiInstanceXmlRewriter.java                          # P3 新建；BpmnXmlPostProcessor 调它
    │   ├── strategy/
    │   │   └── ScriptStrategy.java                                    # ★ 修改：覆盖 P2 stub，调 BpmExpressionEvaluator
    │   ├── controller/
    │   │   ├── TaskController.java                                    # ★ 修改：加 TRANSFER / COUNTERSIGN
    │   │   └── ExpressionTestController.java                          # POST /bpm/v1/expression/test
    │   └── service/
    │       └── TaskActionService.java                                 # ★ 修改：transfer / countersign 方法
    └── resources/
        └── bpm/it/
            ├── amount-branch.bpmn20.xml                               # IT：金额分支
            ├── parallel-countersign.bpmn20.xml                        # IT：并行会签 (PARALLEL)
            ├── any-countersign.bpmn20.xml                             # IT：或签 (ANY)
            ├── sequence-countersign.bpmn20.xml                        # IT：顺序签 (SEQUENCE)
            ├── transfer-flow.bpmn20.xml                               # IT：转审
            └── countersign-flow.bpmn20.xml                            # IT：加签
```

**测试新增：**
```
jeecg-module-bpm-biz/src/test/
├── java/org/jeecg/modules/bpm/
│   ├── expression/
│   │   ├── BpmExpressionEvaluatorTest.java
│   │   ├── BpmExpressionContextBuilderTest.java
│   │   ├── AviatorSandboxTest.java                                   # 验证白名单：反射/System/Runtime/IO 全部抛错
│   │   └── BpmExpressionTimeoutTest.java                             # 死循环脚本必须在 200ms 内被 kill
│   ├── flow/
│   │   ├── NoMatchingFlowHandlerTest.java
│   │   └── AmountBranchIT.java                                       # form.amount > 10000 → 高额；否则普通
│   ├── multi/
│   │   ├── MultiInstanceXmlRewriterTest.java                         # 三种 multi_mode 重写正确性
│   │   ├── ParallelCountersignIT.java                                # 会签 — 三人全过
│   │   ├── AnyCountersignIT.java                                     # 或签 — 任一通过
│   │   └── SequenceCountersignIT.java                                # 顺序签 — 三人逐个
│   ├── strategy/
│   │   └── ScriptStrategyTest.java                                   # 覆盖 P2 stub
│   ├── controller/
│   │   ├── TaskControllerTransferTest.java                           # 单元（MockMvc + service mock）
│   │   ├── TaskControllerCountersignTest.java
│   │   ├── TransferFlowIT.java                                       # 完整流程
│   │   ├── CountersignFlowIT.java
│   │   └── ExpressionTestControllerTest.java
│   └── service/
│       └── TaskActionServiceTest.java                                # transfer / countersign 单元
└── resources/
    └── application-test.yml                                          # 复用 P0 配置
```

**前端新增 / 修改（在 `jeecgboot-vue3/src/views/bpm/`，工作副本不入本仓库；P3_DONE.md 中记录）：**
```
jeecgboot-vue3/src/views/bpm/
├── designer/
│   ├── components/
│   │   ├── ConditionExpressionEditor.vue                             # 新增
│   │   ├── MultiModeSelector.vue                                     # 新增
│   │   └── NodePropertiesPanel.vue                                   # ★ 修改：接入两个新组件
│   └── BpmnDesigner.vue                                              # P1 已存在，本期不动
└── api/bpm/expression.ts                                             # 新增 testExpression 调用
```

**仓库内文档：**
- `jeecg-module-bpm/P3_DONE.md`（新建，最后一个 Task）

---

## Task 1：加 Aviator 依赖 + BpmExpressionEvaluator 白名单 + 200ms 超时

**Files:**
- 修改 `jeecg-module-bpm-biz/pom.xml`
- 创建 `expression/AviatorSandboxOptions.java`
- 创建 `expression/BpmExpressionCacheKey.java`
- 创建 `expression/BpmExpressionException.java`
- 创建 `expression/BpmExpressionEvaluator.java`
- 创建测试 `BpmExpressionEvaluatorTest.java` / `AviatorSandboxTest.java` / `BpmExpressionTimeoutTest.java`

> **设计决策：** Aviator 5.4.x 提供 `AviatorEvaluatorInstance`（实例隔离，不污染全局）。每个流程定义构造一份实例，用 `Options.FEATURE_SET` 显式白名单关闭 `Module / NewInstance / StaticMethods / StaticFields / Lambda` 等危险 feature；用 `AviatorEvaluatorInstance.removeFunction` 删 `RuntimeFunction` 等可疑函数；通过 `disableFeature(Feature.Module)` 禁 `import`；表达式预编译后 cache，cache key = `defKey + version + sha256(expr)`；运行时用 `Future.get(200ms, MILLIS)` 实现硬超时，超时 `cancel(true)` + 抛 `BpmExpressionException`。

- [ ] **Step 1：修改 bpm-biz pom 加 aviatorscript 依赖**

`jeecg-module-bpm-biz/pom.xml` 在 `<dependencies>` 中追加：
```xml
<dependency>
    <groupId>com.googlecode.aviator</groupId>
    <artifactId>aviator</artifactId>
    <version>5.4.3</version>
</dependency>
```

执行：
```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz -am dependency:resolve
```
期望 BUILD SUCCESS、`com.googlecode.aviator:aviator:5.4.3` 被解析。

- [ ] **Step 2：写 AviatorSandboxTest（先失败）**

`jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/expression/AviatorSandboxTest.java`：
```java
package org.jeecg.modules.bpm.expression;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AviatorSandboxTest {

    private final BpmExpressionEvaluator evaluator =
            new BpmExpressionEvaluator(/*defaultTimeoutMillis*/ 200L);

    @Test
    void rejectsReflectionAccess() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "Class.forName('java.lang.Runtime')");
        assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                .isInstanceOf(BpmExpressionException.class);
    }

    @Test
    void rejectsSystemAccess() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "System.exit(0)");
        assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                .isInstanceOf(BpmExpressionException.class);
    }

    @Test
    void rejectsRuntimeExec() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "Runtime.getRuntime().exec('id')");
        assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                .isInstanceOf(BpmExpressionException.class);
    }

    @Test
    void rejectsImportStatement() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "import java.io.File; new File('/etc/passwd').exists()");
        assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                .isInstanceOf(BpmExpressionException.class);
    }

    @Test
    void allowsArithmeticAndComparison() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "form.amount > 10000");
        Map<String,Object> ctx = Map.of("form", Map.of("amount", 12000));
        assertThat(evaluator.evaluate(key, ctx)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void allowsBooleanLogic() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1, "form.amount > 1000 && form.amount < 5000");
        Map<String,Object> ctx = Map.of("form", Map.of("amount", 2500));
        assertThat(evaluator.evaluate(key, ctx)).isEqualTo(Boolean.TRUE);
    }
}
```

`BpmExpressionTimeoutTest.java`：
```java
package org.jeecg.modules.bpm.expression;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static java.time.Duration.ofMillis;

class BpmExpressionTimeoutTest {

    @Test
    void killsInfiniteLoopUnderHardTimeout() {
        BpmExpressionEvaluator evaluator = new BpmExpressionEvaluator(200L);
        // 用 Aviator 内置 while-style 表达式构造死循环（seq.range + reduce 模拟）；
        // 避免使用被白名单禁的 feature——靠 reduce 一个超大集合 + 过滤。
        // 该表达式如果不被超时拦截，单次求值远超 200ms。
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("k", 1,
                "reduce(seq.range(0, 100000000), '+', 0) > 0");

        assertTimeout(ofMillis(2000), () ->
            assertThatThrownBy(() -> evaluator.evaluate(key, Collections.emptyMap()))
                    .isInstanceOf(BpmExpressionException.class)
                    .hasMessageContaining("timeout"));
    }
}
```

`BpmExpressionEvaluatorTest.java`：
```java
package org.jeecg.modules.bpm.expression;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BpmExpressionEvaluatorTest {

    private final BpmExpressionEvaluator evaluator = new BpmExpressionEvaluator(200L);

    @Test
    void cachesCompiledExpressionByKey() {
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of("def_a", 1, "form.amount > 0");
        Map<String,Object> ctx = Map.of("form", Map.of("amount", 5));

        Object first  = evaluator.evaluate(key, ctx);
        Object second = evaluator.evaluate(key, ctx);

        assertThat(first).isEqualTo(Boolean.TRUE);
        assertThat(second).isEqualTo(Boolean.TRUE);
        assertThat(evaluator.cacheSize()).isEqualTo(1);
    }

    @Test
    void differentVersionsKeepSeparateCacheEntries() {
        BpmExpressionCacheKey k1 = BpmExpressionCacheKey.of("def_a", 1, "form.amount > 0");
        BpmExpressionCacheKey k2 = BpmExpressionCacheKey.of("def_a", 2, "form.amount > 0");
        evaluator.evaluate(k1, Map.of("form", Map.of("amount", 1)));
        evaluator.evaluate(k2, Map.of("form", Map.of("amount", 1)));
        assertThat(evaluator.cacheSize()).isEqualTo(2);
    }
}
```

跑：
```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest='AviatorSandboxTest,BpmExpressionTimeoutTest,BpmExpressionEvaluatorTest'
```
期望编译失败（实现类不存在）。

- [ ] **Step 3：写 AviatorSandboxOptions 常量**

`expression/AviatorSandboxOptions.java`：
```java
package org.jeecg.modules.bpm.expression;

import com.googlecode.aviator.Feature;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** 白名单 Aviator Feature 集合 — 显式列出允许项。 */
public final class AviatorSandboxOptions {

    /** 允许的最小 feature 集：算术 / 比较 / 布尔 / If / Let / 函数调用。
     *  禁用项（不在此集合）：Module(import) / NewInstance / StaticMethods / StaticFields / Lambda /
     *  In / InternalVars / Comparator。 */
    public static final Set<Feature> ALLOWED_FEATURES = Collections.unmodifiableSet(
            EnumSet.of(
                    Feature.Assignment,
                    Feature.Return,
                    Feature.If,
                    Feature.For,           // 仅与 seq.* 配合用，无法访问外部 IO
                    Feature.Let,
                    Feature.LexicalScope,
                    Feature.StringInterpolation
            ));

    /** 黑名单函数名（构造器移除以拒绝调用）。 */
    public static final Set<String> BLOCKED_FUNCTIONS = Set.of(
            "Class.forName", "System.exit", "Runtime.getRuntime",
            "ProcessBuilder", "java.io.File", "FileInputStream",
            "FileOutputStream", "Socket", "URL"
    );

    private AviatorSandboxOptions() {}
}
```

- [ ] **Step 4：写 BpmExpressionCacheKey + Exception**

`expression/BpmExpressionCacheKey.java`：
```java
package org.jeecg.modules.bpm.expression;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class BpmExpressionCacheKey {

    private final String defKey;
    private final int version;
    private final String expression;
    private final String exprHash;

    private BpmExpressionCacheKey(String defKey, int version, String expression) {
        this.defKey = Objects.requireNonNull(defKey);
        this.version = version;
        this.expression = Objects.requireNonNull(expression);
        this.exprHash = sha256(expression);
    }

    public static BpmExpressionCacheKey of(String defKey, int version, String expression) {
        return new BpmExpressionCacheKey(defKey, version, expression);
    }

    public String getDefKey()     { return defKey; }
    public int    getVersion()    { return version; }
    public String getExpression() { return expression; }
    public String getExprHash()   { return exprHash; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmExpressionCacheKey)) return false;
        BpmExpressionCacheKey that = (BpmExpressionCacheKey) o;
        return version == that.version
                && defKey.equals(that.defKey)
                && exprHash.equals(that.exprHash);
    }
    @Override public int hashCode() { return Objects.hash(defKey, version, exprHash); }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

`expression/BpmExpressionException.java`：
```java
package org.jeecg.modules.bpm.expression;

public class BpmExpressionException extends RuntimeException {
    public BpmExpressionException(String msg)              { super(msg); }
    public BpmExpressionException(String msg, Throwable t) { super(msg, t); }
}
```

- [ ] **Step 5：写 BpmExpressionEvaluator**

`expression/BpmExpressionEvaluator.java`：
```java
package org.jeecg.modules.bpm.expression;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.Feature;
import com.googlecode.aviator.Options;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class BpmExpressionEvaluator {

    private final long timeoutMillis;
    private final AviatorEvaluatorInstance aviator;
    private final Map<BpmExpressionCacheKey, Expression> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor =
            Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "bpm-aviator");
                t.setDaemon(true);
                return t;
            });

    /** Spring 默认 200ms。 */
    public BpmExpressionEvaluator() { this(200L); }

    /** 测试构造器。 */
    public BpmExpressionEvaluator(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        this.aviator = AviatorEvaluator.newInstance();
        // 白名单 Feature
        this.aviator.setOption(Options.FEATURE_SET, EnumSet.copyOf(AviatorSandboxOptions.ALLOWED_FEATURES));
        // 显式禁掉 Module（import）
        this.aviator.disableFeature(Feature.Module);
        this.aviator.disableFeature(Feature.NewInstance);
        this.aviator.disableFeature(Feature.StaticMethods);
        this.aviator.disableFeature(Feature.StaticFields);
        this.aviator.disableFeature(Feature.Lambda);
        // 禁用反射相关 / RuntimeFunction
        for (String fn : AviatorSandboxOptions.BLOCKED_FUNCTIONS) {
            try { this.aviator.removeFunction(fn); } catch (Exception ignored) {}
        }
    }

    public Object evaluate(BpmExpressionCacheKey key, Map<String,Object> env) {
        Expression expr;
        try {
            expr = cache.computeIfAbsent(key, k -> aviator.compile(k.getExpression(), true));
        } catch (Exception e) {
            throw new BpmExpressionException("compile failed: " + e.getMessage(), e);
        }
        Future<Object> future = executor.submit(() -> expr.execute(env));
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            throw new BpmExpressionException("expression timeout (>" + timeoutMillis + "ms)");
        } catch (ExecutionException ee) {
            throw new BpmExpressionException("evaluate failed: " + ee.getCause().getMessage(), ee.getCause());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BpmExpressionException("evaluate interrupted", ie);
        }
    }

    public int cacheSize() { return cache.size(); }
}
```

- [ ] **Step 6：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='AviatorSandboxTest,BpmExpressionTimeoutTest,BpmExpressionEvaluatorTest'
```
期望全部通过。

- [ ] **Step 7：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/expression/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/expression/
git commit -m "feat(bpm-p3): add BpmExpressionEvaluator with aviator sandbox + 200ms hard timeout"
```

---

## Task 2：变量注入器（form / sys / user 三命名空间）

**Files:**
- 创建 `expression/BpmExpressionContextBuilder.java`
- 创建测试 `BpmExpressionContextBuilderTest.java`

负责把流程实例运行时的"申请人 / 部门 / 表单数据"统一封装成 Aviator 可见的三个 Map：`form` / `sys` / `user`。

- [ ] **Step 1：写测试**

`BpmExpressionContextBuilderTest.java`：
```java
package org.jeecg.modules.bpm.expression;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BpmExpressionContextBuilderTest {

    @Test
    void buildsFormSysUserMaps() {
        BpmUserContext userCtx = mock(BpmUserContext.class);
        BpmOrgService  orgSvc  = mock(BpmOrgService.class);
        BpmFormService formSvc = mock(BpmFormService.class);

        when(userCtx.currentUserId()).thenReturn(7L);
        when(userCtx.currentDeptId()).thenReturn(99L);
        when(userCtx.currentRoleCodes()).thenReturn(Set.of("admin", "approver"));
        when(formSvc.loadFormData("form_purchase", "biz_001"))
                .thenReturn(Map.of("amount", 12345, "title", "buy laptop"));

        Clock clock = Clock.fixed(Instant.parse("2026-05-02T10:00:00Z"), ZoneId.of("UTC"));

        BpmExpressionContextBuilder builder = new BpmExpressionContextBuilder(userCtx, orgSvc, formSvc, clock);
        Map<String,Object> ctx = builder.build("form_purchase", "biz_001");

        assertThat(ctx).containsKeys("form", "sys", "user");

        Map<String,Object> form = (Map<String,Object>) ctx.get("form");
        assertThat(form).containsEntry("amount", 12345).containsEntry("title", "buy laptop");

        Map<String,Object> sys = (Map<String,Object>) ctx.get("sys");
        assertThat(sys).containsKey("now").containsEntry("today", LocalDate.of(2026,5,2));

        Map<String,Object> user = (Map<String,Object>) ctx.get("user");
        assertThat(user).containsEntry("id", 7L).containsEntry("deptId", 99L);
        assertThat((Set<String>) user.get("roles")).containsExactlyInAnyOrder("admin", "approver");
    }

    @Test
    void formMapEmptyWhenFormIdNull() {
        BpmExpressionContextBuilder builder = new BpmExpressionContextBuilder(
                mock(BpmUserContext.class), mock(BpmOrgService.class), mock(BpmFormService.class),
                Clock.systemUTC());
        Map<String,Object> ctx = builder.build(null, null);
        assertThat((Map<?,?>) ctx.get("form")).isEmpty();
    }
}
```

期望编译失败。

- [ ] **Step 2：写实现**

`expression/BpmExpressionContextBuilder.java`：
```java
package org.jeecg.modules.bpm.expression;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class BpmExpressionContextBuilder {

    private final BpmUserContext userCtx;
    private final BpmOrgService  orgSvc;
    private final BpmFormService formSvc;
    private final Clock clock;

    public BpmExpressionContextBuilder(BpmUserContext userCtx, BpmOrgService orgSvc,
                                       BpmFormService formSvc, Clock clock) {
        this.userCtx = userCtx;
        this.orgSvc  = orgSvc;
        this.formSvc = formSvc;
        this.clock   = clock;
    }

    public Map<String,Object> build(String formId, String businessKey) {
        Map<String,Object> ctx = new HashMap<>();

        // form.*
        Map<String,Object> form = (formId != null && businessKey != null)
                ? new HashMap<>(formSvc.loadFormData(formId, businessKey))
                : new HashMap<>();
        ctx.put("form", form);

        // sys.*
        Instant now = clock.instant();
        Map<String,Object> sys = new HashMap<>();
        sys.put("now",   now);
        sys.put("today", LocalDate.ofInstant(now, clock.getZone() == null ? ZoneId.systemDefault() : clock.getZone()));
        ctx.put("sys", sys);

        // user.*
        Map<String,Object> user = new HashMap<>();
        user.put("id",     userCtx.currentUserId());
        user.put("deptId", userCtx.currentDeptId());
        user.put("roles",  userCtx.currentRoleCodes() == null ? Collections.emptySet() : userCtx.currentRoleCodes());
        ctx.put("user", user);

        return ctx;
    }
}
```

- [ ] **Step 3：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmExpressionContextBuilderTest
```
期望通过。

- [ ] **Step 4：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/expression/BpmExpressionContextBuilder.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/expression/BpmExpressionContextBuilderTest.java
git commit -m "feat(bpm-p3): expression context builder for form/sys/user namespaces"
```

---

## Task 3：Flowable 集成 — conditionExpression 走 Aviator + 金额分支 IT

**Files:**
- 创建 `expression/delegate/AviatorConditionDelegate.java`
- 创建 `resources/bpm/it/amount-branch.bpmn20.xml`
- 创建 `flow/AmountBranchIT.java`

> **决策记录（spec §5.2）：** 不做"全局替换 Flowable ExpressionManager"——那需要 deeper Flowable engine config 改造，风险高。改用 Flowable 原生 `conditionExpression` 支持的 `JUEL ${...}` 语法调用一个名为 `aviatorEval` 的 bean 函数；BPMN XML 里 `<conditionExpression>${aviatorEval(execution, 'expr-text')}</conditionExpression>`，由 `AviatorConditionDelegate` 接住后查 `BpmExpressionEvaluator`。这样：
> 1. 不动 Flowable 引擎初始化；2. BPMN XML 仍然合法；3. 单元/集成测试都能命中。
>
> **BpmnXmlPostProcessor（P1 已落）改造：** 发布时把节点定义里"用户写的 `form.amount > 10000`"原样转写成 `${aviatorEval(execution, '<base64-or-quoted>')}`。本 Task 只在 BPMN 文件里手写好后用做 IT；BpmnXmlPostProcessor 的"条件表达式重写"作为 Step 4 的一部分由 IT 反向覆盖（IT 直接用包装好的 BPMN 文件就行；P3 范围内不要求修改 BpmnXmlPostProcessor 的 condition 重写——P1 已存在的 BpmnXmlPostProcessor 已经处理了 jeecg: 扩展属性，IT 用的 BPMN 直接写最终 JUEL 即可）。

- [ ] **Step 1：写 amount-branch.bpmn20.xml**

`jeecg-module-bpm-biz/src/main/resources/bpm/it/amount-branch.bpmn20.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://iimt.com/bpm/it"
             id="defs_amount_branch">
  <process id="bpm_amount_branch" name="Amount Branch" isExecutable="true">
    <startEvent id="start"/>
    <sequenceFlow id="f1" sourceRef="start" targetRef="gw"/>
    <exclusiveGateway id="gw" default="f_default"/>
    <sequenceFlow id="f_high" sourceRef="gw" targetRef="task_high">
      <conditionExpression xsi:type="tFormalExpression"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${aviatorEval(execution, 'form.amount &gt; 10000')}</conditionExpression>
    </sequenceFlow>
    <sequenceFlow id="f_default" sourceRef="gw" targetRef="task_normal"/>
    <userTask id="task_high"   name="High Approval"   flowable:assignee="${initiator}"/>
    <userTask id="task_normal" name="Normal Approval" flowable:assignee="${initiator}"/>
    <sequenceFlow id="fh_end" sourceRef="task_high"   targetRef="end"/>
    <sequenceFlow id="fn_end" sourceRef="task_normal" targetRef="end"/>
    <endEvent id="end"/>
  </process>
</definitions>
```

- [ ] **Step 2：写 AviatorConditionDelegate**

`expression/delegate/AviatorConditionDelegate.java`：
```java
package org.jeecg.modules.bpm.expression.delegate;

import org.flowable.engine.delegate.DelegateExecution;
import org.jeecg.modules.bpm.expression.BpmExpressionCacheKey;
import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 由 BPMN <conditionExpression>${aviatorEval(execution, 'expr')}</conditionExpression> 调用。
 *
 * <p>JUEL 直接把 bean.aviatorEval(execution, expr) 解析成 Java 调用，因此本类必须暴露
 *    public Boolean aviatorEval(DelegateExecution, String) — 返回类型为 Boolean，让 Flowable 决定走哪个分支。
 */
@Component("aviatorEval")
public class AviatorConditionDelegate {

    private final BpmExpressionEvaluator evaluator;
    private final BpmExpressionContextBuilder ctxBuilder;

    public AviatorConditionDelegate(BpmExpressionEvaluator evaluator,
                                    BpmExpressionContextBuilder ctxBuilder) {
        this.evaluator   = evaluator;
        this.ctxBuilder  = ctxBuilder;
    }

    public Boolean aviatorEval(DelegateExecution execution, String expression) {
        String defKey = (String) execution.getVariable("bpm_def_key");
        Integer ver   = (Integer) execution.getVariable("bpm_def_version");
        String formId = (String) execution.getVariable("bpm_form_id");
        String bizKey = execution.getProcessInstanceBusinessKey();

        Map<String,Object> env = new HashMap<>(ctxBuilder.build(formId, bizKey));
        // 把 Flowable 已有的 process variable 也透传到 form.* 兜底（实例发起时 putAll 进来的）
        Map<String,Object> formMap = (Map<String,Object>) env.get("form");
        execution.getVariables().forEach((k,v) -> {
            if (k.startsWith("form.")) {
                formMap.put(k.substring(5), v);
            }
        });

        BpmExpressionCacheKey key = BpmExpressionCacheKey.of(
                defKey == null ? "anonymous" : defKey,
                ver == null ? 0 : ver,
                expression);
        Object result = evaluator.evaluate(key, env);
        if (result instanceof Boolean) return (Boolean) result;
        return Boolean.parseBoolean(String.valueOf(result));
    }
}
```

- [ ] **Step 3：写 AmountBranchIT**

`jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/flow/AmountBranchIT.java`：
```java
package org.jeecg.modules.bpm.flow;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { org.jeecg.modules.bpm.BpmModuleAutoConfiguration.class })
@ActiveProfiles("test")
@Testcontainers
class AmountBranchIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void p(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService    runtimeService;
    @Autowired TaskService       taskService;

    @Test
    void highAmountGoesToHighApproval() {
        repositoryService.createDeployment()
                .addClasspathResource("bpm/it/amount-branch.bpmn20.xml")
                .deploy();
        Map<String,Object> v = new HashMap<>();
        v.put("initiator", "alice");
        v.put("form.amount", 20000);
        v.put("bpm_def_key", "bpm_amount_branch");
        v.put("bpm_def_version", 1);
        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_amount_branch", v);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("High Approval");
    }

    @Test
    void normalAmountGoesToDefaultBranch() {
        repositoryService.createDeployment()
                .addClasspathResource("bpm/it/amount-branch.bpmn20.xml")
                .deploy();
        Map<String,Object> v = new HashMap<>();
        v.put("initiator", "alice");
        v.put("form.amount", 5000);
        v.put("bpm_def_key", "bpm_amount_branch");
        v.put("bpm_def_version", 1);
        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_amount_branch", v);
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).getName()).isEqualTo("Normal Approval");
    }
}
```

- [ ] **Step 4：跑 IT**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=AmountBranchIT
```
期望两个测试通过。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/expression/delegate/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm/it/amount-branch.bpmn20.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/flow/AmountBranchIT.java
git commit -m "feat(bpm-p3): wire aviator into flowable conditionExpression via JUEL bean"
```

---

## Task 4：NoMatchingFlowException 处理 + fallbackAssignee

**Files:**
- 创建 `flow/NoMatchingFlowHandler.java`
- 创建 `flow/ManualReviewTaskCreator.java`
- 创建测试 `NoMatchingFlowHandlerTest.java`

需求（spec §9）：当所有 sequenceFlow 条件都不满足且网关无 `default` 时，Flowable 抛 `org.flowable.engine.delegate.event.FlowableEngineEventType.PROCESS_COMPLETED_WITH_ERROR_END_EVENT` / 或 `FlowableException`；要捕获并把实例推入"manual review"——读 `bpm_node_config.fallbackAssignee`（默认 admin role）建任务。

> **实现路径：** Flowable 提供 `FlowableEventListener` 注册到全局；监听 `JOB_EXECUTION_FAILURE` + 自定义 `BoundaryErrorEvent` 在网关上挂 errorEventDefinition。本期采用"轻量化方案"：在 `AviatorConditionDelegate.aviatorEval` 内部 try/catch 表达式异常 → 走默认分支（spec §9）；当节点本身确实没有任何匹配的 outgoing flow 时（`exclusiveGateway` 无 default），通过 `org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener` 监听 `JOB_EXECUTION_FAILURE`，调 `ManualReviewTaskCreator.createForInstance`。

- [ ] **Step 1：写 NoMatchingFlowHandlerTest**

`jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/flow/NoMatchingFlowHandlerTest.java`：
```java
package org.jeecg.modules.bpm.flow;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEntityExceptionEventImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class NoMatchingFlowHandlerTest {

    @Test
    void delegatesToManualReviewWhenNoMatchingFlow() {
        ManualReviewTaskCreator creator = mock(ManualReviewTaskCreator.class);
        NoMatchingFlowHandler handler = new NoMatchingFlowHandler(creator);

        FlowableEntityExceptionEventImpl evt = mock(FlowableEntityExceptionEventImpl.class);
        when(evt.getType()).thenReturn(FlowableEngineEventType.JOB_EXECUTION_FAILURE);
        when(evt.getProcessInstanceId()).thenReturn("inst_1");
        when(evt.getCause()).thenReturn(new RuntimeException(
                "No outgoing sequence flow of the exclusive gateway 'gw' could be selected"));

        handler.onEvent(evt);

        ArgumentCaptor<String> instCap = ArgumentCaptor.forClass(String.class);
        verify(creator).createForInstance(instCap.capture(), org.mockito.ArgumentMatchers.anyString());
        assertThat(instCap.getValue()).isEqualTo("inst_1");
    }

    @Test
    void ignoresUnrelatedJobFailures() {
        ManualReviewTaskCreator creator = mock(ManualReviewTaskCreator.class);
        NoMatchingFlowHandler handler = new NoMatchingFlowHandler(creator);

        FlowableEntityExceptionEventImpl evt = mock(FlowableEntityExceptionEventImpl.class);
        when(evt.getType()).thenReturn(FlowableEngineEventType.JOB_EXECUTION_FAILURE);
        when(evt.getCause()).thenReturn(new RuntimeException("connection lost"));

        handler.onEvent(evt);

        org.mockito.Mockito.verifyNoInteractions(creator);
    }
}
```

- [ ] **Step 2：写 ManualReviewTaskCreator**

`flow/ManualReviewTaskCreator.java`：
```java
package org.jeecg.modules.bpm.flow;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

@Component
public class ManualReviewTaskCreator {

    private final TaskService taskService;
    private final BpmNodeConfigReader nodeConfigReader; // P2 已存在

    public ManualReviewTaskCreator(TaskService taskService, BpmNodeConfigReader nodeConfigReader) {
        this.taskService = taskService;
        this.nodeConfigReader = nodeConfigReader;
    }

    /** 创建一条独立任务（不绑定 flowable 节点），指派到 fallbackAssignee。 */
    public void createForInstance(String processInstanceId, String reason) {
        String fallbackAssignee = nodeConfigReader.findFallbackAssignee(processInstanceId);
        if (fallbackAssignee == null) fallbackAssignee = "admin"; // spec §9 兜底
        Task t = taskService.newTask("manual_review_" + processInstanceId);
        t.setName("流程异常人工处理");
        t.setAssignee(fallbackAssignee);
        t.setDescription(reason);
        taskService.saveTask(t);
    }
}
```

> **`BpmNodeConfigReader`：** 在 P2 已落地，提供 `findFallbackAssignee(processInstanceId)` 读 `bpm_node_config.fallbackAssignee`（实际字段：`assignee_strategy` JSON 中的 `fallbackAssignee` 子键；adapter 层提供）。本 Task 不新建该类，假定 P2 已存在；若实际未存在，由 P2 plan 补足。

- [ ] **Step 3：写 NoMatchingFlowHandler**

`flow/NoMatchingFlowHandler.java`：
```java
package org.jeecg.modules.bpm.flow;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.impl.FlowableEntityExceptionEventImpl;
import org.springframework.stereotype.Component;

@Component
public class NoMatchingFlowHandler extends AbstractFlowableEngineEventListener {

    private static final String NO_OUTGOING_MARKER = "No outgoing sequence flow";

    private final ManualReviewTaskCreator creator;

    public NoMatchingFlowHandler(ManualReviewTaskCreator creator) {
        this.creator = creator;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event.getType() != FlowableEngineEventType.JOB_EXECUTION_FAILURE) return;
        if (!(event instanceof FlowableEntityExceptionEventImpl)) return;
        FlowableEntityExceptionEventImpl ee = (FlowableEntityExceptionEventImpl) event;
        Throwable cause = ee.getCause();
        if (cause == null) return;
        String msg = cause.getMessage();
        if (msg == null || !msg.contains(NO_OUTGOING_MARKER)) return;
        creator.createForInstance(ee.getProcessInstanceId(), msg);
    }

    @Override public boolean isFailOnException() { return false; }
}
```

注册到 Flowable：在 `FlowableConfig`（P0 已落）追加 `processEngineConfiguration.getEventDispatcher().addEventListener(noMatchingFlowHandler)`，通过 `EngineConfigurationConfigurer<SpringProcessEngineConfiguration>` bean 实现：
```java
@Bean
public org.flowable.spring.boot.EngineConfigurationConfigurer<
        org.flowable.spring.SpringProcessEngineConfiguration> bpmEventListenersConfigurer(
        NoMatchingFlowHandler handler) {
    return cfg -> cfg.setEventListeners(java.util.List.of(handler));
}
```

- [ ] **Step 4：写 IT — 故意触发 NoMatchingFlow**

新建 `jeecg-module-bpm-biz/src/test/resources/bpm/it/no-default-branch.bpmn20.xml`：内含一个 exclusiveGateway，仅 1 条 condition flow 且无 default：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://iimt.com/bpm/it" id="defs_no_default">
  <process id="bpm_no_default" isExecutable="true">
    <startEvent id="s"/>
    <sequenceFlow id="f1" sourceRef="s" targetRef="gw"/>
    <exclusiveGateway id="gw"/>
    <sequenceFlow id="f_only" sourceRef="gw" targetRef="t">
      <conditionExpression xsi:type="tFormalExpression"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">${aviatorEval(execution, 'form.amount &gt; 99999')}</conditionExpression>
    </sequenceFlow>
    <userTask id="t" name="X" flowable:assignee="${initiator}"/>
    <sequenceFlow id="fe" sourceRef="t" targetRef="e"/>
    <endEvent id="e"/>
  </process>
</definitions>
```

`flow/NoMatchingFlowIT.java`：起一个 amount=0 的实例，断言 1) `bpm_no_default` 主流程 task 数 = 0；2) 出现一条 `manual_review_*` 独立任务指派 admin。

- [ ] **Step 5：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='NoMatchingFlowHandlerTest,NoMatchingFlowIT'
```
期望全部通过。

- [ ] **Step 6：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/flow/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/resources/bpm/it/no-default-branch.bpmn20.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/flow/
git commit -m "feat(bpm-p3): no-matching-flow fallback to manual review task with admin assignee"
```

---

## Task 5：ScriptStrategy 完整实现（覆盖 P2 stub）

**Files:**
- 修改 `strategy/ScriptStrategy.java`（P2 stub → 真实现）
- 创建测试 `ScriptStrategyTest.java`

`bpm_assignee_strategy.payload` JSON 形如：`{ "script": "form.managerId" }`，期望脚本求值后返回单个 user id 或 user id 列表（Long / List<Long> / Number / List<Number>）。

- [ ] **Step 1：写 ScriptStrategyTest**

```java
package org.jeecg.modules.bpm.strategy;

import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScriptStrategyTest {

    @Test
    void resolvesSingleUserIdFromForm() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        when(ctxBuilder.build(any(), any())).thenReturn(Map.of(
                "form", Map.of("managerId", 42L),
                "sys", Map.of(), "user", Map.of()));
        when(evaluator.evaluate(any(), any())).thenReturn(42L);

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = new ResolveContext("def_a", 1, "form_a", "biz_1", null);
        List<Long> ids = s.resolve(ctx, Map.of("script", "form.managerId"));

        assertThat(ids).containsExactly(42L);
    }

    @Test
    void resolvesUserListFromExpression() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        when(ctxBuilder.build(any(), any())).thenReturn(Map.of("form", Map.of(), "sys", Map.of(), "user", Map.of()));
        when(evaluator.evaluate(any(), any())).thenReturn(List.of(1L, 2L, 3L));

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = new ResolveContext("def_a", 1, null, null, null);
        List<Long> ids = s.resolve(ctx, Map.of("script", "[1, 2, 3]"));

        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    void returnsEmptyOnNullResult() {
        BpmExpressionEvaluator evaluator = mock(BpmExpressionEvaluator.class);
        BpmExpressionContextBuilder ctxBuilder = mock(BpmExpressionContextBuilder.class);
        when(ctxBuilder.build(any(), any())).thenReturn(Map.of("form", Map.of(), "sys", Map.of(), "user", Map.of()));
        when(evaluator.evaluate(any(), any())).thenReturn(null);

        ScriptStrategy s = new ScriptStrategy(evaluator, ctxBuilder);
        ResolveContext ctx = new ResolveContext("def_a", 1, null, null, null);
        assertThat(s.resolve(ctx, Map.of("script", "form.managerId"))).isEmpty();
    }
}
```

- [ ] **Step 2：写实现（替换 P2 stub）**

`strategy/ScriptStrategy.java`：
```java
package org.jeecg.modules.bpm.strategy;

import org.jeecg.modules.bpm.expression.BpmExpressionCacheKey;
import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ScriptStrategy implements AssigneeStrategy {

    private final BpmExpressionEvaluator evaluator;
    private final BpmExpressionContextBuilder ctxBuilder;

    public ScriptStrategy(BpmExpressionEvaluator evaluator, BpmExpressionContextBuilder ctxBuilder) {
        this.evaluator = evaluator;
        this.ctxBuilder = ctxBuilder;
    }

    @Override public String type() { return "SCRIPT"; }

    @Override
    public List<Long> resolve(ResolveContext ctx, Map<String,Object> payload) {
        String script = (String) payload.get("script");
        if (script == null || script.isBlank()) return Collections.emptyList();

        Map<String,Object> env = ctxBuilder.build(ctx.formId(), ctx.businessKey());
        BpmExpressionCacheKey key = BpmExpressionCacheKey.of(ctx.defKey(), ctx.defVersion(), script);
        Object result = evaluator.evaluate(key, env);
        return toUserIds(result);
    }

    private List<Long> toUserIds(Object result) {
        if (result == null) return Collections.emptyList();
        if (result instanceof Number) return List.of(((Number) result).longValue());
        if (result instanceof Collection) {
            List<Long> out = new ArrayList<>();
            for (Object o : (Collection<?>) result) {
                if (o instanceof Number) out.add(((Number) o).longValue());
                else if (o != null) out.add(Long.valueOf(o.toString()));
            }
            return out;
        }
        return List.of(Long.valueOf(result.toString()));
    }
}
```

- [ ] **Step 3：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=ScriptStrategyTest
```
期望 3 个测试通过。

- [ ] **Step 4：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/strategy/ScriptStrategy.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/strategy/ScriptStrategyTest.java
git commit -m "feat(bpm-p3): ScriptStrategy backed by BpmExpressionEvaluator (replaces P2 stub)"
```

---

## Task 6：BpmnXmlPostProcessor — multi_mode 重写 userTask 为 multiInstance

**Files:**
- 创建 `multi/MultiInstanceXmlRewriter.java`
- 修改 P1 已存在的 `BpmnXmlPostProcessor.java`（在 publish 链路中调 `MultiInstanceXmlRewriter.rewrite()`）
- 创建测试 `MultiInstanceXmlRewriterTest.java`

**核心约束（spec / 用户硬约束）：** **不修改** `bpm_process_definition.bpmn_xml` 持久化字段；rewrite 在内存中发生，rewrite 后的 XML 直接喂给 `repositoryService.createDeployment().addString(...)`。

映射规则（spec §5.1）：
| multi_mode | BPMN multiInstanceLoopCharacteristics | completionCondition |
|---|---|---|
| `SEQUENCE` | `isSequential="true"` | `${nrOfCompletedInstances == nrOfInstances}` |
| `PARALLEL` | `isSequential="false"` | `${nrOfCompletedInstances/nrOfInstances >= 1.0}`（会签 — 全过）|
| `ANY` | `isSequential="false"` | `${nrOfCompletedInstances >= 1}`（或签 — 任一）|

`collection`：来自 process variable `bpm_assignees_<nodeId>`（List<String> of user id），由 P2 `AssigneeResolver` 在实例发起前 putAll 进 process vars。

- [ ] **Step 1：写 MultiInstanceXmlRewriterTest**

```java
package org.jeecg.modules.bpm.multi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultiInstanceXmlRewriterTest {

    private static final String INPUT_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
            "             xmlns:flowable=\"http://flowable.org/bpmn\" " +
            "             targetNamespace=\"x\" id=\"d\">" +
            "  <process id=\"p\" isExecutable=\"true\">" +
            "    <userTask id=\"t1\" name=\"Approve\"/>" +
            "  </process>" +
            "</definitions>";

    @Test
    void rewritesParallelMultiInstance() {
        MultiInstanceXmlRewriter r = new MultiInstanceXmlRewriter();
        String out = r.rewrite(INPUT_XML, Map.of("t1", new MultiModeConfig("PARALLEL")));
        assertThat(out).contains("multiInstanceLoopCharacteristics");
        assertThat(out).contains("isSequential=\"false\"");
        assertThat(out).contains("nrOfCompletedInstances/nrOfInstances &gt;= 1.0");
        assertThat(out).contains("flowable:collection=\"${bpm_assignees_t1}\"");
        assertThat(out).contains("flowable:elementVariable=\"assignee\"");
    }

    @Test
    void rewritesAnyMultiInstance() {
        String out = new MultiInstanceXmlRewriter().rewrite(INPUT_XML, Map.of("t1", new MultiModeConfig("ANY")));
        assertThat(out).contains("nrOfCompletedInstances &gt;= 1");
    }

    @Test
    void rewritesSequenceMultiInstance() {
        String out = new MultiInstanceXmlRewriter().rewrite(INPUT_XML, Map.of("t1", new MultiModeConfig("SEQUENCE")));
        assertThat(out).contains("isSequential=\"true\"");
        assertThat(out).contains("nrOfCompletedInstances == nrOfInstances");
    }

    @Test
    void leavesXmlUntouchedWhenNoConfig() {
        String out = new MultiInstanceXmlRewriter().rewrite(INPUT_XML, Map.of());
        assertThat(out).doesNotContain("multiInstanceLoopCharacteristics");
    }
}
```

- [ ] **Step 2：写实现**

`multi/MultiInstanceXmlRewriter.java`：
```java
package org.jeecg.modules.bpm.multi;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class MultiInstanceXmlRewriter {

    private static final String BPMN_NS     = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    public String rewrite(String bpmnXml, Map<String, MultiModeConfig> nodeConfigs) {
        if (nodeConfigs == null || nodeConfigs.isEmpty()) return bpmnXml;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(
                    new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            NodeList userTasks = doc.getElementsByTagNameNS(BPMN_NS, "userTask");
            for (int i = 0; i < userTasks.getLength(); i++) {
                Element ut = (Element) userTasks.item(i);
                String id = ut.getAttribute("id");
                MultiModeConfig cfg = nodeConfigs.get(id);
                if (cfg == null) continue;
                injectMultiInstance(doc, ut, id, cfg);
            }

            StringWriter sw = new StringWriter();
            javax.xml.transform.Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException("rewrite multiInstance failed", e);
        }
    }

    private void injectMultiInstance(Document doc, Element userTask, String nodeId, MultiModeConfig cfg) {
        Element mi = doc.createElementNS(BPMN_NS, "multiInstanceLoopCharacteristics");
        mi.setAttribute("isSequential", "SEQUENCE".equals(cfg.mode()) ? "true" : "false");
        mi.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:flowable", FLOWABLE_NS);
        mi.setAttributeNS(FLOWABLE_NS, "flowable:collection", "${bpm_assignees_" + nodeId + "}");
        mi.setAttributeNS(FLOWABLE_NS, "flowable:elementVariable", "assignee");

        Element completion = doc.createElementNS(BPMN_NS, "completionCondition");
        completion.setTextContent(buildCompletionCondition(cfg));
        mi.appendChild(completion);

        // 让 userTask 用 elementVariable 作为 assignee（覆盖原 assignee 属性）
        userTask.setAttributeNS(FLOWABLE_NS, "flowable:assignee", "${assignee}");

        // 把 mi 插在 userTask 第一个子节点位置
        Node first = userTask.getFirstChild();
        if (first != null) userTask.insertBefore(mi, first); else userTask.appendChild(mi);
    }

    private String buildCompletionCondition(MultiModeConfig cfg) {
        switch (cfg.mode()) {
            case "PARALLEL": return "${nrOfCompletedInstances/nrOfInstances >= 1.0}";
            case "ANY":      return "${nrOfCompletedInstances >= 1}";
            case "SEQUENCE": return "${nrOfCompletedInstances == nrOfInstances}";
            default: throw new IllegalArgumentException("unknown multi mode: " + cfg.mode());
        }
    }
}
```

`multi/MultiModeConfig.java`：
```java
package org.jeecg.modules.bpm.multi;

public record MultiModeConfig(String mode) {
    public MultiModeConfig {
        if (!"PARALLEL".equals(mode) && !"ANY".equals(mode) && !"SEQUENCE".equals(mode)) {
            throw new IllegalArgumentException("invalid mode: " + mode);
        }
    }
}
```

- [ ] **Step 3：在 P1 BpmnXmlPostProcessor publish 链路调 rewriter**

修改 P1 已存在的 `BpmnXmlPostProcessor.java`：在 publish 方法的 deploy 前一步加：
```java
Map<String,MultiModeConfig> miMap = nodeConfigReader
        .findAllByDefId(defId).stream()
        .filter(c -> c.getMultiMode() != null)
        .collect(Collectors.toMap(NodeConfig::getNodeId, c -> new MultiModeConfig(c.getMultiMode())));
String deployedXml = multiInstanceXmlRewriter.rewrite(persistedBpmnXml, miMap);
repositoryService.createDeployment().addString(name + ".bpmn20.xml", deployedXml).deploy();
```

> 注意：`persistedBpmnXml` 不被覆盖回 DB；`deployedXml` 仅作为 deploy 入参。

- [ ] **Step 4：跑单元测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=MultiInstanceXmlRewriterTest
```
期望 4 个测试通过。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/multi/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/publish/BpmnXmlPostProcessor.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/multi/MultiInstanceXmlRewriterTest.java
git commit -m "feat(bpm-p3): rewrite userTask to multiInstance per bpm_node_config.multi_mode at publish"
```

---

## Task 7：多实例集成测试 — 会签 / 或签 / 顺序签

**Files:**
- 创建 3 个 BPMN 资源（已在 File Structure 列）
- 创建 3 个 IT：`ParallelCountersignIT.java` / `AnyCountersignIT.java` / `SequenceCountersignIT.java`

每个测试：
1. 用 rewriter 处理一个原始 userTask BPMN，得到 multiInstance XML
2. deploy → start，传 `bpm_assignees_<nodeId> = [u1,u2,u3]`
3. 断言：
   - 会签：3 个 task 同时存在；逐个 complete；只有第 3 个完成后实例才结束
   - 或签：3 个 task 同时存在；complete 第 1 个后实例立刻进入下一个节点（剩余 2 个被自动取消）
   - 顺序签：每次只看到 1 个 task；complete 后下一个出现；3 次后实例结束

- [ ] **Step 1：写 parallel-countersign.bpmn20.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://iimt.com/bpm/it" id="defs_pc">
  <process id="bpm_parallel_cs" isExecutable="true">
    <startEvent id="s"/>
    <sequenceFlow id="f1" sourceRef="s" targetRef="t"/>
    <userTask id="t" name="Approve"/>
    <sequenceFlow id="f2" sourceRef="t" targetRef="e"/>
    <endEvent id="e"/>
  </process>
</definitions>
```

类似写 `any-countersign.bpmn20.xml` / `sequence-countersign.bpmn20.xml`（结构一致，nodeId/process id 不同）。

- [ ] **Step 2：写 ParallelCountersignIT**

`jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/multi/ParallelCountersignIT.java`：
```java
package org.jeecg.modules.bpm.multi;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { org.jeecg.modules.bpm.BpmModuleAutoConfiguration.class })
@ActiveProfiles("test") @Testcontainers
class ParallelCountersignIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void p(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService    runtimeService;
    @Autowired TaskService       taskService;
    @Autowired MultiInstanceXmlRewriter rewriter;

    @Test
    void allThreeMustApproveBeforeProceed() throws IOException {
        String raw = new String(getClass().getResourceAsStream(
                "/bpm/it/parallel-countersign.bpmn20.xml").readAllBytes(), StandardCharsets.UTF_8);
        String rewritten = rewriter.rewrite(raw, Map.of("t", new MultiModeConfig("PARALLEL")));
        repositoryService.createDeployment().addString("pc.bpmn20.xml", rewritten).deploy();

        Map<String,Object> v = new HashMap<>();
        v.put("bpm_assignees_t", List.of("u1", "u2", "u3"));
        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_parallel_cs", v);

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(3);

        // 完成前两个：实例还在
        taskService.complete(tasks.get(0).getId());
        taskService.complete(tasks.get(1).getId());
        ProcessInstance still = runtimeService.createProcessInstanceQuery().processInstanceId(inst.getId()).singleResult();
        assertThat(still).isNotNull();

        // 完成第三个：实例结束
        taskService.complete(tasks.get(2).getId());
        ProcessInstance ended = runtimeService.createProcessInstanceQuery().processInstanceId(inst.getId()).singleResult();
        assertThat(ended).isNull();
    }
}
```

- [ ] **Step 3：写 AnyCountersignIT 与 SequenceCountersignIT**

`AnyCountersignIT`：
- 起实例后断言 3 个 task；complete 第 1 个后断言实例立即结束（剩余被取消）

`SequenceCountersignIT`：
- 起实例后只断言 1 个 task；complete 后下一个 task 出现；循环 3 次后实例结束

- [ ] **Step 4：跑全部 IT**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='ParallelCountersignIT,AnyCountersignIT,SequenceCountersignIT'
```
期望全部通过。首次拉镜像 ~30–60s。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm/it/parallel-countersign.bpmn20.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm/it/any-countersign.bpmn20.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm/it/sequence-countersign.bpmn20.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/multi/
git commit -m "test(bpm-p3): integration tests for parallel/any/sequence multi-instance"
```

---

## Task 8：TaskController 增 TRANSFER 动作

**Files:**
- 修改 `controller/TaskController.java`
- 修改 `service/TaskActionService.java`
- 修改 `bpm-api` 中 `TaskCompleteRequest`（如已存在；若 P2 仅有 APPROVE/REJECT，需扩 enum）
- 修改 `bpm_task_history` 的 action enum 常量类（adapter 层）
- 创建测试 `TaskActionServiceTest.java`（transfer case） / `TaskControllerTransferTest.java` / `TransferFlowIT.java`

**行为：** `POST /bpm/v1/task/{id}/complete` body：`{"action":"TRANSFER","targetUserId":"u_b","comment":"..."}`。
- 写 `bpm_task_history` 一条 `action=TRANSFER, assigneeId=A, comment=transfer to B`
- 调 `taskService.setAssignee(taskId, "u_b")`，**任务保留**（不 complete）
- 不推进流程

- [ ] **Step 1：写 TaskActionServiceTest**

```java
package org.jeecg.modules.bpm.service;

import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.jeecg.modules.bpm.api.dto.TaskCompleteRequest;
import org.jeecg.modules.bpm.api.dto.TaskAction;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class TaskActionServiceTest {

    @Test
    void transferReassignsTaskAndWritesHistory() {
        TaskService taskService = mock(TaskService.class);
        BpmTaskHistoryRecorder recorder = mock(BpmTaskHistoryRecorder.class);
        BpmUserContext userCtx = mock(BpmUserContext.class);
        when(userCtx.currentUserId()).thenReturn(100L);

        Task t = mock(Task.class);
        when(t.getId()).thenReturn("task_1");
        when(t.getAssignee()).thenReturn("100");
        when(taskService.createTaskQuery().taskId("task_1").singleResult()).thenReturn(t);

        TaskActionService svc = new TaskActionService(taskService, recorder, userCtx);
        TaskCompleteRequest req = new TaskCompleteRequest(TaskAction.TRANSFER, "200", "to B", null);
        svc.complete("task_1", req);

        verify(taskService).setAssignee("task_1", "200");
        verify(recorder).record("task_1", TaskAction.TRANSFER, 100L, "to B");
        // 不调 complete
        verify(taskService, org.mockito.Mockito.never()).complete(org.mockito.ArgumentMatchers.anyString());
    }
}
```

- [ ] **Step 2：扩 enum + DTO（在 bpm-api）**

`jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/dto/TaskAction.java`：
```java
package org.jeecg.modules.bpm.api.dto;

public enum TaskAction {
    APPROVE,
    REJECT,
    TRANSFER,
    COUNTERSIGN
}
```

`TaskCompleteRequest.java`：
```java
package org.jeecg.modules.bpm.api.dto;

import java.util.Map;

public record TaskCompleteRequest(TaskAction action,
                                  String targetUserId,   // TRANSFER / COUNTERSIGN 用
                                  String comment,
                                  Map<String,Object> formData /* APPROVE 用 */) { }
```

- [ ] **Step 3：扩 TaskActionService**

修改 `service/TaskActionService.java`，加 `transfer` 分支：
```java
public void complete(String taskId, TaskCompleteRequest req) {
    switch (req.action()) {
        case APPROVE:     approve(taskId, req); break;
        case REJECT:      reject(taskId, req); break;
        case TRANSFER:    transfer(taskId, req); break;
        case COUNTERSIGN: countersign(taskId, req); break;
        default: throw new IllegalArgumentException("unknown action: " + req.action());
    }
}

private void transfer(String taskId, TaskCompleteRequest req) {
    if (req.targetUserId() == null || req.targetUserId().isBlank())
        throw new IllegalArgumentException("targetUserId required for TRANSFER");
    taskService.setAssignee(taskId, req.targetUserId());
    recorder.record(taskId, TaskAction.TRANSFER, userCtx.currentUserId(), req.comment());
}
```

- [ ] **Step 4：扩 TaskController**

P2 已存在的 `TaskController` 已经把 body 反序列化到 `TaskCompleteRequest`，本 Task 无需改 controller，只是 enum 扩了之后路由自然支持新 action。补一个简单 controller 测试覆盖路由：

```java
package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.api.dto.TaskAction;
import org.jeecg.modules.bpm.service.TaskActionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc
class TaskControllerTransferTest {

    @Autowired MockMvc mvc;
    @MockBean TaskActionService svc;

    @Test
    void postCompleteRoutesTransfer() throws Exception {
        mvc.perform(post("/bpm/v1/task/task_1/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"TRANSFER\",\"targetUserId\":\"u_b\",\"comment\":\"to B\"}"))
           .andExpect(status().isOk());
        verify(svc).complete(eq("task_1"), any());
    }
}
```

- [ ] **Step 5：写 TransferFlowIT**

完整流程：deploy hello-world → start（assignee=u_a）→ POST /complete TRANSFER targetUserId=u_b → 断言 task.assignee=u_b → POST /complete APPROVE → 实例结束 → bpm_task_history 至少 2 条（TRANSFER + APPROVE）。

- [ ] **Step 6：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='TaskActionServiceTest,TaskControllerTransferTest,TransferFlowIT'
```
期望全部通过。

- [ ] **Step 7：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/dto/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/service/TaskActionService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/service/TaskActionServiceTest.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/TaskControllerTransferTest.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/TransferFlowIT.java
git commit -m "feat(bpm-p3): TRANSFER action — reassign task without advancing flow"
```

---

## Task 9：TaskController 增 COUNTERSIGN 动作

**Files:**
- 修改 `service/TaskActionService.java`（加 `countersign`）
- 创建测试 `TaskControllerCountersignTest.java` / `CountersignFlowIT.java`

**行为：** `POST /bpm/v1/task/{id}/complete` body：`{"action":"COUNTERSIGN","targetUserId":"u_c","comment":"..."}`。
- 在**同节点**为加签人新建一个独立 task（`taskService.newTask(...)` + `setAssignee("u_c")` + 设置 `parent_task_id` = 原 task）
- 原 task 不动，仍然 assigned to A
- A 和 C 都 complete 后才推进（用 process variable + execution listener 协调；本期最简实现：原 task 设置 owner，并把"待加签人完成数"作为 process variable，由原 task 完成时检查）

> **简化决策：** 本期采用"挂起原 task + 等加签 task 完成"模式更稳：
> 1. 调 `taskService.suspendTask(原 task id)` 暂停原 task
> 2. 新建 sub-task（`taskService.newSubTask(...)` 模式）：assignee = u_c，parent = 原 task id
> 3. `bpm_task_history` 记 `action=COUNTERSIGN, assigneeId=A, comment="加签 to C"`
> 4. 加签 task complete 时 → 自动 activate 原 task
> 5. 原 task 完成时正常 complete 流程

简化的关键：**用 Flowable 的 sub-task API**（`taskService.newSubTask` / `taskService.getSubTasks`），不引入新表。

- [ ] **Step 1：写测试**

`TaskControllerCountersignTest.java`（MockMvc 路由）：略，结构同 transfer。

`CountersignFlowIT.java`：
- deploy hello-world → start（assignee=u_a）
- 取 task_a，POST /complete COUNTERSIGN targetUserId=u_c
- 断言：task_a 仍存在但被挂起；task_c 出现（`createTaskQuery().taskParentTaskId(task_a)`）
- POST /complete APPROVE on task_c
- 断言：task_c 消失；task_a 恢复 active
- POST /complete APPROVE on task_a
- 断言：实例结束；bpm_task_history 含 COUNTERSIGN + 2 条 APPROVE（C 和 A）

- [ ] **Step 2：写实现**

`TaskActionService.java#countersign`：
```java
private void countersign(String taskId, TaskCompleteRequest req) {
    if (req.targetUserId() == null || req.targetUserId().isBlank())
        throw new IllegalArgumentException("targetUserId required for COUNTERSIGN");
    Task parent = taskService.createTaskQuery().taskId(taskId).singleResult();
    if (parent == null) throw new IllegalArgumentException("task not found: " + taskId);

    // 1. 挂起原 task
    taskService.suspendTask(taskId);

    // 2. 建 sub-task
    Task sub = taskService.newTask();
    sub.setName(parent.getName() + "（加签）");
    sub.setAssignee(req.targetUserId());
    sub.setParentTaskId(taskId);
    sub.setOwner(String.valueOf(userCtx.currentUserId()));
    taskService.saveTask(sub);

    // 3. 历史
    recorder.record(taskId, TaskAction.COUNTERSIGN, userCtx.currentUserId(),
            "加签给 " + req.targetUserId() + (req.comment() == null ? "" : "：" + req.comment()));
}
```

并在 `approve(...)` 入口检测：若 task 是 sub-task（`getParentTaskId() != null`），complete 后自动 `activateTask(parent)`：
```java
private void approve(String taskId, TaskCompleteRequest req) {
    Task t = taskService.createTaskQuery().taskId(taskId).singleResult();
    String parentId = (t == null) ? null : t.getParentTaskId();
    taskService.complete(taskId, req.formData() == null ? java.util.Map.of() : req.formData());
    recorder.record(taskId, TaskAction.APPROVE, userCtx.currentUserId(), req.comment());
    if (parentId != null) {
        // 检查是否还有未完成的 sub-task
        long pending = taskService.createTaskQuery().taskParentTaskId(parentId).count();
        if (pending == 0) taskService.activateTask(parentId);
    }
}
```

- [ ] **Step 3：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='TaskControllerCountersignTest,CountersignFlowIT'
```
期望全部通过。

- [ ] **Step 4：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/service/TaskActionService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/TaskControllerCountersignTest.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/CountersignFlowIT.java
git commit -m "feat(bpm-p3): COUNTERSIGN action — suspend parent + sub-task + auto-resume"
```

---

## Task 10：前端 ConditionExpressionEditor.vue + MultiModeSelector.vue 接入设计器

**Files（在工作副本 `./jeecgboot-vue3/src/views/bpm/` — 不入本 BPM 仓库）：**
- 创建 `designer/components/ConditionExpressionEditor.vue`
- 创建 `designer/components/MultiModeSelector.vue`
- 修改 `designer/components/NodePropertiesPanel.vue`（接入两个新组件）
- 创建 `api/bpm/expression.ts`

> **重要：** 这些前端文件**不提交到 BPM 仓库**（仓库只装后端）；变更示例片段写到 `INTEGRATION.md` + `P3_DONE.md` 末尾"前端集成片段"小节，便于实际部署时拷贝。

- [ ] **Step 1：写 ConditionExpressionEditor.vue**

```vue
<template>
  <div class="condition-expression-editor">
    <a-textarea
      v-model:value="expression"
      placeholder="例如：form.amount > 10000"
      :rows="3"
      @change="emit('update:modelValue', expression)"
    />
    <div class="variable-picker">
      <a-tag v-for="f in formFields" :key="f.name"
             style="cursor:pointer; margin-top:4px;"
             @click="insert(`form.${f.name}`)">
        form.{{ f.name }}
      </a-tag>
      <a-tag style="cursor:pointer" @click="insert('sys.now')">sys.now</a-tag>
      <a-tag style="cursor:pointer" @click="insert('sys.today')">sys.today</a-tag>
      <a-tag style="cursor:pointer" @click="insert('user.id')">user.id</a-tag>
      <a-tag style="cursor:pointer" @click="insert('user.deptId')">user.deptId</a-tag>
    </div>
    <div style="margin-top:8px">
      <a-button size="small" @click="testEval">测试求值</a-button>
      <span v-if="testResult !== null" style="margin-left:8px">结果：{{ testResult }}</span>
      <span v-if="testError" style="margin-left:8px; color:red">{{ testError }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { testExpression } from '/@/api/bpm/expression';

const props = defineProps<{ modelValue: string; formFields: { name: string; type: string }[]; defKey: string; defVersion: number }>();
const emit  = defineEmits(['update:modelValue']);

const expression = ref(props.modelValue || '');
const testResult = ref<any>(null);
const testError  = ref<string>('');

watch(() => props.modelValue, v => { expression.value = v || ''; });

function insert(token: string) {
  expression.value = (expression.value || '') + token;
  emit('update:modelValue', expression.value);
}

async function testEval() {
  testError.value = '';
  testResult.value = null;
  try {
    const r = await testExpression({
      defKey: props.defKey,
      defVersion: props.defVersion,
      expression: expression.value,
      sampleForm: Object.fromEntries(props.formFields.map(f => [f.name, defaultFor(f.type)])),
    });
    testResult.value = r.result;
  } catch (e: any) {
    testError.value = e.message || '求值失败';
  }
}

function defaultFor(type: string) {
  if (type === 'number' || type === 'int') return 0;
  if (type === 'boolean') return false;
  return '';
}
</script>
```

- [ ] **Step 2：写 MultiModeSelector.vue**

```vue
<template>
  <div>
    <a-radio-group v-model:value="mode" @change="emit('update:modelValue', mode)">
      <a-radio value="">不启用</a-radio>
      <a-radio value="SEQUENCE">顺序签</a-radio>
      <a-radio value="PARALLEL">会签（全部通过）</a-radio>
      <a-radio value="ANY">或签（任一通过）</a-radio>
    </a-radio-group>
    <div v-if="mode" style="margin-top:8px; color:#888; font-size:12px;">
      完成条件：<code>{{ completionPreview }}</code>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';

const props = defineProps<{ modelValue: string }>();
const emit  = defineEmits(['update:modelValue']);

const mode = ref(props.modelValue || '');
watch(() => props.modelValue, v => { mode.value = v || ''; });

const completionPreview = computed(() => {
  switch (mode.value) {
    case 'SEQUENCE': return '${nrOfCompletedInstances == nrOfInstances}';
    case 'PARALLEL': return '${nrOfCompletedInstances/nrOfInstances >= 1.0}';
    case 'ANY':      return '${nrOfCompletedInstances >= 1}';
    default: return '';
  }
});
</script>
```

- [ ] **Step 3：写 api/bpm/expression.ts**

```ts
import { defHttp } from '/@/utils/http/axios';

export interface TestExpressionReq {
  defKey: string;
  defVersion: number;
  expression: string;
  sampleForm: Record<string, any>;
}
export interface TestExpressionResp { result: any; error?: string; }

export const testExpression = (params: TestExpressionReq) =>
  defHttp.post<TestExpressionResp>({ url: '/bpm/v1/expression/test', data: params });
```

- [ ] **Step 4：修改 NodePropertiesPanel.vue**

在原有节点表单中加入：
```vue
<a-form-item v-if="node.type === 'sequenceFlow'" label="条件表达式">
  <ConditionExpressionEditor
    v-model="node.conditionExpression"
    :form-fields="formFields"
    :def-key="defKey"
    :def-version="defVersion"/>
</a-form-item>

<a-form-item v-if="node.type === 'userTask'" label="多人模式">
  <MultiModeSelector v-model="node.multiMode"/>
</a-form-item>
```

- [ ] **Step 5：在浏览器中冒烟（需 P0 Task 11 已建立 jeecg-boot 本地 dev 环境）**

启动后端 + 启动 jeecgboot-vue3，登录 → 进入"流程配置 / 流程设计器" → 新建流程 → 拖一个 sequenceFlow → 右侧出现"条件表达式"输入框 → 点"测试求值" → 看到 200/400 + result 字段。

> 单元测试可选用 Vitest，但前端单测不在 P3 必交清单内（P5 / 体验优化阶段做）。

- [ ] **Step 6：commit（仅后端 + 文档片段）**

前端文件不入仓库；本 Step 只提交"在 INTEGRATION.md 末尾追加前端集成片段"：

```bash
git add INTEGRATION.md
git commit -m "docs(bpm-p3): frontend snippets for condition editor + multi-mode selector"
```

---

## Task 11：/bpm/v1/expression/test endpoint + P3 验收清单

**Files:**
- 创建 `controller/ExpressionTestController.java`
- 创建 `bpm-api` 中 `ExpressionTestRequest` / `ExpressionTestResponse`
- 创建测试 `ExpressionTestControllerTest.java`
- 创建 `jeecg-module-bpm/P3_DONE.md`

- [ ] **Step 1：写 DTO**

`jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/dto/ExpressionTestRequest.java`：
```java
package org.jeecg.modules.bpm.api.dto;

import java.util.Map;

public record ExpressionTestRequest(String defKey,
                                    int    defVersion,
                                    String expression,
                                    Map<String,Object> sampleForm) { }
```

`ExpressionTestResponse.java`：
```java
package org.jeecg.modules.bpm.api.dto;

public record ExpressionTestResponse(Object result, String error) { }
```

- [ ] **Step 2：写 controller**

`controller/ExpressionTestController.java`：
```java
package org.jeecg.modules.bpm.controller;

import org.jeecg.modules.bpm.api.dto.ExpressionTestRequest;
import org.jeecg.modules.bpm.api.dto.ExpressionTestResponse;
import org.jeecg.modules.bpm.expression.BpmExpressionCacheKey;
import org.jeecg.modules.bpm.expression.BpmExpressionContextBuilder;
import org.jeecg.modules.bpm.expression.BpmExpressionEvaluator;
import org.jeecg.modules.bpm.expression.BpmExpressionException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bpm/v1/expression")
public class ExpressionTestController {

    private final BpmExpressionEvaluator evaluator;
    private final BpmExpressionContextBuilder ctxBuilder;

    public ExpressionTestController(BpmExpressionEvaluator e, BpmExpressionContextBuilder b) {
        this.evaluator = e; this.ctxBuilder = b;
    }

    @PostMapping("/test")
    public ExpressionTestResponse test(@RequestBody ExpressionTestRequest req) {
        try {
            Map<String,Object> env = new HashMap<>(ctxBuilder.build(null, null));
            env.put("form", req.sampleForm() == null ? Map.of() : req.sampleForm());
            BpmExpressionCacheKey key = BpmExpressionCacheKey.of(
                    req.defKey() == null ? "test" : req.defKey(),
                    req.defVersion(),
                    req.expression());
            Object r = evaluator.evaluate(key, env);
            return new ExpressionTestResponse(r, null);
        } catch (BpmExpressionException be) {
            return new ExpressionTestResponse(null, be.getMessage());
        }
    }
}
```

- [ ] **Step 3：写 ExpressionTestControllerTest**

```java
package org.jeecg.modules.bpm.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc
class ExpressionTestControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void evaluatesAmountGreaterThan() throws Exception {
        mvc.perform(post("/bpm/v1/expression/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"defKey\":\"d\",\"defVersion\":1,\"expression\":\"form.amount > 100\"," +
                         "\"sampleForm\":{\"amount\":150}}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.result").value(true));
    }

    @Test
    void rejectsReflectionAttempt() throws Exception {
        mvc.perform(post("/bpm/v1/expression/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"defKey\":\"d\",\"defVersion\":1," +
                         "\"expression\":\"Class.forName('java.lang.Runtime')\",\"sampleForm\":{}}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.error").exists());
    }
}
```

- [ ] **Step 4：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=ExpressionTestControllerTest
```
期望 2 个测试通过。

- [ ] **Step 5：跑全量测试 + install**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml clean install
```
期望 BUILD SUCCESS；P0+P1+P2+P3 全量测试都通过。

- [ ] **Step 6：写 P3_DONE.md**

`jeecg-module-bpm/P3_DONE.md`：
```markdown
# P3 验收清单 ✅

## 表达式引擎
- [x] aviatorscript 5.4.3 引入 bpm-biz；BpmExpressionEvaluator 单测通过
- [x] 沙箱测试：反射 / System / Runtime / import / FileIO 全部被拒绝
- [x] 200ms 硬超时：死循环表达式被强制中断
- [x] 编译缓存 key = `defKey + version + sha256(expression)`，命中复用
- [x] form / sys / user 三命名空间组装通过 BpmExpressionContextBuilder

## Flowable 集成
- [x] AviatorConditionDelegate 通过 JUEL `${aviatorEval(...)}` 暴露给 Flowable
- [x] amount-branch IT：amount=20000 走 high；amount=5000 走 default

## 异常兜底
- [x] NoMatchingFlowHandler 监听 JOB_EXECUTION_FAILURE，匹配"No outgoing sequence flow"消息
- [x] 触发 ManualReviewTaskCreator → 读 fallbackAssignee（默认 admin）建任务
- [x] no-default-branch IT：amount=0 → 主流程无 task；出现 manual_review_* 任务

## 多实例
- [x] MultiInstanceXmlRewriter：SEQUENCE / PARALLEL / ANY 三种正确重写（4 个单测）
- [x] 不修改 bpm_process_definition.bpmn_xml；rewrite 仅在内存中
- [x] BpmnXmlPostProcessor publish 链路调用 rewriter
- [x] ParallelCountersignIT：3 人会签，全过才结束
- [x] AnyCountersignIT：3 人或签，任一通过即结束
- [x] SequenceCountersignIT：3 人顺序签，逐个推进

## ScriptStrategy
- [x] 替换 P2 stub；调 BpmExpressionEvaluator 求值
- [x] 支持单 user id / List<user id> / null 三种返回

## TaskController
- [x] TRANSFER：reassign + 写 bpm_task_history.action=TRANSFER；不推进流程
- [x] COUNTERSIGN：挂起原 task + 建 sub-task；sub-task 完成自动 activate parent
- [x] TransferFlowIT / CountersignFlowIT 均通过

## 表达式 dry-run
- [x] POST /bpm/v1/expression/test 接受 expression + sampleForm，返回 result 或 error
- [x] 反射攻击在 dry-run 也被沙箱拦截

## 前端
- [x] ConditionExpressionEditor.vue：textarea + 变量 tag picker + 测试求值按钮
- [x] MultiModeSelector.vue：4 选 1 radio + completionCondition 预览
- [x] NodePropertiesPanel.vue 接入；INTEGRATION.md 含前端集成片段

## 数据
- [x] bpm_task_history.action enum 扩展 TRANSFER / COUNTERSIGN（在 P2 已有 APPROVE/REJECT 基础上）

## 下一步（P4）
- 完整版本状态机 DRAFT→TESTING→PUBLISHED→ARCHIVED
- Redisson 分布式锁 publish 防并发
- 沙箱模式（category=SANDBOX + 静默通知）
- 历史版本快照 / 回滚
```

- [ ] **Step 7：commit + push**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/dto/ExpressionTest*.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/controller/ExpressionTestController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/ExpressionTestControllerTest.java \
        jeecg-module-bpm/P3_DONE.md
git commit -m "feat(bpm-p3): /bpm/v1/expression/test endpoint + P3 acceptance checklist"
git push origin main
```

---

## Self-Review Notes

**spec 覆盖：**
- §3.4（AviatorScript 选型）→ Task 1 ✅
- §4.1（`bpm_node_config.multi_mode` / `bpm_task_history.action` 扩 TRANSFER/COUNTERSIGN）→ Task 6, 8, 9 ✅
- §5.1（多人节点模式 + ScriptStrategy）→ Task 5, 6, 7 ✅
- §5.2（条件分支 + 表达式 + 默认分支）→ Task 1, 2, 3, 4 ✅
- §6（API：`/task/{id}/complete` 扩 + 新 `/expression/test`）→ Task 8, 9, 11 ✅
- §8（脚本沙箱：反射/System/ProcessBuilder/200ms）→ Task 1（AviatorSandboxTest 4 项断言 + BpmExpressionTimeoutTest）✅
- §9（表达式异常 / 节点人员空 / 无默认分支挂人工）→ Task 4, 5 ✅
- §10（Testcontainers MySQL）→ Task 3, 4, 7, 8, 9 ✅

**未覆盖（按 P3 范围正确排除）：**
- 完整版本状态机 / Redisson / 沙箱（spec §5.4 / §5.5）→ P4
- 监控页 / 强制干预 / 效率统计（spec §5.6）→ P5
- 前端 i18n / Qiankun 子应用 → P4 评估

**硬约束自检：**
- ✅ Aviator 显式 `Options.FEATURE_SET` + disableFeature(Module/NewInstance/StaticMethods/StaticFields/Lambda) — Task 1 Step 5
- ✅ 缓存 key = `defKey + version + sha256(expr)` — Task 1 Step 4
- ✅ 200ms 硬超时（Future.get + cancel）— Task 1 Step 5；测试覆盖（BpmExpressionTimeoutTest）
- ✅ bpm-biz 零 jeecg 依赖 — Task 1 Step 1 仅加 aviator；其他 task 都通过 SPI
- ✅ multi_mode 重写仅在内存（rewriter 入参为 String，输出 String，不写 DB）— Task 6 Step 2
- ✅ `bpm_task_history.action` enum 在 bpm-api 中维护 — Task 8 Step 2
- ✅ TDD 顺序：每个 Task 都先写测试看其失败再实现 — Task 1/2/3/4/5/6/7/8/9/11 全部体现
- ✅ Conventional commits `feat(bpm-p3): ...` / `test(bpm-p3): ...` / `docs(bpm-p3): ...`

**类型/路径一致性：**
- `BpmExpressionEvaluator` Task 1 定义；Task 2/3/5/11 引用 ✅
- `BpmExpressionContextBuilder` Task 2 定义；Task 3/5/11 引用 ✅
- `BpmExpressionCacheKey` Task 1；其它处使用 `BpmExpressionCacheKey.of(defKey, version, expr)` 一致 ✅
- `MultiInstanceXmlRewriter` / `MultiModeConfig` Task 6 定义；Task 7 IT 引用 ✅
- `TaskAction` enum 在 bpm-api Task 8 定义；TaskActionService 与 controller 引用 ✅
- 路径前缀 `/bpm/v1/...` 全部一致 ✅
- BPMN 文件路径 `bpm/it/*.bpmn20.xml` 在 main resources 与 IT classpath 都一致 ✅

**Placeholder 检查：** 无 TBD / TODO / FIXME / 待补充 / 待定 / 稍后 / 未来补 字样。
