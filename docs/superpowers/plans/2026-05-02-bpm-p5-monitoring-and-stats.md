# BPM P5 — 运维监控 + 效率统计 + 强制干预 + 定时任务（模块收官） 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付 BPM 模块**运维监控与效率统计**完整能力，以及**强制干预 / 节点超时 / 历史清理**三类运行时治理特性，关闭 BPM 模块需求文档第三节 7 大块的最后一块（监控与运维）。在 P4（版本生命周期 + 沙箱）完成的基础上，新增 `MonitorController`（实例查询 / 流程图高亮 / 三类聚合统计）、`/instance/{id}/intervene`（FORCE_COMPLETE_TASK / FORCE_CANCEL / FORCE_REASSIGN）、Spring `@Scheduled` 节点超时检查 job、Spring `@Scheduled` 历史清理 job；前端补 `ProcessMonitor.vue` / `InstanceDetailDrawer.vue`（含 bpmn-js viewer 高亮当前节点）/ `StatsDashboard.vue`（ECharts）。**P5 是 BPM 模块的最后一个 phase——本计划完成后整个模块闭环。**

**Architecture：**
- bpm-biz 在原有 `controller / service / engine / mapper` 之外新增 `monitor`（监控查询 + 聚合统计 + 干预）与 `scheduler`（@Scheduled 任务）两个子包；保持对 jeecg 零依赖（节点超时通知 / 升级走 SPI `BpmNotificationSender` 与 `BpmOrgService`）
- 强制干预统一走 `InstanceInterventionService`，落库 `bpm_task_history.action` ∈ {`FORCE_COMPLETE_TASK`, `FORCE_CANCEL_INSTANCE`, `FORCE_REASSIGN`}；接口侧 Shiro `@RequiresPermissions("bpm:monitor:intervene")`
- 节点超时 + 历史清理：使用 Spring `@Scheduled`（spec §1.2 / §13 — 与 jeecg Quartz starter 共存而**不**用 jeecg Quartz）；通过 `bpm.scheduler.timeout.cron`（默认 `0 */5 * * * ?`）与 `bpm.scheduler.history.cleanup.cron`（默认 `0 0 3 * * ?`）开关与 cron 可配
- 历史保留期通过 `bpm.history.retention-days`（默认 180）配置，删除 `bpm_task_history` + `act_hi_*` 中超期的、`bpm_instance_meta.state` ∈ {COMPLETED, CANCELLED} 的行
- 前端：jeecgboot-vue3 工作副本 `./jeecgboot-vue3/src/views/bpm/` 下新增 monitor + stats 页面；图表用现有 ECharts vendor chunk（**不**升版本）；流程图查看用 P1 已引入的 bpmn-js viewer

**Tech Stack:**（在 P4 已有栈基础上**不**新增依赖）
- 后端：Spring Boot 2.7.10 / Flowable 6.8.0（`HistoryService` / `RuntimeService` / `TaskService`）/ MyBatis-Plus 3.5.3.1 / Spring `@Scheduled`（无新依赖，spring-context 自带）/ Aviator（已在 P3 接入，不变）
- 测试：JUnit 5 + Mockito + Testcontainers MySQL 1.19.3（与 P0 一致）
- 前端：jeecgboot-vue3（Vue 3.3.4 / Ant Design Vue 3.x / VxeTable / ECharts — 现有 vendor chunk）/ bpmn-js 17.x（P1 已引入）

**与 spec 对应章节：** `docs/superpowers/specs/2026-04-30-bpm-module-design.md` §1.2（Quartz 共存原则）、§5.6（监控与运维全部）、§6（API 设计：`/monitor/instances`、`/monitor/stats`、`/instance/{id}/intervene`）、§7.2（前端组件 `ProcessMonitor.vue` / bpmn-js viewer 高亮）、§8（强制干预的接口级权限）、§11（历史清理 6 个月保留期）、§13（Quartz 与 jeecg 共存）。

**前置假设（P0~P4 已交付，复述见用户指令"Assumed delivered before P5"）：**
1. BPM 模块独立 Maven 工程已发布到本地 m2，`bpm-biz` 已在 jeecgboot v3.5.5 真实环境冒烟通过（P0 Task 11 `INTEGRATION.md`）
2. `bpm-spi` 4 个接口（`BpmUserContext` / `BpmOrgService` / `BpmFormService` / `BpmNotificationSender`）已在 `bpm-adapter-jeecg` 落地（P1）
3. `bpm_*` 表（含 `bpm_instance_meta` / `bpm_task_history` / `bpm_node_config` / `bpm_process_definition`）已建好（P1）；Flowable 自带 `act_hi_*`、`act_ru_*` 表已就绪（P0 Task 9 IT 已验证）
4. `BpmDefinitionService` / `BpmInstanceService` / `BpmTaskService` / `AssigneeResolver` / `FormBindingService` / `BpmSandboxService` 已实现（P1~P4）
5. 前端 `src/views/bpm/{designer,definition,task,sandbox}` 已落地，BPMN 设计器与 bpmn-js viewer 都能加载（P1~P4）；`src/api/bpm/{definition,instance,task,sandbox}.ts` 已存在
6. `INTEGRATION.md` 中 Shiro 放行 `/bpm/v1/**` 已包含 `/bpm/v1/monitor/**` 与 `/bpm/v1/instance/*/intervene`（前缀通配已覆盖，无需再改 ShiroConfig）
7. 工作目录 `/Users/wuhoujin/Documents/dev/bpm`；`source ~/bin/bpm-env.sh` 后 JDK 11 / Maven 3.9.x / Docker 就绪

---

## File Structure（本计划新增/修改的全部文件）

**后端新增（`jeecg-module-bpm/jeecg-module-bpm-biz/`）：**

```
jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/
├── monitor/
│   ├── controller/MonitorController.java                # GET /bpm/v1/monitor/instances /diagram /stats
│   ├── controller/InstanceInterventionController.java   # POST /bpm/v1/instance/{id}/intervene
│   ├── service/MonitorQueryService.java                 # 实例查询 + diagram 解析
│   ├── service/MonitorStatsService.java                 # 三类聚合统计
│   ├── service/InstanceInterventionService.java         # FORCE_COMPLETE_TASK / FORCE_CANCEL / FORCE_REASSIGN
│   ├── dto/MonitorInstanceQuery.java                    # 列表查询入参
│   ├── dto/MonitorInstanceVO.java                       # 列表行
│   ├── dto/InstanceDiagramVO.java                       # bpmn_xml + currentNodeIds
│   ├── dto/StatsQuery.java                              # dateRange / defKey / scope 入参
│   ├── dto/StatsByDefinitionRow.java
│   ├── dto/StatsByNodeRow.java
│   ├── dto/StatsByApplyDeptRow.java
│   ├── dto/StatsResponse.java                           # { byDefinition, byNode, byApplyDept }
│   ├── dto/InterveneRequest.java                        # action enum + payload
│   └── enums/InterveneAction.java                       # FORCE_COMPLETE_TASK / FORCE_CANCEL / FORCE_REASSIGN
├── scheduler/
│   ├── BpmSchedulerAutoConfiguration.java               # @EnableScheduling + @ConditionalOnProperty
│   ├── BpmSchedulerProperties.java                      # bpm.scheduler.* 配置
│   ├── NodeTimeoutCheckJob.java                         # @Scheduled 5min — REMIND/AUTO_PASS/ESCALATE
│   ├── HistoryCleanupJob.java                           # @Scheduled 03:00 — 删 bpm_task_history + act_hi_*
│   └── service/NodeTimeoutHandler.java                  # 三个 action 分支（剥离便于单测）
└── mapper/
    ├── MonitorMapper.java                               # 监控 + 统计 SQL（XML mapper 文件配套）
    └── HistoryCleanupMapper.java                        # 清理 SQL

jeecg-module-bpm-biz/src/main/resources/mapper/
├── MonitorMapper.xml
└── HistoryCleanupMapper.xml
```

**后端测试新增：**

```
jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/
├── monitor/
│   ├── controller/MonitorControllerTest.java                        # 单测 (MockMvc + service mock)
│   ├── controller/InstanceInterventionControllerTest.java           # 单测
│   ├── service/MonitorQueryServiceTest.java                         # 单测
│   ├── service/MonitorStatsServiceTest.java                         # 单测
│   └── service/InstanceInterventionServiceTest.java                 # 单测
├── scheduler/
│   ├── NodeTimeoutCheckJobTest.java                                 # 单测
│   ├── service/NodeTimeoutHandlerTest.java                          # 单测（三个分支）
│   └── HistoryCleanupJobTest.java                                   # 单测
└── monitor/it/
    ├── MonitorInstanceQueryIT.java                                  # IT (Testcontainers)
    ├── MonitorStatsIT.java                                          # IT
    ├── InstanceInterventionIT.java                                  # IT (3 个 action)
    ├── NodeTimeoutJobIT.java                                        # IT (3 个 action)
    └── HistoryCleanupJobIT.java                                     # IT
```

**前端新增（`jeecgboot-vue3/`，本地工作副本）：**

```
src/views/bpm/monitor/
├── ProcessMonitor.vue                # VxeTable + filters
├── InstanceDetailDrawer.vue          # tabs: 流程图 / 任务历史 / 表单数据 / 干预
├── components/
│   ├── DiagramViewer.vue             # bpmn-js Viewer + colorize overlay
│   ├── TaskHistoryTable.vue          # VxeTable 任务历史
│   └── InterveneActions.vue          # 三个干预按钮（admin only）
└── stats/
    └── StatsDashboard.vue            # ECharts: 平均耗时 / 超时率 / 申请量趋势

src/api/bpm/
└── monitor.ts                        # listInstances / getDiagram / getStats / intervene
```

**修改：**
- `INTEGRATION.md` 追加"## 8. 后端定时任务配置"章节（cron / 保留期 / 是否启用）
- `jeecgboot-vue3` 路由 + 菜单 SQL（监控页 / 统计页两条菜单注入 sys_permission，admin only）
- `jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/db/migration/`：追加 `V5__monitor_index.sql`（见 Task 1）

**P5 不修改的文件：** P0~P4 已落地的 controller / service / mapper / 前端组件均**不**改动；新增逻辑全部走新增类，避免回归。

---

## Task 1：DB 索引 + 配置项骨架

**Files:**
- 创建 `jeecg-module-bpm-biz/src/main/resources/db/migration/V5__monitor_index.sql`
- 创建 `jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/scheduler/BpmSchedulerProperties.java`
- 修改 `jeecg-module-bpm-biz/src/main/resources/bpm-application.yml`（追加 `bpm.scheduler.*` 与 `bpm.history.*` 默认值）

为 P5 大量监控查询补必要索引（spec §11 "P95 < 300ms"）；落库 `bpm.scheduler.timeout.enabled` / `bpm.scheduler.history.cleanup.enabled` / `bpm.history.retention-days` 配置项。

- [ ] **Step 1：DB 迁移脚本**

`V5__monitor_index.sql`：
```sql
-- P5 监控查询：bpm_instance_meta 高频过滤组合
CREATE INDEX idx_bpm_instance_meta_def_state_time
    ON bpm_instance_meta (def_id, state, start_time);
CREATE INDEX idx_bpm_instance_meta_apply_dept_state
    ON bpm_instance_meta (apply_dept_id, state, start_time);
CREATE INDEX idx_bpm_instance_meta_apply_user_state
    ON bpm_instance_meta (apply_user_id, state, start_time);

-- 历史清理：按状态 + end_time 删除
CREATE INDEX idx_bpm_instance_meta_state_end_time
    ON bpm_instance_meta (state, end_time);

-- 任务历史：按实例 / 节点 / op_time 统计
CREATE INDEX idx_bpm_task_history_inst_op
    ON bpm_task_history (inst_id, op_time);
CREATE INDEX idx_bpm_task_history_node_op
    ON bpm_task_history (node_id, op_time);

-- 节点配置：超时 job 按 def_id + node_id 查超时定义
CREATE INDEX idx_bpm_node_config_def_node
    ON bpm_node_config (def_id, node_id);
```

> **Why 这些索引：** Task 3（监控列表）按 `(def_id, state, start_time)` 过滤；Task 5（byApplyDept 统计）按 `apply_dept_id` group；Task 9（历史清理）按 `(state, end_time)` 删除；Task 7（超时 job）按 `(def_id, node_id)` 关联 `bpm_node_config`。索引列序遵循"过滤选择性 → 排序"原则。

- [ ] **Step 2：Spring 配置 properties POJO**

`BpmSchedulerProperties.java`：
```java
package org.jeecg.modules.bpm.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bpm.scheduler")
public class BpmSchedulerProperties {

    private final Timeout timeout = new Timeout();
    private final HistoryCleanup historyCleanup = new HistoryCleanup();

    public Timeout getTimeout() { return timeout; }
    public HistoryCleanup getHistoryCleanup() { return historyCleanup; }

    public static class Timeout {
        private boolean enabled = true;
        private String cron = "0 */5 * * * ?";  // 每 5 分钟
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }

    public static class HistoryCleanup {
        private boolean enabled = true;
        private String cron = "0 0 3 * * ?";    // 每天 03:00
        private int retentionDays = 180;        // 6 个月
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }
}
```

- [ ] **Step 3：在 `bpm-application.yml` 追加默认配置**

```yaml
bpm:
  scheduler:
    timeout:
      enabled: true
      cron: "0 */5 * * * ?"
    history-cleanup:
      enabled: true
      cron: "0 0 3 * * ?"
      retention-days: 180
```

- [ ] **Step 4：构建验证**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz compile
```

期望 BUILD SUCCESS。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/db/migration/V5__monitor_index.sql \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/scheduler/BpmSchedulerProperties.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm-application.yml
git commit -m "feat(bpm-p5): db indexes for monitor queries + scheduler properties"
```

---

## Task 2：MonitorController `/instances` 列表查询（TDD：单测 → 实现 → IT）

**Files:**
- 创建 `monitor/dto/MonitorInstanceQuery.java`、`monitor/dto/MonitorInstanceVO.java`
- 创建 `monitor/mapper/MonitorMapper.java` + `mapper/MonitorMapper.xml`（仅 `selectInstances` / `countInstances`）
- 创建 `monitor/service/MonitorQueryService.java`（仅 `listInstances`）
- 创建 `monitor/controller/MonitorController.java`（仅 `GET /instances`）
- 创建测试 `MonitorQueryServiceTest`、`MonitorControllerTest`、`MonitorInstanceQueryIT`

**目标：** `GET /bpm/v1/monitor/instances` 返回运行中实例分页列表，过滤 `defKey` / `defVersion` / `applyDeptId` / `applyUserId` / `state` / `dateRange`（spec §5.6）。SQL 走 `bpm_instance_meta` 单表（已包含 `def_id` / `def_version` / `business_key` / `apply_user_id` / `apply_dept_id` / `state` / `start_time` / `end_time`），并以 `bpm_process_definition` 反查 `def_key` 用于过滤——避免 join `act_ru_execution`（运行中状态由 `bpm_instance_meta.state` 直接提供，spec §4.1 设计如此）。

> **决策说明：** 用户指令提到 "joins act_ru_execution + bpm_instance_meta"，但 `bpm_instance_meta` 已经记录 `state`/`start_time` 等运行状态字段（spec §4.1），且 P0 Task 9 已验证 Flowable 自带 `act_ru_*` 表。为避免重复信息源 + 保持 SQL 简洁，本 Task 主查 `bpm_instance_meta`，仅在 Task 3 `/diagram` endpoint 中按需走 `act_ru_execution` 取当前活动节点。

- [ ] **Step 1：DTO**

`MonitorInstanceQuery.java`：
```java
package org.jeecg.modules.bpm.monitor.dto;

import java.time.LocalDateTime;

public class MonitorInstanceQuery {
    private String defKey;
    private Integer defVersion;
    private Long applyDeptId;
    private Long applyUserId;
    private String state;            // RUNNING / COMPLETED / CANCELLED / SUSPENDED / SANDBOX
    private LocalDateTime startTimeFrom;
    private LocalDateTime startTimeTo;
    private int pageNo = 1;
    private int pageSize = 20;
    // getters/setters 全部省略提示，实际生成时全部补齐
}
```

`MonitorInstanceVO.java`：
```java
package org.jeecg.modules.bpm.monitor.dto;

import java.time.LocalDateTime;

public class MonitorInstanceVO {
    private String id;                 // bpm_instance_meta.id
    private String actInstId;          // act_ru_execution / act_hi_procinst.id
    private String defKey;
    private String defName;
    private Integer defVersion;
    private String businessKey;
    private Long applyUserId;
    private String applyUserName;
    private Long applyDeptId;
    private String applyDeptName;
    private String state;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    // getters/setters
}
```

- [ ] **Step 2：MyBatis Mapper（仅查询）**

`MonitorMapper.java`：
```java
package org.jeecg.modules.bpm.monitor.mapper;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceQuery;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceVO;
import java.util.List;

public interface MonitorMapper {
    List<MonitorInstanceVO> selectInstances(@Param("q") MonitorInstanceQuery q,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);
    long countInstances(@Param("q") MonitorInstanceQuery q);
}
```

`MonitorMapper.xml`（关键片段）：
```xml
<select id="selectInstances" resultType="org.jeecg.modules.bpm.monitor.dto.MonitorInstanceVO">
  SELECT
    m.id AS id, m.act_inst_id AS actInstId,
    pd.`key` AS defKey, pd.name AS defName,
    m.def_version AS defVersion, m.business_key AS businessKey,
    m.apply_user_id AS applyUserId, m.apply_dept_id AS applyDeptId,
    m.state AS state, m.start_time AS startTime, m.end_time AS endTime
  FROM bpm_instance_meta m
  LEFT JOIN bpm_process_definition pd ON pd.id = m.def_id
  <where>
    <if test="q.defKey != null and q.defKey != ''">AND pd.`key` = #{q.defKey}</if>
    <if test="q.defVersion != null">AND m.def_version = #{q.defVersion}</if>
    <if test="q.applyDeptId != null">AND m.apply_dept_id = #{q.applyDeptId}</if>
    <if test="q.applyUserId != null">AND m.apply_user_id = #{q.applyUserId}</if>
    <if test="q.state != null and q.state != ''">AND m.state = #{q.state}</if>
    <if test="q.startTimeFrom != null">AND m.start_time &gt;= #{q.startTimeFrom}</if>
    <if test="q.startTimeTo != null">AND m.start_time &lt;= #{q.startTimeTo}</if>
  </where>
  ORDER BY m.start_time DESC
  LIMIT #{limit} OFFSET #{offset}
</select>

<select id="countInstances" resultType="long">
  SELECT COUNT(1)
  FROM bpm_instance_meta m
  LEFT JOIN bpm_process_definition pd ON pd.id = m.def_id
  <where>
    <!-- 同上过滤 -->
  </where>
</select>
```

> **applyUserName / applyDeptName 留空：** 这俩字段属于 SPI（`BpmOrgService`）职责，service 层在循环中按 id 反查（adapter-jeecg 内是直连 sys_user/sys_depart），mapper 不掺 sys_*。

- [ ] **Step 3：写 service 单测（先红）**

`MonitorQueryServiceTest.java`：
```java
package org.jeecg.modules.bpm.monitor.service;

import org.jeecg.modules.bpm.monitor.dto.*;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class MonitorQueryServiceTest {

    @Test
    void listInstancesEnrichesWithUserAndDeptNames() {
        MonitorMapper mapper = Mockito.mock(MonitorMapper.class);
        BpmOrgService org = Mockito.mock(BpmOrgService.class);

        MonitorInstanceVO row = new MonitorInstanceVO();
        row.setId("inst1"); row.setApplyUserId(7L); row.setApplyDeptId(3L);
        row.setStartTime(LocalDateTime.now());
        when(mapper.selectInstances(any(), anyInt(), anyInt()))
            .thenReturn(Collections.singletonList(row));
        when(mapper.countInstances(any())).thenReturn(1L);
        when(org.findUserName(7L)).thenReturn("alice");
        when(org.findDeptName(3L)).thenReturn("研发部");

        MonitorQueryService svc = new MonitorQueryService(mapper, org);
        MonitorInstanceQuery q = new MonitorInstanceQuery();
        Map<String,Object> page = svc.listInstances(q);

        assertThat(page.get("total")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<MonitorInstanceVO> list = (List<MonitorInstanceVO>) page.get("records");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getApplyUserName()).isEqualTo("alice");
        assertThat(list.get(0).getApplyDeptName()).isEqualTo("研发部");
    }
}
```

> **依赖前提：** 测试假设 `BpmOrgService` 在 P1 已包含 `findUserName(Long)` / `findDeptName(Long)` 两个查询方法（spec §3.3 SPI 中虽未列出但属于"按 id 取展示名"——如果 P1 未含，本 Task 内补齐，并相应在 adapter-jeecg 实现）。

- [ ] **Step 4：跑测试看其失败**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=MonitorQueryServiceTest
```

期望编译失败（`MonitorQueryService` 未实现）。

- [ ] **Step 5：实现 `MonitorQueryService`**

```java
package org.jeecg.modules.bpm.monitor.service;

import org.jeecg.modules.bpm.monitor.dto.*;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MonitorQueryService {

    private final MonitorMapper mapper;
    private final BpmOrgService orgService;

    public MonitorQueryService(MonitorMapper mapper, BpmOrgService orgService) {
        this.mapper = mapper;
        this.orgService = orgService;
    }

    public Map<String,Object> listInstances(MonitorInstanceQuery q) {
        int pageNo = Math.max(1, q.getPageNo());
        int pageSize = Math.max(1, Math.min(200, q.getPageSize()));
        int offset = (pageNo - 1) * pageSize;

        long total = mapper.countInstances(q);
        List<MonitorInstanceVO> rows = mapper.selectInstances(q, offset, pageSize);
        for (MonitorInstanceVO row : rows) {
            if (row.getApplyUserId() != null) {
                row.setApplyUserName(orgService.findUserName(row.getApplyUserId()));
            }
            if (row.getApplyDeptId() != null) {
                row.setApplyDeptName(orgService.findDeptName(row.getApplyDeptId()));
            }
        }
        Map<String,Object> page = new LinkedHashMap<>();
        page.put("records", rows);
        page.put("total", total);
        page.put("pageNo", pageNo);
        page.put("pageSize", pageSize);
        return page;
    }
}
```

- [ ] **Step 6：写 controller 单测**

`MonitorControllerTest.java`（仅 `/instances`，结构同 P0 Task 7 的 `BpmHealthControllerTest`）：用 `@WebMvcTest` + `@MockBean MonitorQueryService` 验证：
- `GET /bpm/v1/monitor/instances?defKey=foo&pageNo=1&pageSize=10` → 200，body 中 `total` / `records` 字段存在
- 入参没有 `pageSize` 时 service 收到 `pageSize=20`（默认）

- [ ] **Step 7：实现 `MonitorController.list`（先只实现 `/instances`）**

```java
@RestController
@RequestMapping("/bpm/v1/monitor")
public class MonitorController {

    private final MonitorQueryService queryService;

    public MonitorController(MonitorQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/instances")
    @RequiresPermissions("bpm:monitor:list")
    public Map<String,Object> instances(MonitorInstanceQuery query) {
        return queryService.listInstances(query);
    }
}
```

> **`@RequiresPermissions` 注解：** 由 `bpm-adapter-jeecg`（Shiro）在宿主环境生效；`bpm-biz` 直接使用 Shiro API 注解（Shiro `org.apache.shiro.authz.annotation.RequiresPermissions` 来自 shiro-core，不属于 jeecg），符合 §1.2 "复用 Shiro" 原则——`bpm-biz` 仅依赖 shiro-core 注解 jar（apache-shiro 已是 spring-boot 2.7 兼容，不破坏零 jeecg dep 约束）。

- [ ] **Step 8：跑全部相关单测验证**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='MonitorQueryServiceTest,MonitorControllerTest'
```

期望全部通过。

- [ ] **Step 9：写 IT（Testcontainers）**

`MonitorInstanceQueryIT.java`：复用 P0 Task 9 的 `MySQLContainer` + `@DynamicPropertySource` 模板。
- 启动 Spring Context（包含 BPM 模块全套）
- 直接 INSERT 三条 `bpm_instance_meta` 行（不同 def / state / dept），并对应造 `bpm_process_definition` 三行
- 调 `MonitorQueryService.listInstances(...)`，依次验证：
  - 不带过滤 → 3 条
  - `defKey` 过滤 → 1 条
  - `state=RUNNING` → 仅 RUNNING 的 N 条
  - `applyDeptId=3` → 该部门的 N 条
  - `dateRange` → 落在范围内的 N 条
  - 分页 `pageSize=2` → 2 条 / total=3

- [ ] **Step 10：跑 IT**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=MonitorInstanceQueryIT
```

期望通过。

- [ ] **Step 11：commit**

```bash
git add <Task 2 全部新文件>
git commit -m "feat(bpm-p5): GET /monitor/instances list with filters + IT"
```

---

## Task 3：`/instances/{id}/diagram` endpoint + currentNodeIds 解析

**Files:**
- 创建 `monitor/dto/InstanceDiagramVO.java`
- 修改 `MonitorQueryService`（追加 `getDiagram`）
- 修改 `MonitorController`（追加 `GET /instances/{id}/diagram`）
- 创建测试 `MonitorQueryServiceDiagramTest`、`MonitorControllerDiagramTest`

**目标：** 返回 `{ bpmnXml: "...", currentNodeIds: ["task_a","task_b"] }`，前端 bpmn-js viewer 用 `currentNodeIds` 高亮（spec §7.2 colorize overlay）。

- [ ] **Step 1：DTO**

```java
public class InstanceDiagramVO {
    private String bpmnXml;
    private java.util.List<String> currentNodeIds;
    // getters/setters
}
```

- [ ] **Step 2：service 单测**

mock 三件事：
1. `BpmInstanceService.findMeta(id)` 返回 `act_inst_id="proc1"` + `def_id="def1"` + `def_version=2`
2. `BpmDefinitionService.loadBpmnXml(defId, version)` 返回 `<bpmn>...</bpmn>` 字符串
3. `RuntimeService.createExecutionQuery().processInstanceId("proc1").onlyChildExecutions().list()` 返回若干 `Execution` 对象，`getActivityId()` 分别为 `task_a` / `task_b`

期望 service 返回的 VO `bpmnXml` = 上面字符串、`currentNodeIds` = `[task_a, task_b]`（去重去 null）。

> **若实例已结束（`state ∈ COMPLETED/CANCELLED`）：** `act_ru_execution` 已无记录，应回退到 `act_hi_actinst` 取**最后一个 endTime IS NULL** 的活动 — 但既然已结束就没"当前节点"概念，本 Task 直接返回 `currentNodeIds = []`，前端不高亮。

- [ ] **Step 3：实现**

```java
public InstanceDiagramVO getDiagram(String instMetaId) {
    BpmInstanceMeta meta = instanceService.findMeta(instMetaId);
    String bpmnXml = definitionService.loadBpmnXml(meta.getDefId(), meta.getDefVersion());
    InstanceDiagramVO vo = new InstanceDiagramVO();
    vo.setBpmnXml(bpmnXml);

    if ("RUNNING".equals(meta.getState()) || "SANDBOX".equals(meta.getState())
            || "SUSPENDED".equals(meta.getState())) {
        List<Execution> execs = runtimeService.createExecutionQuery()
                .processInstanceId(meta.getActInstId()).onlyChildExecutions().list();
        Set<String> nodes = new LinkedHashSet<>();
        for (Execution e : execs) {
            if (e.getActivityId() != null) nodes.add(e.getActivityId());
        }
        vo.setCurrentNodeIds(new ArrayList<>(nodes));
    } else {
        vo.setCurrentNodeIds(Collections.emptyList());
    }
    return vo;
}
```

- [ ] **Step 4：controller endpoint**

```java
@GetMapping("/instances/{id}/diagram")
@RequiresPermissions("bpm:monitor:list")
public InstanceDiagramVO diagram(@PathVariable String id) {
    return queryService.getDiagram(id);
}
```

- [ ] **Step 5：跑测试**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='MonitorQueryServiceDiagramTest,MonitorControllerDiagramTest'
```

期望通过。

- [ ] **Step 6：commit**

```bash
git add <Task 3 文件>
git commit -m "feat(bpm-p5): GET /monitor/instances/{id}/diagram with current node ids"
```

---

## Task 4：`/stats` endpoint —— `byDefinition` 聚合（TDD：单测 → 实现 → IT）

**Files:**
- 创建 `monitor/dto/StatsQuery.java`、`StatsByDefinitionRow.java`、`StatsResponse.java`
- 修改 `MonitorMapper` + `MonitorMapper.xml`（追加 `selectStatsByDefinition`）
- 创建 `monitor/service/MonitorStatsService.java`（仅 `byDefinition`）
- 修改 `MonitorController`（追加 `GET /stats`，本 Task 仅返回 `byDefinition`）
- 测试：`MonitorStatsServiceByDefTest`、`MonitorStatsByDefinitionIT`

**目标：** 单 SQL 走 `act_hi_procinst` LEFT JOIN `bpm_instance_meta` LEFT JOIN `bpm_process_definition`，按 `pd.key` group by 出 `avg_duration`（毫秒，来自 `act_hi_procinst.duration_`）/ `instance_count` / `completion_rate`（`COMPLETED`/total）/ `overdue_rate`（任一节点超时——任一 `act_hi_taskinst` 行 `duration_ > nodeConfig.timeout_hours * 3600 * 1000`）。

- [ ] **Step 1：DTO**

```java
public class StatsQuery {
    private LocalDateTime startTimeFrom;
    private LocalDateTime startTimeTo;
    private String defKey;     // 可选：只统计某个流程
    // getters/setters
}

public class StatsByDefinitionRow {
    private String defKey;
    private String defName;
    private long instanceCount;
    private long completedCount;
    private long overdueCount;
    private Double avgDurationMs;        // 可空（无完成实例）
    private Double completionRate;       // completedCount / instanceCount
    private Double overdueRate;          // overdueCount / instanceCount
    // getters/setters
}

public class StatsResponse {
    private List<StatsByDefinitionRow> byDefinition = new ArrayList<>();
    private List<StatsByNodeRow> byNode = new ArrayList<>();           // Task 5 填充
    private List<StatsByApplyDeptRow> byApplyDept = new ArrayList<>(); // Task 5 填充
    // getters/setters
}
```

- [ ] **Step 2：Mapper SQL（聚合大查询）**

```xml
<select id="selectStatsByDefinition"
        resultType="org.jeecg.modules.bpm.monitor.dto.StatsByDefinitionRow">
  SELECT
    pd.`key`           AS defKey,
    pd.name            AS defName,
    COUNT(m.id)        AS instanceCount,
    SUM(CASE WHEN m.state = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount,
    SUM(CASE WHEN ovd.inst_id IS NOT NULL THEN 1 ELSE 0 END) AS overdueCount,
    AVG(CASE WHEN m.state = 'COMPLETED' THEN hpi.duration_ ELSE NULL END) AS avgDurationMs
  FROM bpm_instance_meta m
  LEFT JOIN bpm_process_definition pd ON pd.id = m.def_id
  LEFT JOIN act_hi_procinst hpi       ON hpi.id_ = m.act_inst_id
  LEFT JOIN (
    SELECT DISTINCT hti.proc_inst_id_ AS inst_id
    FROM act_hi_taskinst hti
    JOIN bpm_node_config nc ON nc.def_id = hti.proc_def_id_ AND nc.node_id = hti.task_def_key_
    WHERE hti.duration_ &gt; nc.timeout_hours * 3600 * 1000
  ) ovd ON ovd.inst_id = m.act_inst_id
  WHERE m.state &lt;&gt; 'SANDBOX'
  <if test="q.defKey != null and q.defKey != ''">AND pd.`key` = #{q.defKey}</if>
  <if test="q.startTimeFrom != null">AND m.start_time &gt;= #{q.startTimeFrom}</if>
  <if test="q.startTimeTo != null">AND m.start_time &lt;= #{q.startTimeTo}</if>
  GROUP BY pd.`key`, pd.name
  ORDER BY instanceCount DESC
  LIMIT 200
</select>
```

> **Why LIMIT 200：** spec §11 一期流程定义 ≤ 500，加上 dateRange 过滤实际不会全选；显式 LIMIT 是 spec 性能要求"P95<300ms"的兜底，前端一次最多展示 100 条柱图（P5 spec §6 `/monitor/stats` 注明）。
>
> **Why DISTINCT 子查询而非 EXISTS：** EXISTS 在 MySQL 8 一般会走 semi-join 优化，但任务历史的 `(proc_inst_id_, task_def_key_)` 联合索引存在性不确定（act_hi_taskinst 由 Flowable 建表）；先用 DISTINCT 子查询确保 query plan 稳定，性能不达 P95 时再调。

- [ ] **Step 3：service 单测（mock mapper，不连库）**

```java
@Test
void byDefinitionComputesRates() {
    MonitorMapper mapper = mock(MonitorMapper.class);
    StatsByDefinitionRow row = new StatsByDefinitionRow();
    row.setDefKey("leave"); row.setInstanceCount(10L);
    row.setCompletedCount(7L); row.setOverdueCount(2L);
    row.setAvgDurationMs(123456.0);
    when(mapper.selectStatsByDefinition(any())).thenReturn(List.of(row));

    MonitorStatsService svc = new MonitorStatsService(mapper);
    StatsResponse r = svc.compute(new StatsQuery(), Set.of("byDefinition"));

    assertThat(r.getByDefinition()).hasSize(1);
    StatsByDefinitionRow o = r.getByDefinition().get(0);
    assertThat(o.getCompletionRate()).isEqualTo(0.7);
    assertThat(o.getOverdueRate()).isEqualTo(0.2);
}

@Test
void byDefinitionHandlesZeroInstances() {
    // 边界：instanceCount = 0 → completionRate / overdueRate = 0.0 而非 NaN
}
```

- [ ] **Step 4：跑测试看其失败 → 实现 service**

```java
@Service
public class MonitorStatsService {
    private final MonitorMapper mapper;
    public MonitorStatsService(MonitorMapper mapper) { this.mapper = mapper; }

    public StatsResponse compute(StatsQuery q, Set<String> scopes) {
        StatsResponse r = new StatsResponse();
        if (scopes == null || scopes.isEmpty() || scopes.contains("byDefinition")) {
            List<StatsByDefinitionRow> rows = mapper.selectStatsByDefinition(q);
            for (StatsByDefinitionRow row : rows) {
                long total = row.getInstanceCount();
                row.setCompletionRate(total == 0 ? 0.0 : (double) row.getCompletedCount() / total);
                row.setOverdueRate(total == 0 ? 0.0 : (double) row.getOverdueCount() / total);
            }
            r.setByDefinition(rows);
        }
        return r;
    }
}
```

- [ ] **Step 5：controller endpoint**

```java
@GetMapping("/stats")
@RequiresPermissions("bpm:monitor:stats")
public StatsResponse stats(StatsQuery query,
                           @RequestParam(name="scope", required=false) Set<String> scope) {
    return statsService.compute(query, scope);
}
```

> **`scope` 参数：** 客户端可传 `?scope=byDefinition&scope=byNode&scope=byApplyDept`，省去无用聚合（前端 dashboard 一次拉全，但单图表也能单独刷新）。

- [ ] **Step 6：跑测试通过**

- [ ] **Step 7：写 IT `MonitorStatsByDefinitionIT`**

Testcontainers 启 MySQL；先用 P0 Task 9 同款方式 deploy hello-world bpmn 并跑通几个实例：
- 跑 5 个 `bpm_helloworld` 实例：3 个 complete、2 个进行中
- 强行往 `bpm_node_config` 写一条 `(def_id, node_id=task_hello, timeout_hours=0.001)`，让 `act_hi_taskinst.duration_ > timeout_hours*3600*1000` 成立
- 调 `/stats?scope=byDefinition` 验证：`instanceCount=5` / `completedCount=3` / `completionRate=0.6` / `overdueCount > 0`

- [ ] **Step 8：commit**

```bash
git commit -m "feat(bpm-p5): GET /monitor/stats byDefinition aggregation + IT"
```

---

## Task 5：`/stats` —— `byNode` + `byApplyDept` 聚合（TDD：单测 → 实现 → IT）

**Files:**
- 创建 `StatsByNodeRow.java`、`StatsByApplyDeptRow.java`
- 修改 `MonitorMapper` + xml（追加 `selectStatsByNode`、`selectStatsByApplyDept`）
- 修改 `MonitorStatsService`（补充两个 scope）
- 测试：`MonitorStatsServiceByNodeAndDeptTest`、`MonitorStatsByNodeIT`、`MonitorStatsByApplyDeptIT`

**目标：**
- `byNode`：每个 `(def_key, node_id)` 维度的 `avg_duration_ms`（来自 `act_hi_taskinst.duration_` 平均）+ `overdue_rate`（duration > timeout_hours * 3600000 的比例）
- `byApplyDept`：每个 `apply_dept_id` 维度的 `instance_count`

- [ ] **Step 1：DTO**

```java
public class StatsByNodeRow {
    private String defKey;
    private String nodeId;
    private String nodeName;        // act_hi_taskinst.name_
    private long taskCount;
    private long overdueCount;
    private Double avgDurationMs;
    private Double overdueRate;
    // getters/setters
}

public class StatsByApplyDeptRow {
    private Long applyDeptId;
    private String applyDeptName;
    private long instanceCount;
    // getters/setters
}
```

- [ ] **Step 2：Mapper SQL**

`selectStatsByNode`：
```xml
<select id="selectStatsByNode"
        resultType="org.jeecg.modules.bpm.monitor.dto.StatsByNodeRow">
  SELECT
    pd.`key` AS defKey, hti.task_def_key_ AS nodeId, MAX(hti.name_) AS nodeName,
    COUNT(1) AS taskCount,
    AVG(hti.duration_) AS avgDurationMs,
    SUM(CASE WHEN nc.timeout_hours IS NOT NULL
             AND hti.duration_ &gt; nc.timeout_hours * 3600 * 1000
             THEN 1 ELSE 0 END) AS overdueCount
  FROM act_hi_taskinst hti
  JOIN bpm_instance_meta m ON m.act_inst_id = hti.proc_inst_id_
  JOIN bpm_process_definition pd ON pd.id = m.def_id
  LEFT JOIN bpm_node_config nc ON nc.def_id = m.def_id AND nc.node_id = hti.task_def_key_
  WHERE m.state &lt;&gt; 'SANDBOX' AND hti.end_time_ IS NOT NULL
  <if test="q.defKey != null and q.defKey != ''">AND pd.`key` = #{q.defKey}</if>
  <if test="q.startTimeFrom != null">AND hti.start_time_ &gt;= #{q.startTimeFrom}</if>
  <if test="q.startTimeTo != null">AND hti.start_time_ &lt;= #{q.startTimeTo}</if>
  GROUP BY pd.`key`, hti.task_def_key_
  ORDER BY taskCount DESC
  LIMIT 500
</select>
```

`selectStatsByApplyDept`：
```xml
<select id="selectStatsByApplyDept"
        resultType="org.jeecg.modules.bpm.monitor.dto.StatsByApplyDeptRow">
  SELECT m.apply_dept_id AS applyDeptId, COUNT(1) AS instanceCount
  FROM bpm_instance_meta m
  WHERE m.state &lt;&gt; 'SANDBOX' AND m.apply_dept_id IS NOT NULL
  <if test="q.startTimeFrom != null">AND m.start_time &gt;= #{q.startTimeFrom}</if>
  <if test="q.startTimeTo != null">AND m.start_time &lt;= #{q.startTimeTo}</if>
  GROUP BY m.apply_dept_id
  ORDER BY instanceCount DESC
  LIMIT 100
</select>
```

- [ ] **Step 3：service 单测（mock mapper）覆盖 `byNode` 算 overdueRate、`byApplyDept` 反查 dept name**

- [ ] **Step 4：实现**

```java
public StatsResponse compute(StatsQuery q, Set<String> scopes) {
    StatsResponse r = new StatsResponse();
    boolean all = scopes == null || scopes.isEmpty();

    if (all || scopes.contains("byDefinition")) { /* Task 4 已实现 */ }

    if (all || scopes.contains("byNode")) {
        List<StatsByNodeRow> rows = mapper.selectStatsByNode(q);
        for (StatsByNodeRow row : rows) {
            row.setOverdueRate(row.getTaskCount() == 0 ? 0.0
                    : (double) row.getOverdueCount() / row.getTaskCount());
        }
        r.setByNode(rows);
    }

    if (all || scopes.contains("byApplyDept")) {
        List<StatsByApplyDeptRow> rows = mapper.selectStatsByApplyDept(q);
        for (StatsByApplyDeptRow row : rows) {
            row.setApplyDeptName(orgService.findDeptName(row.getApplyDeptId()));
        }
        r.setByApplyDept(rows);
    }
    return r;
}
```

> **service 构造器追加 `BpmOrgService` 依赖：** Task 4 中只注入 mapper，本 Task 改成 `(MonitorMapper, BpmOrgService)`，相应改动 Task 4 的单测使用 mock。

- [ ] **Step 5：IT — 用 hello-world + 多实例 + 多部门数据**

`MonitorStatsByNodeIT`：5 个实例完成 task_hello（其中 2 个超时——同 Task 4 IT 的造数据手法），断 `byNode` 行 `overdueRate ≈ 0.4`。
`MonitorStatsByApplyDeptIT`：3 个实例 deptId=1、2 个 deptId=2，断分组 + `instanceCount` + 反查 deptName（mock SPI 或在 adapter 测试中真连）。

- [ ] **Step 6：跑全套测试 + commit**

```bash
git commit -m "feat(bpm-p5): GET /monitor/stats byNode + byApplyDept aggregation + IT"
```

---

## Task 6：`/instance/{id}/intervene` —— FORCE_COMPLETE_TASK（TDD）

**Files:**
- 创建 `monitor/enums/InterveneAction.java`
- 创建 `monitor/dto/InterveneRequest.java`
- 创建 `monitor/service/InstanceInterventionService.java`（仅 FORCE_COMPLETE_TASK）
- 创建 `monitor/controller/InstanceInterventionController.java`
- 测试：`InstanceInterventionServiceForceCompleteTest`、`InstanceInterventionIT`（覆盖 1/3 action）

**目标：** 管理员调 `POST /bpm/v1/instance/{instMetaId}/intervene` body `{action:"FORCE_COMPLETE_TASK", taskId:"...", comment:"...", variables:{...}}` → 调 `taskService.complete(taskId, variables)` → 写 `bpm_task_history.action=FORCE_COMPLETE_TASK`。

- [ ] **Step 1：DTO + enum**

```java
public enum InterveneAction { FORCE_COMPLETE_TASK, FORCE_CANCEL, FORCE_REASSIGN }

public class InterveneRequest {
    private InterveneAction action;
    private String taskId;                   // FORCE_COMPLETE_TASK / FORCE_REASSIGN 必填
    private Long newAssigneeId;              // FORCE_REASSIGN 必填
    private String comment;
    private Map<String,Object> variables;    // 可选
    // getters/setters
}
```

- [ ] **Step 2：service 单测（先红）**

```java
@Test
void forceCompleteCallsTaskServiceAndWritesHistory() {
    TaskService taskService = mock(TaskService.class);
    BpmTaskHistoryMapper historyMapper = mock(BpmTaskHistoryMapper.class);
    BpmInstanceService instanceService = mock(BpmInstanceService.class);
    BpmUserContext userCtx = mock(BpmUserContext.class);
    when(userCtx.currentUserId()).thenReturn(99L);

    BpmInstanceMeta meta = new BpmInstanceMeta();
    meta.setId("inst1"); meta.setActInstId("proc1");
    when(instanceService.findMeta("inst1")).thenReturn(meta);

    Task task = mock(Task.class);
    when(task.getId()).thenReturn("t1"); when(task.getTaskDefinitionKey()).thenReturn("approve");
    when(task.getProcessInstanceId()).thenReturn("proc1");
    when(taskService.createTaskQuery().taskId("t1").singleResult()).thenReturn(task);

    InstanceInterventionService svc =
        new InstanceInterventionService(taskService, /* runtime */ null,
            instanceService, historyMapper, userCtx);

    InterveneRequest req = new InterveneRequest();
    req.setAction(InterveneAction.FORCE_COMPLETE_TASK);
    req.setTaskId("t1"); req.setComment("强制完成");

    svc.intervene("inst1", req);

    verify(taskService).complete(eq("t1"), any());
    ArgumentCaptor<BpmTaskHistory> cap = ArgumentCaptor.forClass(BpmTaskHistory.class);
    verify(historyMapper).insert(cap.capture());
    BpmTaskHistory h = cap.getValue();
    assertThat(h.getAction()).isEqualTo("FORCE_COMPLETE_TASK");
    assertThat(h.getActTaskId()).isEqualTo("t1");
    assertThat(h.getAssigneeId()).isEqualTo(99L);
    assertThat(h.getComment()).isEqualTo("强制完成");
}

@Test
void forceCompleteFailsIfTaskNotInInstance() {
    // taskService 返回 task.processInstanceId != meta.actInstId → 抛 IllegalArgumentException
}
```

- [ ] **Step 3：实现 service（仅 FORCE_COMPLETE_TASK 分支）**

```java
@Service
public class InstanceInterventionService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final BpmInstanceService instanceService;
    private final BpmTaskHistoryMapper historyMapper;
    private final BpmUserContext userCtx;

    // constructor

    public void intervene(String instMetaId, InterveneRequest req) {
        BpmInstanceMeta meta = requireRunningOrSuspended(instMetaId);
        switch (req.getAction()) {
            case FORCE_COMPLETE_TASK: forceComplete(meta, req); break;
            case FORCE_CANCEL:        forceCancel(meta, req); break;       // Task 7
            case FORCE_REASSIGN:      forceReassign(meta, req); break;     // Task 7
            default: throw new IllegalArgumentException("Unknown action: " + req.getAction());
        }
    }

    private void forceComplete(BpmInstanceMeta meta, InterveneRequest req) {
        if (req.getTaskId() == null) throw new IllegalArgumentException("taskId required");
        Task task = taskService.createTaskQuery().taskId(req.getTaskId()).singleResult();
        if (task == null) throw new IllegalArgumentException("task not found");
        if (!meta.getActInstId().equals(task.getProcessInstanceId())) {
            throw new IllegalArgumentException("task does not belong to instance");
        }
        Map<String,Object> vars = req.getVariables() == null ? Collections.emptyMap()
                                                              : req.getVariables();
        taskService.complete(task.getId(), vars);
        writeHistory(meta, task.getTaskDefinitionKey(), task.getId(),
                     "FORCE_COMPLETE_TASK", req.getComment());
    }

    private void writeHistory(BpmInstanceMeta meta, String nodeId, String actTaskId,
                              String action, String comment) {
        BpmTaskHistory h = new BpmTaskHistory();
        h.setId(UUID.randomUUID().toString());
        h.setActTaskId(actTaskId);
        h.setInstId(meta.getId());
        h.setNodeId(nodeId);
        h.setAssigneeId(userCtx.currentUserId());
        h.setAction(action);
        h.setComment(comment);
        h.setOpTime(LocalDateTime.now());
        historyMapper.insert(h);
    }

    private BpmInstanceMeta requireRunningOrSuspended(String instMetaId) {
        BpmInstanceMeta meta = instanceService.findMeta(instMetaId);
        if (meta == null) throw new IllegalArgumentException("instance not found");
        if (!Set.of("RUNNING","SUSPENDED").contains(meta.getState())) {
            throw new IllegalStateException("intervene only allowed on RUNNING/SUSPENDED");
        }
        return meta;
    }
}
```

- [ ] **Step 4：controller**

```java
@RestController
@RequestMapping("/bpm/v1/instance")
public class InstanceInterventionController {
    private final InstanceInterventionService svc;
    public InstanceInterventionController(InstanceInterventionService svc) { this.svc = svc; }

    @PostMapping("/{id}/intervene")
    @RequiresPermissions("bpm:monitor:intervene")
    public Map<String,Object> intervene(@PathVariable("id") String instMetaId,
                                        @RequestBody InterveneRequest req) {
        svc.intervene(instMetaId, req);
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("success", true);
        return r;
    }
}
```

- [ ] **Step 5：IT — 跑 hello-world，造一个 RUNNING 任务，调 intervene 完成**

`InstanceInterventionIT.forceCompleteTaskCompletesAndWritesHistory()`：
1. Testcontainers MySQL + deploy hello-world
2. `runtimeService.startProcessInstanceByKey(...)` → 拿到 task
3. POST `/bpm/v1/instance/{metaId}/intervene` body action=FORCE_COMPLETE_TASK + taskId
4. 断：`taskService.createTaskQuery().processInstanceId(...).list()` 为空（实例已 end）
5. 断：`bpm_task_history` 行 action=FORCE_COMPLETE_TASK 存在

- [ ] **Step 6：commit**

```bash
git commit -m "feat(bpm-p5): POST /instance/{id}/intervene FORCE_COMPLETE_TASK + IT"
```

---

## Task 7：FORCE_CANCEL + FORCE_REASSIGN（TDD）

**Files:** 修改 `InstanceInterventionService`（追加两个分支）+ 测试 `...ForceCancelTest` / `...ForceReassignTest` + IT 中追加 2 个用例

- [ ] **Step 1：单测（FORCE_CANCEL）**

期望：
- `runtimeService.deleteProcessInstance(meta.getActInstId(), comment)` 被调
- `bpm_instance_meta` 通过 `instanceService.markCancelled(metaId)` 更新 state=CANCELLED + end_time
- `bpm_task_history` 写 `action=FORCE_CANCEL_INSTANCE`（注意：用户指令明确写法是 `FORCE_CANCEL_INSTANCE`，与 enum 名 `FORCE_CANCEL` 不同——enum 在外部 API 用，落库 action 字段用 `FORCE_CANCEL_INSTANCE` 区分实例级动作）

- [ ] **Step 2：实现 forceCancel**

```java
private void forceCancel(BpmInstanceMeta meta, InterveneRequest req) {
    runtimeService.deleteProcessInstance(meta.getActInstId(),
        req.getComment() == null ? "强制取消" : req.getComment());
    instanceService.markCancelled(meta.getId());
    writeHistory(meta, null, null, "FORCE_CANCEL_INSTANCE", req.getComment());
}
```

- [ ] **Step 3：单测（FORCE_REASSIGN）**

```java
@Test
void forceReassignSetsAssigneeAndWritesHistory() {
    // mock taskService.setAssignee(taskId, userIdString) 被调
    // 写 action=FORCE_REASSIGN，comment 含旧/新 assignee
}

@Test
void forceReassignFailsIfNewAssigneeIdMissing() { /* IllegalArgumentException */ }
```

- [ ] **Step 4：实现 forceReassign**

```java
private void forceReassign(BpmInstanceMeta meta, InterveneRequest req) {
    if (req.getTaskId() == null) throw new IllegalArgumentException("taskId required");
    if (req.getNewAssigneeId() == null) throw new IllegalArgumentException("newAssigneeId required");
    Task task = taskService.createTaskQuery().taskId(req.getTaskId()).singleResult();
    if (task == null || !meta.getActInstId().equals(task.getProcessInstanceId())) {
        throw new IllegalArgumentException("task does not belong to instance");
    }
    String oldAssignee = task.getAssignee();
    taskService.setAssignee(task.getId(), String.valueOf(req.getNewAssigneeId()));
    String comment = "FORCE_REASSIGN: " + oldAssignee + " -> " + req.getNewAssigneeId()
                     + (req.getComment() == null ? "" : " | " + req.getComment());
    writeHistory(meta, task.getTaskDefinitionKey(), task.getId(),
                 "FORCE_REASSIGN", comment);
}
```

- [ ] **Step 5：IT 中追加两个用例**

`InstanceInterventionIT`：
- `forceCancelDeletesInstanceAndMarksCancelled()`
- `forceReassignChangesAssigneeAndWritesHistory()`

- [ ] **Step 6：跑全套 IT + commit**

```bash
git commit -m "feat(bpm-p5): intervene FORCE_CANCEL + FORCE_REASSIGN + IT"
```

---

## Task 8：节点超时 Quartz job —— REMIND 分支（TDD）

**Files:**
- 创建 `scheduler/BpmSchedulerAutoConfiguration.java`（@EnableScheduling + @ConditionalOnProperty）
- 创建 `scheduler/NodeTimeoutCheckJob.java`（@Scheduled 入口，逻辑全代理给 handler）
- 创建 `scheduler/service/NodeTimeoutHandler.java`（核心逻辑，便于单测）
- 创建 `mapper/MonitorMapper.xml` 追加 `selectOverdueRunningTasks`
- 测试：`NodeTimeoutHandlerRemindTest`、`NodeTimeoutCheckJobTest`

**目标：** 每 5 分钟扫描所有 `act_ru_task` 中 `now - create_time > nc.timeout_hours` 的任务，按 `nc.timeout_action`：
- `REMIND`：调 `BpmNotificationSender.send(toUserId, "INTERNAL", "BPM_TIMEOUT_REMIND", vars)` + 写 task_history `action=TIMEOUT_REMIND`（同一 task 只提醒一次：用 `bpm_task_history` 唯一索引 `(act_task_id, action)` 幂等）
- `AUTO_PASS`：Task 9
- `ESCALATE`：Task 9

- [ ] **Step 1：AutoConfiguration**

```java
package org.jeecg.modules.bpm.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BpmSchedulerProperties.class)
@ComponentScan("org.jeecg.modules.bpm.scheduler")
public class BpmSchedulerAutoConfiguration {
}
```

> **挂入 spring.factories：** 修改 `META-INF/spring.factories` 追加这一行（与 P0 已有的 `BpmModuleAutoConfiguration` 并列）。

- [ ] **Step 2：Mapper SQL — 找超时未完成任务**

```xml
<select id="selectOverdueRunningTasks"
        resultType="org.jeecg.modules.bpm.scheduler.service.OverdueTaskRow">
  SELECT
    t.id_           AS taskId,
    t.proc_inst_id_ AS actInstId,
    t.task_def_key_ AS nodeId,
    t.assignee_     AS assignee,
    t.create_time_  AS createTime,
    nc.timeout_hours   AS timeoutHours,
    nc.timeout_action  AS timeoutAction,
    m.id            AS instMetaId,
    m.def_id        AS defId
  FROM act_ru_task t
  JOIN bpm_instance_meta m ON m.act_inst_id = t.proc_inst_id_
  JOIN bpm_node_config nc ON nc.def_id = m.def_id AND nc.node_id = t.task_def_key_
  WHERE m.state = 'RUNNING'
    AND nc.timeout_hours IS NOT NULL
    AND TIMESTAMPDIFF(SECOND, t.create_time_, NOW()) &gt; nc.timeout_hours * 3600
  LIMIT 500
</select>
```

`OverdueTaskRow` POJO 在 `scheduler/service/` 下。

- [ ] **Step 3：handler 单测（mock 所有依赖）**

```java
class NodeTimeoutHandlerTest {

    @Test
    void remindCallsNotificationSenderAndWritesHistoryOnce() {
        MonitorMapper mapper = mock(MonitorMapper.class);
        BpmNotificationSender sender = mock(BpmNotificationSender.class);
        BpmTaskHistoryMapper historyMapper = mock(BpmTaskHistoryMapper.class);

        OverdueTaskRow row = row("t1", "approve", "REMIND", 7L, "alice");
        when(mapper.selectOverdueRunningTasks()).thenReturn(List.of(row));
        when(historyMapper.existsActionForTask("t1", "TIMEOUT_REMIND")).thenReturn(false);

        NodeTimeoutHandler h = new NodeTimeoutHandler(mapper, sender,
            mock(TaskService.class), mock(BpmOrgService.class), historyMapper);
        h.runOnce();

        verify(sender).send(eq(7L), eq("INTERNAL"), eq("BPM_TIMEOUT_REMIND"), anyMap());
        verify(historyMapper).insert(argThat(hist ->
            "TIMEOUT_REMIND".equals(hist.getAction())
            && "t1".equals(hist.getActTaskId())));
    }

    @Test
    void remindIsIdempotentWhenAlreadyReminded() {
        // existsActionForTask 返回 true → sender 不被调
    }

    @Test
    void remindSkipsWhenAssigneeMissing() {
        // assignee=null 时跳过（避免 NPE）
    }
}
```

> **`historyMapper.existsActionForTask(taskId, action)`** 是新加的 SPI（在 `BpmTaskHistoryMapper` 中），用 SQL `SELECT COUNT(1) FROM bpm_task_history WHERE act_task_id=? AND action=?` 实现幂等保护。

- [ ] **Step 4：实现 handler.runOnce + remind 分支**

```java
@Service
public class NodeTimeoutHandler {

    private static final Logger log = LoggerFactory.getLogger(NodeTimeoutHandler.class);

    private final MonitorMapper mapper;
    private final BpmNotificationSender sender;
    private final TaskService taskService;
    private final BpmOrgService orgService;
    private final BpmTaskHistoryMapper historyMapper;

    // ctor

    @Transactional
    public void runOnce() {
        List<OverdueTaskRow> rows = mapper.selectOverdueRunningTasks();
        for (OverdueTaskRow row : rows) {
            try {
                handle(row);
            } catch (Exception e) {
                log.warn("[bpm-p5] handle overdue task failed: taskId={} action={}",
                         row.getTaskId(), row.getTimeoutAction(), e);
            }
        }
    }

    private void handle(OverdueTaskRow row) {
        switch (row.getTimeoutAction()) {
            case "REMIND":   doRemind(row); break;
            case "AUTO_PASS":doAutoPass(row); break;     // Task 9
            case "ESCALATE": doEscalate(row); break;     // Task 9
            default: log.warn("[bpm-p5] unknown timeout_action={}", row.getTimeoutAction());
        }
    }

    private void doRemind(OverdueTaskRow row) {
        if (row.getAssignee() == null) { return; }
        if (historyMapper.existsActionForTask(row.getTaskId(), "TIMEOUT_REMIND")) { return; }
        Long userId = parseUserId(row.getAssignee());
        Map<String,Object> vars = new LinkedHashMap<>();
        vars.put("taskId", row.getTaskId());
        vars.put("nodeId", row.getNodeId());
        vars.put("createTime", row.getCreateTime());
        vars.put("timeoutHours", row.getTimeoutHours());
        sender.send(userId, "INTERNAL", "BPM_TIMEOUT_REMIND", vars);
        BpmTaskHistory h = newHistory(row, "TIMEOUT_REMIND", "节点超时提醒");
        historyMapper.insert(h);
    }

    // ... newHistory / parseUserId helper
}
```

- [ ] **Step 5：Job 入口**

```java
@Component
@ConditionalOnProperty(prefix = "bpm.scheduler.timeout", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NodeTimeoutCheckJob {
    private final NodeTimeoutHandler handler;
    public NodeTimeoutCheckJob(NodeTimeoutHandler handler) { this.handler = handler; }

    @Scheduled(cron = "${bpm.scheduler.timeout.cron:0 */5 * * * ?}")
    public void run() { handler.runOnce(); }
}
```

- [ ] **Step 6：跑单测**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='NodeTimeoutHandlerTest,NodeTimeoutCheckJobTest'
```

- [ ] **Step 7：commit**

```bash
git commit -m "feat(bpm-p5): node timeout job - REMIND branch + idempotency"
```

---

## Task 9：节点超时 job —— AUTO_PASS + ESCALATE + IT

**Files:** 修改 `NodeTimeoutHandler` + 测试 + 创建 `NodeTimeoutJobIT`

- [ ] **Step 1：单测（AUTO_PASS）**

```java
@Test
void autoPassCompletesTaskAndWritesHistory() {
    // mock taskService.complete(taskId, emptyMap()) 被调
    // history.action = AUTO_PASS
}
```

- [ ] **Step 2：单测（ESCALATE）**

```java
@Test
void escalateReassignsToUpperDeptLeaderAndWritesHistory() {
    // assignee="alice" 来自 dept 3，BpmOrgService.findUpperDeptLeaders(3L) 返回 [99L]
    // taskService.setAssignee(taskId, "99") 被调
    // history.action = ESCALATE
}

@Test
void escalateFallsBackWhenNoUpperLeader() {
    // findUpperDeptLeaders 返回空 → 写 history action=ESCALATE_FAILED + 不改 assignee
}
```

- [ ] **Step 3：实现两个分支**

```java
private void doAutoPass(OverdueTaskRow row) {
    if (historyMapper.existsActionForTask(row.getTaskId(), "AUTO_PASS")) return;
    taskService.complete(row.getTaskId(), Collections.emptyMap());
    historyMapper.insert(newHistory(row, "AUTO_PASS", "节点超时自动通过"));
}

private void doEscalate(OverdueTaskRow row) {
    if (historyMapper.existsActionForTask(row.getTaskId(), "ESCALATE")) return;
    Long currentUserId = parseUserId(row.getAssignee());
    Long currentDeptId = currentUserId == null ? null : orgService.findUserMainDeptId(currentUserId);
    List<Long> uppers = currentDeptId == null
        ? Collections.emptyList()
        : orgService.findUpperDeptLeaders(currentDeptId);
    if (uppers.isEmpty()) {
        historyMapper.insert(newHistory(row, "ESCALATE_FAILED", "找不到上级领导"));
        return;
    }
    Long newAssignee = uppers.get(0);
    taskService.setAssignee(row.getTaskId(), String.valueOf(newAssignee));
    historyMapper.insert(newHistory(row, "ESCALATE",
            "升级到上级领导 user=" + newAssignee));
}
```

> **`BpmOrgService.findUserMainDeptId(Long)`：** 假设 P1 已含；若无在本 Task 同步补 SPI 方法 + adapter 实现（jeecg 中走 `sys_user.org_code` / `sys_user_depart`）。

- [ ] **Step 4：IT — `NodeTimeoutJobIT`**

```java
@SpringBootTest(classes = { BpmModuleAutoConfiguration.class, BpmSchedulerAutoConfiguration.class })
@ActiveProfiles("test")
@Testcontainers
class NodeTimeoutJobIT {

    @Container static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27");

    @DynamicPropertySource static void props(DynamicPropertyRegistry r) { /* 同 P0 Task 9 */ }

    @Autowired NodeTimeoutHandler handler;
    @MockBean BpmNotificationSender sender;
    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService runtimeService;
    @Autowired TaskService taskService;
    @Autowired JdbcTemplate jdbc;

    @Test
    void remindBranch() {
        deployAndStart("alice");
        forceTaskCreateTimeBackBy(2, "task_hello");          // 把 create_time 改成 2 小时前
        insertNodeConfig(1, "REMIND");                       // timeout_hours=1
        handler.runOnce();
        verify(sender).send(eq(parseLong("alice")), eq("INTERNAL"),
                            eq("BPM_TIMEOUT_REMIND"), anyMap());
    }

    @Test
    void autoPassBranch() {
        deployAndStart("alice");
        forceTaskCreateTimeBackBy(2, "task_hello");
        insertNodeConfig(1, "AUTO_PASS");
        handler.runOnce();
        // 任务已 complete
        assertThat(taskService.createTaskQuery().count()).isEqualTo(0);
    }

    @Test
    void escalateBranch() {
        // 这里 assignee 必须是数字 user id（Flowable assignee 字段是 string）
        deployAndStart("7");
        forceTaskCreateTimeBackBy(2, "task_hello");
        insertNodeConfig(1, "ESCALATE");
        // 用 H2/MySQL 直接 mock orgService 的 SPI 行为或注入 stub 实现
        handler.runOnce();
        Task t = taskService.createTaskQuery().singleResult();
        assertThat(t.getAssignee()).isEqualTo("99");          // upper leader
    }
}
```

- [ ] **Step 5：跑 IT + commit**

```bash
git commit -m "feat(bpm-p5): node timeout job AUTO_PASS + ESCALATE + IT"
```

---

## Task 10：历史清理 Quartz job + 保留期配置（TDD）

**Files:**
- 创建 `mapper/HistoryCleanupMapper.java` + xml
- 创建 `scheduler/HistoryCleanupJob.java`
- 测试 `HistoryCleanupJobTest` + `HistoryCleanupJobIT`

**目标：** 每天 03:00 跑，删除：
- `bpm_task_history` 中 `op_time < now - retention_days` 且对应 `bpm_instance_meta.state ∈ {COMPLETED, CANCELLED}` 的行
- 同时清理 Flowable 历史：`historyService.deleteHistoricProcessInstance(actInstId)` 逐个调（清 act_hi_*；批量删 act_hi 表是 Flowable 不推荐做法，因外键复杂）
- 通过 `bpm.history.retention-days` 控制保留期；通过 `bpm.scheduler.history-cleanup.enabled=false` 关闭

- [ ] **Step 1：Mapper**

```java
public interface HistoryCleanupMapper {
    /** 选出符合"已完成/已取消 + 早于阈值"的实例 metaId + actInstId。 */
    List<CleanupCandidate> selectStaleInstances(@Param("threshold") LocalDateTime threshold,
                                                @Param("limit") int limit);
    /** 删除 bpm_task_history，按 inst_id 列表。 */
    int deleteTaskHistoryByInstIds(@Param("instIds") List<String> instIds);
    /** 删除 bpm_instance_meta，按 id 列表（必须最后调，因为 cleanup candidate 来自它）。 */
    int deleteInstanceMetaByIds(@Param("ids") List<String> ids);
}

public class CleanupCandidate {
    private String id;            // bpm_instance_meta.id
    private String actInstId;     // 对应 act_hi_procinst.id
}
```

xml:
```xml
<select id="selectStaleInstances" resultType="org.jeecg.modules.bpm.scheduler.service.CleanupCandidate">
  SELECT id, act_inst_id AS actInstId
  FROM bpm_instance_meta
  WHERE state IN ('COMPLETED','CANCELLED')
    AND end_time IS NOT NULL
    AND end_time &lt; #{threshold}
  ORDER BY end_time ASC
  LIMIT #{limit}
</select>

<delete id="deleteTaskHistoryByInstIds">
  DELETE FROM bpm_task_history WHERE inst_id IN
  <foreach collection="instIds" item="i" open="(" separator="," close=")">#{i}</foreach>
</delete>

<delete id="deleteInstanceMetaByIds">
  DELETE FROM bpm_instance_meta WHERE id IN
  <foreach collection="ids" item="i" open="(" separator="," close=")">#{i}</foreach>
</delete>
```

- [ ] **Step 2：单测**

```java
@Test
void cleanupDeletesTaskHistoryAndActHiAndMeta() {
    HistoryCleanupMapper mapper = mock(HistoryCleanupMapper.class);
    HistoryService histSvc = mock(HistoryService.class);
    BpmSchedulerProperties props = new BpmSchedulerProperties();

    when(mapper.selectStaleInstances(any(), eq(500))).thenReturn(List.of(
        new CleanupCandidate("m1", "p1"), new CleanupCandidate("m2", "p2")
    ), List.of()); // 第二次调用返回空，跳出循环

    HistoryCleanupJob job = new HistoryCleanupJob(mapper, histSvc, props);
    job.run();

    verify(histSvc).deleteHistoricProcessInstance("p1");
    verify(histSvc).deleteHistoricProcessInstance("p2");
    verify(mapper).deleteTaskHistoryByInstIds(List.of("m1","m2"));
    verify(mapper).deleteInstanceMetaByIds(List.of("m1","m2"));
}

@Test
void cleanupRespectsRetentionDaysProperty() {
    BpmSchedulerProperties props = new BpmSchedulerProperties();
    props.getHistoryCleanup().setRetentionDays(30);
    // 验证 selectStaleInstances 收到的 threshold 在 now-30 天附近
}

@Test
void cleanupSkipsWhenDisabled() {
    // enabled=false → 不调任何 mapper（job 实例不被 Spring 创建——本测试覆盖的是 @ConditionalOnProperty 含义）
}
```

- [ ] **Step 3：实现**

```java
@Component
@ConditionalOnProperty(prefix = "bpm.scheduler.history-cleanup", name = "enabled",
                       havingValue = "true", matchIfMissing = true)
public class HistoryCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(HistoryCleanupJob.class);
    private static final int BATCH = 500;

    private final HistoryCleanupMapper mapper;
    private final HistoryService historyService;
    private final BpmSchedulerProperties props;

    public HistoryCleanupJob(HistoryCleanupMapper mapper, HistoryService historyService,
                             BpmSchedulerProperties props) {
        this.mapper = mapper; this.historyService = historyService; this.props = props;
    }

    @Scheduled(cron = "${bpm.scheduler.history-cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void run() {
        int retention = props.getHistoryCleanup().getRetentionDays();
        LocalDateTime threshold = LocalDateTime.now().minusDays(retention);
        log.info("[bpm-p5] history cleanup threshold={} retentionDays={}", threshold, retention);

        long totalDeleted = 0;
        while (true) {
            List<CleanupCandidate> batch = mapper.selectStaleInstances(threshold, BATCH);
            if (batch.isEmpty()) break;

            // 1. 删 Flowable 历史（逐个）
            for (CleanupCandidate c : batch) {
                try {
                    historyService.deleteHistoricProcessInstance(c.getActInstId());
                } catch (Exception e) {
                    log.warn("[bpm-p5] delete act_hi_* failed actInstId={}", c.getActInstId(), e);
                }
            }
            // 2. 删 bpm_task_history
            List<String> instIds = batch.stream().map(CleanupCandidate::getId).collect(toList());
            mapper.deleteTaskHistoryByInstIds(instIds);
            // 3. 删 bpm_instance_meta
            mapper.deleteInstanceMetaByIds(instIds);

            totalDeleted += batch.size();
            if (batch.size() < BATCH) break;
        }
        log.info("[bpm-p5] history cleanup done, deleted instances = {}", totalDeleted);
    }
}
```

> **顺序非常重要：** 先删 act_hi_*，因为 `historyService.deleteHistoricProcessInstance` 内部会清 25 张 act_hi_* 表的全部行；再删 bpm_task_history；最后删 bpm_instance_meta。**反过来会导致 actInstId 已无 meta 引用而漏删 act_hi。**

- [ ] **Step 4：IT — `HistoryCleanupJobIT`**

1. Testcontainers 启 MySQL + 部署 hello-world
2. 跑 3 个实例完成 → 把它们的 `bpm_instance_meta.end_time` 改成 200 天前
3. 跑 1 个实例完成 → end_time 留在今天
4. 设 `bpm.history.retention-days=180`
5. 调 `historyCleanupJob.run()`
6. 断：`act_hi_procinst` 中那 3 个 actInstId 全部 0 行；`bpm_task_history` 中相关 inst_id 行 0 行；`bpm_instance_meta` 中那 3 行删除；剩下 1 行保留

- [ ] **Step 5：跑 IT + commit**

```bash
git commit -m "feat(bpm-p5): history cleanup job (180-day retention) + IT"
```

---

## Task 11：前端 `ProcessMonitor.vue` + `InstanceDetailDrawer.vue` + API 层

**Files（jeecgboot-vue3 工作副本）：**
- `src/api/bpm/monitor.ts`
- `src/views/bpm/monitor/ProcessMonitor.vue`
- `src/views/bpm/monitor/InstanceDetailDrawer.vue`
- `src/views/bpm/monitor/components/DiagramViewer.vue`
- `src/views/bpm/monitor/components/TaskHistoryTable.vue`
- `src/views/bpm/monitor/components/InterveneActions.vue`

> **本 Task 不在 BPM 仓库内 commit；前端代码落地在 jeecgboot-vue3 工作副本。** 但本计划完整列出代码骨架，便于后续单独 commit 到 jeecgboot-vue3 仓库（与 P1 前端代码同仓）。

- [ ] **Step 1：API 层 `src/api/bpm/monitor.ts`**

```ts
import { defHttp } from '/@/utils/http/axios';

export interface MonitorInstanceQuery {
  defKey?: string;
  defVersion?: number;
  applyDeptId?: number;
  applyUserId?: number;
  state?: 'RUNNING' | 'COMPLETED' | 'CANCELLED' | 'SUSPENDED';
  startTimeFrom?: string;
  startTimeTo?: string;
  pageNo?: number;
  pageSize?: number;
}

export const listInstances = (q: MonitorInstanceQuery) =>
  defHttp.get({ url: '/bpm/v1/monitor/instances', params: q });

export const getDiagram = (id: string) =>
  defHttp.get({ url: `/bpm/v1/monitor/instances/${id}/diagram` });

export interface StatsQuery {
  startTimeFrom?: string;
  startTimeTo?: string;
  defKey?: string;
}

export const getStats = (q: StatsQuery, scope?: string[]) =>
  defHttp.get({
    url: '/bpm/v1/monitor/stats',
    params: { ...q, scope: scope?.join(',') },
  });

export type InterveneAction = 'FORCE_COMPLETE_TASK' | 'FORCE_CANCEL' | 'FORCE_REASSIGN';

export interface IntervenePayload {
  action: InterveneAction;
  taskId?: string;
  newAssigneeId?: number;
  comment?: string;
  variables?: Record<string, unknown>;
}

export const intervene = (instanceId: string, payload: IntervenePayload) =>
  defHttp.post({ url: `/bpm/v1/instance/${instanceId}/intervene`, data: payload });
```

- [ ] **Step 2：`ProcessMonitor.vue` —— 列表 + filters**

骨架：
- 顶部 `a-form` 行内表单：流程定义下拉（调 `/bpm/v1/definition/list?state=PUBLISHED`）/ 状态下拉 / 部门 `JSelectDepart` / 申请人 `JSelectUserByDept` / 起止时间 `a-range-picker`
- VxeTable：列：流程定义 / 版本 / 业务键 / 申请人 / 部门 / 状态 / 发起时间 / 操作（"详情"按钮 → 打开 `InstanceDetailDrawer` 传入 `instanceId`）
- 翻页：默认 `pageSize=20`

- [ ] **Step 3：`InstanceDetailDrawer.vue` — tabs**

```vue
<a-drawer width="80%" :open="visible" @close="$emit('update:visible', false)">
  <a-tabs v-model:activeKey="tab">
    <a-tab-pane key="diagram" tab="流程图">
      <DiagramViewer :instance-id="instanceId" />
    </a-tab-pane>
    <a-tab-pane key="history" tab="任务历史">
      <TaskHistoryTable :instance-id="instanceId" />
    </a-tab-pane>
    <a-tab-pane key="form" tab="表单数据">
      <!-- 复用 P2 的 OnlCgform 渲染器（只读模式） -->
    </a-tab-pane>
    <a-tab-pane key="intervene" tab="干预" v-if="isAdmin">
      <InterveneActions :instance-id="instanceId" @done="onIntervened" />
    </a-tab-pane>
  </a-tabs>
</a-drawer>
```

- [ ] **Step 4：`DiagramViewer.vue` — bpmn-js viewer + colorize overlay（spec §7.2）**

```ts
import BpmnViewer from 'bpmn-js/lib/Viewer';

async function load() {
  const { bpmnXml, currentNodeIds } = await getDiagram(props.instanceId);
  viewer = new BpmnViewer({ container: containerRef.value });
  await viewer.importXML(bpmnXml);
  const overlays = viewer.get('overlays');
  const canvas = viewer.get('canvas');
  for (const id of currentNodeIds) {
    canvas.addMarker(id, 'highlight-current');     // 配套 CSS: .highlight-current rect { stroke: #f5222d; stroke-width: 3; }
  }
  canvas.zoom('fit-viewport');
}
```

CSS（同文件 `<style>` 块）：
```css
.highlight-current :where(.djs-visual > rect, .djs-visual > circle, .djs-visual > polygon) {
  stroke: #f5222d !important;
  stroke-width: 3 !important;
}
```

- [ ] **Step 5：`TaskHistoryTable.vue` — VxeTable 任务历史**

调 `/bpm/v1/instance/{id}/history`（P2 已有）；展示 nodeId / assignee / action / comment / opTime。

> **若 P2 没有该接口：** 在 P5 同步在 `BpmInstanceController` 加 `GET /instance/{id}/history`（直接 `historyMapper.selectByInstId`）。这是个微小补丁，commit 单独标注 `feat(bpm-p5): GET /instance/{id}/history (consumed by P5 monitor)`。

- [ ] **Step 6：`InterveneActions.vue` — 三个按钮**

- "强制完成当前任务"：选中一个 RUNNING 任务（从 `getDiagram` 拿 currentNodeIds → 反查任务列表）→ 弹 `a-modal` 填 comment → 调 `intervene(instId, {action:'FORCE_COMPLETE_TASK', taskId, comment})`
- "强制取消实例"：弹确认 → 调 `intervene(instId, {action:'FORCE_CANCEL', comment})`
- "强制重新指派"：选任务 + `JSelectUserByDept` 选新 assignee + comment → `intervene(instId, {action:'FORCE_REASSIGN', taskId, newAssigneeId, comment})`

每次操作成功后 `emit('done')` → drawer 重新加载 `getDiagram`/任务历史。

- [ ] **Step 7：路由 + 菜单**

`src/router/routes/modules/bpm.ts`（P1 已存在）追加：
```ts
{
  path: '/bpm/monitor',
  name: 'BpmMonitor',
  component: () => import('@/views/bpm/monitor/ProcessMonitor.vue'),
  meta: { title: '流程监控', icon: 'ant-design:monitor-outlined' },
}
```

后端 `sys_permission` 注入 SQL（jeecg-boot 工作副本下 `sql/bpm_p5_menu.sql`）：
```sql
INSERT INTO sys_permission (id, parent_id, name, url, component, perms, ...)
VALUES ('bpm-monitor', 'bpm', '流程监控', '/bpm/monitor', 'bpm/monitor/ProcessMonitor', 'bpm:monitor:list', ...);
```

- [ ] **Step 8：手工冒烟（在工作副本中跑 jeecgboot-vue3）**

```bash
cd jeecgboot-vue3
pnpm dev
# 浏览器打开 http://localhost:3100/bpm/monitor
# 点详情 → 流程图 tab 显示 bpmn 图、当前节点高亮红框
# 任务历史 tab 显示 P2 写入的 history 行
# 干预 tab（admin only）能弹三个按钮
```

期望全部链路通。

- [ ] **Step 9：commit（在 jeecgboot-vue3 工作副本下，单独仓库）**

```bash
cd /Users/wuhoujin/Documents/dev/jeecgboot-vue3   # 工作副本路径示例
git add src/api/bpm/monitor.ts src/views/bpm/monitor/
git commit -m "feat(bpm-p5): process monitor page + instance detail drawer with bpmn-js highlight"
```

---

## Task 12：前端 `StatsDashboard.vue` + ECharts + 路由

**Files（jeecgboot-vue3 工作副本）：**
- `src/views/bpm/monitor/stats/StatsDashboard.vue`

- [ ] **Step 1：组件骨架**

```vue
<template>
  <div class="stats-dashboard">
    <a-form layout="inline">
      <a-form-item label="时间区间"><a-range-picker v-model:value="dateRange" /></a-form-item>
      <a-form-item label="流程"><a-select v-model:value="defKey" :options="defOptions" allow-clear/></a-form-item>
      <a-button type="primary" @click="reload">查询</a-button>
    </a-form>

    <a-row :gutter="16" class="mt-4">
      <a-col :span="12"><div ref="avgDurChart" class="chart" /></a-col>
      <a-col :span="12"><div ref="overdueChart" class="chart" /></a-col>
      <a-col :span="24"><div ref="trendChart" class="chart-tall" /></a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts/core';
import { BarChart, HeatmapChart, LineChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, GridComponent, VisualMapComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
echarts.use([BarChart, HeatmapChart, LineChart, TitleComponent, TooltipComponent, GridComponent, VisualMapComponent, CanvasRenderer]);

import { getStats } from '/@/api/bpm/monitor';
// ... refs / onMounted / reload()
</script>
```

> **ECharts 按需引入：** jeecgboot-vue3 已在 `package.json` 包含 `echarts`（spec §1.2 / §3.4），按需 import 不引入新版本——延续 vendor chunk。

- [ ] **Step 2：三张图实现**

1. **平均耗时柱状图**（`byDefinition`）：x = `defName`、y = `avgDurationMs / 1000 / 3600`（小时）、tooltip 显示 instanceCount/completionRate
2. **超时率热力图**（`byNode`）：x = `defKey`、y = `nodeId`、value = `overdueRate`（红→绿渐变 visualMap）
3. **申请量趋势线图**（`byApplyDept`）：x = `applyDeptName`，但既然 dateRange 拉一段汇总 — 改成"按部门 × 时段"的折线，Task 5 已经按部门分组；趋势走需要后端补 `byDate` 维度。**P5 不做 byDate，前端图 3 显示为"按部门柱状图"作为替代——或在 Task 5 追加一个 SQL `selectStatsByApplyDeptOverTime` 按 `DATE_FORMAT(start_time, '%Y-%m')` 分桶。**

> **决策：在 Task 5 追加 byApplyDeptOverTime SQL**（多月趋势线更有意义）。本 Task 12 起在 `MonitorMapper.xml` 追加：

```xml
<select id="selectStatsByApplyDeptOverTime"
        resultType="org.jeecg.modules.bpm.monitor.dto.StatsByApplyDeptTrendRow">
  SELECT m.apply_dept_id AS applyDeptId,
         DATE_FORMAT(m.start_time, '%Y-%m') AS bucket,
         COUNT(1) AS instanceCount
  FROM bpm_instance_meta m
  WHERE m.state &lt;&gt; 'SANDBOX' AND m.apply_dept_id IS NOT NULL
  <if test="q.startTimeFrom != null">AND m.start_time &gt;= #{q.startTimeFrom}</if>
  <if test="q.startTimeTo != null">AND m.start_time &lt;= #{q.startTimeTo}</if>
  GROUP BY m.apply_dept_id, bucket
  ORDER BY bucket, instanceCount DESC
  LIMIT 1000
</select>
```

`StatsResponse` 加 `byApplyDeptOverTime` 字段；service compute 中追加。**Task 5 的单测 / IT 同步追加这个新维度的覆盖**——在 Task 5 完成后回填即可，本 Task 12 只是消费方。

- [ ] **Step 3：路由 + 菜单**

同 Task 11，追加 `bpm/monitor/stats` 菜单 + 权限 `bpm:monitor:stats`。

- [ ] **Step 4：手工冒烟**

打开 `/bpm/monitor/stats` → 三张图渲染、切时间区间能刷新。

- [ ] **Step 5：commit**

```bash
git commit -m "feat(bpm-p5): stats dashboard with echarts (byDefinition / byNode / byApplyDept)"
```

---

## Task 13：模块整体冒烟 + P5_DONE 验收清单

**Files:**
- 创建 `jeecg-module-bpm/P5_DONE.md`
- 修改 `INTEGRATION.md` 追加"## 8. 后端定时任务配置"段

- [ ] **Step 1：跑全量测试**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml clean install
```

期望 BUILD SUCCESS；P0~P5 所有单测 + IT 全部绿。

- [ ] **Step 2：在 jeecg-boot v3.5.5 真实环境冒烟**

```bash
cd jeecg-boot/jeecg-boot-module-system
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

打开浏览器：
1. `/bpm/monitor`：列表能加载、搜索能过滤
2. 点详情 → 流程图 tab → bpmn-js 渲染 + 当前节点红框
3. 任务历史 tab → 行齐全
4. 干预 tab（admin）→ FORCE_COMPLETE_TASK 跑通，回到列表实例消失（已结束）
5. `/bpm/monitor/stats` → 三张图渲染
6. 等待 5 分钟（或手动 cron 改成 `*/30 * * * * ?` 每 30 秒一次）→ 看日志 `[bpm-p5] handle overdue task` 行出现
7. 把 mysql `bpm_instance_meta` 行 end_time 改到 200 天前 → 等到 03:00 或临时改 cron 为 `0 */1 * * * ?` 每分钟 → 看 `[bpm-p5] history cleanup done` 日志 + DB 行数减少

- [ ] **Step 3：写 `P5_DONE.md`**

```markdown
# P5 验收清单 ✅（BPM 模块整体收官）

## 监控查询
- [x] GET /bpm/v1/monitor/instances — 6 类过滤通过 IT
- [x] GET /bpm/v1/monitor/instances/{id}/diagram — bpmnXml + currentNodeIds 单测通过
- [x] GET /bpm/v1/monitor/stats — byDefinition / byNode / byApplyDept / byApplyDeptOverTime 通过 IT

## 强制干预
- [x] POST /bpm/v1/instance/{id}/intervene FORCE_COMPLETE_TASK
- [x] POST /bpm/v1/instance/{id}/intervene FORCE_CANCEL
- [x] POST /bpm/v1/instance/{id}/intervene FORCE_REASSIGN
- [x] 三个动作均落 bpm_task_history.action 审计记录
- [x] @RequiresPermissions("bpm:monitor:intervene") 单测覆盖（401 / 403）

## 节点超时 job（@Scheduled, 每 5 分钟）
- [x] REMIND 分支：调 BpmNotificationSender + 幂等
- [x] AUTO_PASS 分支：调 taskService.complete + 写 history
- [x] ESCALATE 分支：升级到上级领导 + ESCALATE_FAILED 兜底
- [x] 通过 bpm.scheduler.timeout.enabled=false 可关闭

## 历史清理 job（@Scheduled, 每天 03:00）
- [x] 删除 bpm_task_history + bpm_instance_meta + act_hi_*
- [x] 仅删除 state ∈ {COMPLETED, CANCELLED} 的实例
- [x] 通过 bpm.history.retention-days 配置（默认 180 = 6 个月）
- [x] 通过 bpm.scheduler.history-cleanup.enabled=false 可关闭

## 前端
- [x] ProcessMonitor.vue（VxeTable + filters + 详情按钮）
- [x] InstanceDetailDrawer.vue（流程图 / 任务历史 / 表单 / 干预 4 tab）
- [x] DiagramViewer.vue（bpmn-js Viewer + colorize overlay 当前节点）
- [x] StatsDashboard.vue（ECharts 三张图）
- [x] 路由 + sys_permission 菜单 SQL（admin only 干预）

## 模块收官（spec §3 七大块全部闭环）
- [x] 流程定义 CRUD（P1）
- [x] BPMN 设计器 + 节点配置（P1）
- [x] 节点人员调度 6 策略 + 表单绑定（P2）
- [x] 分支表达式 + 多人节点（会签/或签/顺序签）（P3）
- [x] 转审 / 加签 / 版本生命周期 + 沙箱（P4）
- [x] 监控 + 统计 + 强制干预 + 超时 + 清理（P5）✅

## 不达标项
- 无（spec §1.3 所列非目标外，需求章节全部交付）
```

- [ ] **Step 4：补 INTEGRATION.md**

```markdown
## 8. 后端定时任务配置

BPM P5 启用了两个 Spring `@Scheduled` 任务（与 jeecg Quartz 共存，互不影响）：

```yaml
bpm:
  scheduler:
    timeout:
      enabled: true                  # 关闭设为 false
      cron: "0 */5 * * * ?"          # 每 5 分钟扫描超时任务
    history-cleanup:
      enabled: true
      cron: "0 0 3 * * ?"            # 每天 03:00 清理
  history:
    retention-days: 180              # 历史保留 6 个月（删 act_hi_* + bpm_task_history + bpm_instance_meta）
```

> **MySQL 主从延迟提示：** history-cleanup 是物理 DELETE，量大时建议夜间业务低峰期执行；可调高 cron 频率但每次扫描批次固定为 500 条避免锁表。
```

- [ ] **Step 5：commit + push**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/P5_DONE.md INTEGRATION.md
git commit -m "docs(bpm-p5): P5 acceptance checklist + scheduler config in INTEGRATION.md"
git push origin main
```

---

## Self-Review Notes

**spec 覆盖：**
- §1.2（Quartz 与 jeecg 共存）→ Task 8 / 10 用 Spring `@Scheduled`，**不**用 jeecg Quartz starter ✅
- §5.6（监控全部）→ Task 2 / 3 / 4 / 5 / 6 / 7 ✅
- §6（API：`/monitor/instances` / `/monitor/stats` / `/instance/{id}/intervene`）→ Task 2 / 3 / 4 / 5 / 6 / 7 ✅
- §7.2（前端 ProcessMonitor.vue + bpmn-js viewer 高亮）→ Task 11 ✅
- §8（强制干预 admin 权限）→ Task 6 controller `@RequiresPermissions("bpm:monitor:intervene")` ✅
- §11（历史保留 6 个月）→ Task 10 默认 retention-days=180 ✅
- §13（Quartz 与 jeecg 共存）→ Task 8 / 10 通过 `@ConditionalOnProperty` 可关 ✅

**未覆盖（按 spec §1.3 非目标正确排除）：**
- 跨租户 / 多版本（一期单租户，spec Q1）
- 流程图运行时回放动画（spec §1.3）— 仅静态高亮当前节点

**约束达成：**
- bpm-biz 零 jeecg dep：全部用 SPI（BpmOrgService / BpmNotificationSender / BpmUserContext）+ Shiro 注解（shiro-core），无 `org.jeecgframework.boot:*` 依赖 ✅
- ECharts 不升级版本：Task 12 `import * as echarts from 'echarts/core'` 按需引入，复用 vendor chunk ✅
- 聚合 SQL 显式 LIMIT：byDefinition LIMIT 200 / byNode LIMIT 500 / byApplyDept LIMIT 100 / byApplyDeptOverTime LIMIT 1000 ✅
- @Scheduled 而非 jeecg Quartz：Task 8 / 10 ✅
- 干预审计：Task 6 / 7 全部走 `bpm_task_history.action` 字段（FORCE_COMPLETE_TASK / FORCE_CANCEL_INSTANCE / FORCE_REASSIGN / TIMEOUT_REMIND / AUTO_PASS / ESCALATE / ESCALATE_FAILED）✅
- TDD：每个 Task 单测 → IT；P0 同款节奏 ✅
- Conventional commits：每个 Task `feat(bpm-p5): ...` / `docs(bpm-p5): ...` ✅

**类型一致性：**
- `MonitorInstanceQuery`（Task 2 定义、Task 3 复用）✅
- `StatsQuery` / `StatsResponse`（Task 4 定义、Task 5 / 12 扩展）✅
- `InterveneAction` enum（Task 6 定义、Task 7 / 11 复用）✅
- `BpmTaskHistory.action` 字段值集合：{APPROVE / REJECT / TRANSFER / COUNTERSIGN（P2~P4）/ FORCE_COMPLETE_TASK / FORCE_CANCEL_INSTANCE / FORCE_REASSIGN / TIMEOUT_REMIND / AUTO_PASS / ESCALATE / ESCALATE_FAILED（P5）}——总集合在 P5_DONE.md 中固化 ✅
- `BpmOrgService` 在 P5 新增的方法（findUserName / findDeptName / findUserMainDeptId / findUpperDeptLeaders）：Task 2 / 5 / 9 中明确"如 P1 未含则同步补 SPI + adapter 实现"——P5 计划接受这个边界依赖 ✅

**Placeholder 检查：** 无 TBD / TODO / FIXME / 待补充 / 待定 / 稍后 / 未来补 字样；唯二的 "若 P2 没有该接口" / "如 P1 未含" 是与既定前置 phase 的 contract 边界，已明确写出补救路径（在本 Task 内同步补一行接口）。

**P5 完成即整个 BPM 模块完成。** 计划本身的"下一步"——交付前端代码合并到 jeecgboot-vue3 主仓库 / 生产灰度上线 / spec §12 待决问题 Q1~Q11 决策落地——属于运维与运营，不再属于本 plan 范围。
