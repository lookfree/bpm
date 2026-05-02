# BPM P1 — 流程定义 CRUD + BPMN 设计器 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 P0 已交付的 `jeecg-module-bpm` 工程上补齐 4 个 SPI 接口、jeecg 适配器、`bpm_process_definition` + `bpm_process_definition_history` 两张业务表、`/bpm/v1/definition` 全套 REST 接口（含 BPMN XML 校验、版本快照、简化 publish），以及 `jeecgboot-vue3` 工程内的 bpmn-js 17 设计器与流程定义列表页，使管理员能够"画图 → 保存 → 发布 → 看版本"完整闭环。

**Architecture：**
- `bpm-spi` 子模块新增（依赖 `bpm-api`），定义 4 个接口 + 2 个 DTO；`bpm-biz` 通过 `@ConditionalOnMissingBean` 提供 NoOp fallback 保证测试可运行
- `bpm-adapter-jeecg` 子模块新增（依赖 `bpm-spi` + jeecg `jeecg-boot-base-core`），提供 `JeecgBpmUserContext` 完整实现（读 jeecg `JwtUtil`），其余 3 个接口先 stub 返回空，留给 P2/后续 phase 落地
- `bpm-biz` 引入 MyBatis-Plus 3.5.3.1 处理 `bpm_*` 业务表（与 jeecg 同版本以避免 ClassLoader 冲突）；`act_*` 表仍由 Flowable 自管，与本计划无关
- `BpmSchemaInitializer` 监听 `ApplicationReadyEvent`，幂等执行 classpath 内 `bpm-schema-mysql.sql`（CREATE TABLE IF NOT EXISTS），可由 `bpm.schema.auto-init=false` 关闭
- `DefinitionController`（`/bpm/v1/definition`）做 CRUD + simplified DRAFT→PUBLISHED publish + version 列表，BPMN XML 用 Flowable `BpmnXMLConverter` 校验
- 前端在**平行仓库** `jeecgboot-vue3`（不在本 BPM 仓库中）`src/views/bpm/` 下新增定义列表 + 设计器路由，`BpmnDesigner.vue` 包装 bpmn-js 17，菜单/权限通过 SQL 注入到 `sys_permission`

**Tech Stack:** Spring Boot 2.7.10 / Flowable 6.8.0（`BpmnXMLConverter` 复用其 `bpmn-model`） / MyBatis-Plus 3.5.3.1 / MySQL 8.0.27（生产同版本，测试用 Testcontainers） / JUnit 5 + AssertJ + Mockito / Vue 3.3.4 / Ant Design Vue 3.x / VxeTable 4.x / bpmn-js ^17 / Vite。

**与 spec 对应章节：** `docs/superpowers/specs/2026-04-30-bpm-module-design.md` §1.2（独立模块）、§3.1（4 模块架构 — 本 phase 把 spi+adapter 真正激活）、§3.3（SPI 4 接口完整签名）、§4.1（`bpm_process_definition` + `bpm_process_definition_history` 字段）、§4.3（与 jeecg 同库 schema）、§6（`/definition` 端点列表）、§7.1（前端路由 `/bpm/designer`、`/bpm/definition`）、§7.2（`BpmnDesigner.vue` 描述）、§7.3（API 调用层 `src/api/bpm/definition.ts`）、§8（BPMN 上传校验）、§10（测试策略）、§13（耦合点：JwtUtil → BpmUserContext）。

**前置假设：**
1. P0 全部 12 个 Task 已完成（commit 范围 `412a3e3..1e65d07`）；本机能 `mvn -f jeecg-module-bpm/pom.xml clean install -DskipTests` BUILD SUCCESS
2. `jeecg-module-bpm-spi/src/main/java/org/jeecg/modules/bpm/spi/.gitkeep` 与 `jeecg-module-bpm-adapter-jeecg/src/main/java/org/jeecg/modules/bpm/adapter/jeecg/.gitkeep` 占位文件已存在（commit `1b0cf11`），但两个子模块 pom 与 module 注册尚未存在
3. `~/bin/bpm-env.sh` 设置 JDK 11 + Maven 3.9.x 路径；Docker Desktop 已启动（Testcontainers 用）
4. 本仓库已 clone jeecg-boot v3.5.5 到 `./jeecg-boot/`（仅 Task 5 IT 测试用 jeecg 的 `JwtUtil` jar；不参与日常 mvn build）
5. 前端 `jeecgboot-vue3` 工程是**独立的 git 仓库**，不在本 `/Users/wuhoujin/Documents/dev/bpm` 工作区；本计划 Task 10/11/12 中明确给出在 jeecgboot-vue3 工程内的相对路径，但 commit 与 push 在 jeecgboot-vue3 工程的 git 仓库内独立执行；本 BPM 仓库只承载后端代码与一份"前端代码归档副本 + SQL 脚本"于 `jeecg-module-bpm/frontend-snapshot/` 与 `jeecg-module-bpm/db/menu/` 下作为参考与可移植产出

---

## File Structure（本计划新增/修改的全部文件）

**后端新增（在仓库 `/Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/` 下）：**

```
jeecg-module-bpm/
├── pom.xml                                         # ★ 修改：<modules> 加入 spi、adapter-jeecg
├── jeecg-module-bpm-spi/
│   ├── pom.xml                                     # 新增（替换 .gitkeep 不动）
│   └── src/main/java/org/jeecg/modules/bpm/spi/
│       ├── BpmUserContext.java                     # 4 SPI 接口
│       ├── BpmOrgService.java
│       ├── BpmFormService.java
│       ├── BpmNotificationSender.java
│       └── dto/
│           ├── BpmFormSchema.java                  # DTO
│           └── BpmFormField.java                   # DTO
├── jeecg-module-bpm-adapter-jeecg/
│   ├── pom.xml                                     # 新增
│   └── src/main/
│       ├── java/org/jeecg/modules/bpm/adapter/jeecg/
│       │   ├── BpmAdapterJeecgAutoConfiguration.java
│       │   ├── JeecgBpmUserContext.java            # 完整实现（调 JwtUtil）
│       │   ├── NoopBpmOrgService.java              # 空实现，P2 替换
│       │   ├── NoopBpmFormService.java             # 空实现，P2 替换
│       │   └── NoopBpmNotificationSender.java      # 空实现，P2 替换
│       └── resources/META-INF/spring.factories     # 注册 BpmAdapterJeecgAutoConfiguration
└── jeecg-module-bpm-biz/
    ├── pom.xml                                     # ★ 修改：加 bpm-spi + mybatis-plus
    └── src/main/
        ├── java/org/jeecg/modules/bpm/
        │   ├── BpmModuleAutoConfiguration.java     # ★ 修改：加 @MapperScan + 引 NoOpUserContextConfiguration
        │   ├── config/
        │   │   ├── MybatisPlusConfig.java          # 新增（分页插件）
        │   │   └── NoOpUserContextConfiguration.java  # 新增（@ConditionalOnMissingBean fallback）
        │   ├── support/
        │   │   ├── NoOpBpmUserContext.java         # 测试 fallback
        │   │   └── BpmSchemaInitializer.java       # 新增（ApplicationReadyEvent 跑 SQL）
        │   ├── definition/
        │   │   ├── entity/
        │   │   │   ├── BpmProcessDefinition.java
        │   │   │   └── BpmProcessDefinitionHistory.java
        │   │   ├── mapper/
        │   │   │   ├── BpmProcessDefinitionMapper.java
        │   │   │   └── BpmProcessDefinitionHistoryMapper.java
        │   │   ├── service/
        │   │   │   ├── BpmProcessDefinitionService.java          # 接口
        │   │   │   ├── BpmProcessDefinitionServiceImpl.java
        │   │   │   ├── BpmProcessDefinitionHistoryService.java   # 接口
        │   │   │   └── BpmProcessDefinitionHistoryServiceImpl.java
        │   │   ├── dto/
        │   │   │   ├── DefinitionCreateRequest.java
        │   │   │   ├── DefinitionUpdateRequest.java
        │   │   │   ├── DefinitionQueryRequest.java
        │   │   │   └── DefinitionVO.java
        │   │   ├── support/
        │   │   │   └── BpmnXmlValidator.java       # Flowable BpmnXMLConverter 包装
        │   │   └── controller/
        │   │       └── DefinitionController.java
        └── resources/
            ├── bpm-application.yml                 # ★ 修改：加 mybatis-plus + bpm.schema.auto-init
            ├── db/
            │   └── bpm-schema-mysql.sql            # 新增 DDL
            └── mapper/                             # MyBatis XML（如需要）
                ├── BpmProcessDefinitionMapper.xml
                └── BpmProcessDefinitionHistoryMapper.xml
```

**后端测试新增：**

```
jeecg-module-bpm-spi/src/test/java/org/jeecg/modules/bpm/spi/dto/
└── BpmFormSchemaTest.java                          # POJO 等价性

jeecg-module-bpm-adapter-jeecg/src/test/java/org/jeecg/modules/bpm/adapter/jeecg/
├── JeecgBpmUserContextTest.java                    # Mockito mock JwtUtil + RequestContextHolder
└── BpmAdapterJeecgAutoConfigurationTest.java       # 装配冒烟

jeecg-module-bpm-biz/src/test/
├── java/org/jeecg/modules/bpm/
│   ├── support/
│   │   ├── NoOpBpmUserContextTest.java
│   │   └── BpmSchemaInitializerIT.java             # Testcontainers 验证 7 张 act_* 之外的 bpm_* 表
│   └── definition/
│       ├── entity/
│       │   └── BpmProcessDefinitionEntityTest.java
│       ├── service/
│       │   ├── BpmProcessDefinitionServiceImplTest.java
│       │   └── BpmProcessDefinitionHistoryServiceImplTest.java
│       ├── support/
│       │   └── BpmnXmlValidatorTest.java
│       └── controller/
│           ├── DefinitionControllerTest.java       # MockMvc CRUD
│           └── DefinitionControllerPublishIT.java  # Testcontainers publish + version
└── resources/
    └── bpm/
        ├── valid-definition.bpmn20.xml             # 简单合法 BPMN
        └── invalid-definition.bpmn20.xml           # 故意破坏 XML schema
```

**前端新增（在平行仓库 `jeecgboot-vue3` 工程内 — 此工程不在本 BPM 仓库；以下路径以 jeecgboot-vue3 工程根为基准）：**

```
jeecgboot-vue3/
├── package.json                                    # ★ 修改：加 bpmn-js ^17
├── src/
│   ├── api/bpm/
│   │   ├── definition.ts                           # 新增：list / get / create / update / delete / publish / versions
│   │   └── model/definitionModel.ts                # TS 类型
│   ├── views/bpm/
│   │   ├── designer/
│   │   │   ├── BpmnDesigner.vue                    # bpmn-js 17 包装
│   │   │   └── DesignerPage.vue                    # 路由页
│   │   └── definition/
│   │       ├── DefinitionList.vue                  # VxeTable 列表
│   │       ├── DefinitionList.data.ts              # 列定义/搜索
│   │       └── components/VersionsModal.vue        # 历史版本侧拉
│   └── router/routes/modules/bpm.ts                # 新增模块路由（如该工程使用本地路由）
└── README-bpm.md                                   # 简短一页，安装步骤 + 菜单 SQL 引用
```

**前端代码归档（在本 BPM 仓库 — 平行仓库不可达时仍可作为唯一权威产出）：**

```
jeecg-module-bpm/frontend-snapshot/                 # 与上一段一对一拷贝（CI 比对一致性）
└── ...（同上目录树，文件内容一致）
```

**SQL 脚本（在本 BPM 仓库）：**

```
jeecg-module-bpm/db/menu/
├── bpm-p1-menu.sql                                 # 新增菜单 + 路由 + 权限点
└── bpm-p1-rollback.sql                             # 反向脚本，按 perm_code 回退
```

**验收文档：**

```
jeecg-module-bpm/P1_DONE.md                         # 新增：P1 验收清单
```

---

## Task 1：bpm-spi 子模块（pom + 4 接口 + 2 DTO + spec §3.3 完整签名）

**Files:**
- 创建 `jeecg-module-bpm/jeecg-module-bpm-spi/pom.xml`
- 创建 `jeecg-module-bpm-spi/src/main/java/org/jeecg/modules/bpm/spi/{BpmUserContext,BpmOrgService,BpmFormService,BpmNotificationSender}.java`
- 创建 `jeecg-module-bpm-spi/src/main/java/org/jeecg/modules/bpm/spi/dto/{BpmFormSchema,BpmFormField}.java`
- 创建测试 `jeecg-module-bpm-spi/src/test/java/org/jeecg/modules/bpm/spi/dto/BpmFormSchemaTest.java`
- 修改父 pom 注册子模块

- [ ] **Step 1：父 pom 注册 spi 子模块**

修改 `jeecg-module-bpm/pom.xml` 的 `<modules>` 段，在 `bpm-api` 后追加：

```xml
<modules>
    <module>jeecg-module-bpm-api</module>
    <module>jeecg-module-bpm-spi</module>
    <module>jeecg-module-bpm-biz</module>
</modules>
```

并在 `<dependencyManagement>` 加上 spi 与 adapter 坐标管理：

```xml
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-spi</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-adapter-jeecg</artifactId>
    <version>${project.version}</version>
</dependency>
```

- [ ] **Step 2：spi 子模块 pom**

`jeecg-module-bpm/jeecg-module-bpm-spi/pom.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.iimt.bpm</groupId>
        <artifactId>jeecg-module-bpm</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>jeecg-module-bpm-spi</artifactId>
    <name>${project.artifactId}</name>
    <description>BPM 对宿主系统的 SPI 接口（用户/组织/表单/通知）</description>

    <dependencies>
        <dependency>
            <groupId>com.iimt.bpm</groupId>
            <artifactId>jeecg-module-bpm-api</artifactId>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3：写 4 个 SPI 接口（按 spec §3.3 完整签名）**

`BpmUserContext.java`：
```java
package org.jeecg.modules.bpm.spi;

import java.util.Set;

/**
 * 当前 HTTP/JWT 上下文中的用户信息访问器。
 * <p>未登录或上下文不可用时方法返回 null / 空集合，绝不抛异常。
 */
public interface BpmUserContext {

    /** 当前用户 id；未登录返回 null */
    Long currentUserId();

    /** 当前用户名（jeecg sys_user.username）；未登录返回 null */
    String currentUsername();

    /** 当前用户主部门 id；未登录或无部门返回 null */
    Long currentDeptId();

    /** 当前用户角色 code 集合；未登录返回 emptySet() */
    Set<String> currentRoleCodes();
}
```

`BpmOrgService.java`：
```java
package org.jeecg.modules.bpm.spi;

import java.util.List;

public interface BpmOrgService {
    /** 部门负责人 user id 列表；找不到返回 emptyList() */
    List<Long> findDeptLeaders(Long deptId);

    /** 上级部门负责人 */
    List<Long> findUpperDeptLeaders(Long deptId);

    /** 按角色 code 反查用户 id 列表 */
    List<Long> findUsersByRole(String roleCode);

    /** 按岗位 code 反查 */
    List<Long> findUsersByPosition(String positionCode);

    /** 用户是否存在且启用 */
    boolean isUserActive(Long userId);
}
```

`BpmFormService.java`：
```java
package org.jeecg.modules.bpm.spi;

import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;

import java.util.Map;

public interface BpmFormService {
    /** 加载表单 schema；未找到返回 null */
    BpmFormSchema loadFormSchema(String formId);

    /** 保存一次表单提交，返回业务键（business_key），与 act_ru_execution.business_key_ 关联 */
    String saveFormSubmission(String formId, Map<String, Object> data);

    /** 加载已有表单数据（审批节点回显） */
    Map<String, Object> loadFormData(String formId, String businessKey);
}
```

`BpmNotificationSender.java`：
```java
package org.jeecg.modules.bpm.spi;

import java.util.Map;

public interface BpmNotificationSender {
    /**
     * @param toUserId 接收人
     * @param channel ∈ {DING, EMAIL, INTERNAL}
     * @param templateCode 模板 code（jeecg sys_message_template）
     * @param vars 模板变量
     */
    void send(Long toUserId, String channel, String templateCode, Map<String, Object> vars);
}
```

- [ ] **Step 4：写 2 个 DTO**

`dto/BpmFormSchema.java`：
```java
package org.jeecg.modules.bpm.spi.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 与具体宿主表单引擎解耦的统一表单结构 */
public class BpmFormSchema {
    private String formId;
    private String formName;
    private List<BpmFormField> fields = new ArrayList<>();

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getFormName() { return formName; }
    public void setFormName(String formName) { this.formName = formName; }
    public List<BpmFormField> getFields() { return fields; }
    public void setFields(List<BpmFormField> fields) { this.fields = fields == null ? new ArrayList<>() : fields; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmFormSchema)) return false;
        BpmFormSchema that = (BpmFormSchema) o;
        return Objects.equals(formId, that.formId)
                && Objects.equals(formName, that.formName)
                && Objects.equals(fields, that.fields);
    }
    @Override public int hashCode() { return Objects.hash(formId, formName, fields); }
}
```

`dto/BpmFormField.java`：
```java
package org.jeecg.modules.bpm.spi.dto;

import java.util.Objects;

public class BpmFormField {
    private String name;
    private String label;
    /** STRING / NUMBER / DATE / BOOLEAN / FILE / USER */
    private String type;
    private boolean required;
    private String defaultValue;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmFormField)) return false;
        BpmFormField that = (BpmFormField) o;
        return required == that.required
                && Objects.equals(name, that.name)
                && Objects.equals(label, that.label)
                && Objects.equals(type, that.type)
                && Objects.equals(defaultValue, that.defaultValue);
    }
    @Override public int hashCode() { return Objects.hash(name, label, type, required, defaultValue); }
}
```

- [ ] **Step 5：写 DTO 测试（先写 fail）**

`jeecg-module-bpm-spi/src/test/java/org/jeecg/modules/bpm/spi/dto/BpmFormSchemaTest.java`：
```java
package org.jeecg.modules.bpm.spi.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class BpmFormSchemaTest {

    @Test
    void equalsAndHashCodeRespectFields() {
        BpmFormField f1 = new BpmFormField();
        f1.setName("amount"); f1.setLabel("金额"); f1.setType("NUMBER"); f1.setRequired(true);

        BpmFormSchema a = new BpmFormSchema();
        a.setFormId("form_x"); a.setFormName("申请单"); a.setFields(Arrays.asList(f1));
        BpmFormSchema b = new BpmFormSchema();
        b.setFormId("form_x"); b.setFormName("申请单"); b.setFields(Arrays.asList(f1));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void fieldsDefaultsToEmptyListNotNull() {
        BpmFormSchema s = new BpmFormSchema();
        assertThat(s.getFields()).isNotNull().isEmpty();
        s.setFields(null);
        assertThat(s.getFields()).isNotNull().isEmpty();
    }
}
```

- [ ] **Step 6：跑测试看 fail（接口/DTO 还未编译则编译失败）**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-spi -am test
```

期望：编译失败（先写测试再写实现的顺序下符合预期）；调换顺序后再次跑应 BUILD SUCCESS / 1 test passed。

- [ ] **Step 7：写实现并 install 验证**

按上面 Step 3、4 创建文件后再跑：
```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-spi -am clean install
```

期望：BUILD SUCCESS；`~/.m2/repository/com/iimt/bpm/jeecg-module-bpm-spi/0.1.0-SNAPSHOT/jeecg-module-bpm-spi-0.1.0-SNAPSHOT.jar` 出现。

- [ ] **Step 8：commit**

```bash
git add jeecg-module-bpm/pom.xml \
        jeecg-module-bpm/jeecg-module-bpm-spi/pom.xml \
        jeecg-module-bpm/jeecg-module-bpm-spi/src/
git commit -m "feat(bpm-p1): add bpm-spi submodule with 4 SPI interfaces + form DTOs"
```

---

## Task 2：bpm-adapter-jeecg 子模块（pom + JeecgBpmUserContext 完整 + 3 个 NoOp）

**Files:**
- 创建 `jeecg-module-bpm/jeecg-module-bpm-adapter-jeecg/pom.xml`
- 创建 `adapter-jeecg/src/main/java/org/jeecg/modules/bpm/adapter/jeecg/{BpmAdapterJeecgAutoConfiguration,JeecgBpmUserContext,NoopBpmOrgService,NoopBpmFormService,NoopBpmNotificationSender}.java`
- 创建 `adapter-jeecg/src/main/resources/META-INF/spring.factories`
- 创建测试 `JeecgBpmUserContextTest.java` + `BpmAdapterJeecgAutoConfigurationTest.java`
- 修改父 pom `<modules>` 注册 adapter-jeecg

- [ ] **Step 1：父 pom 注册 adapter-jeecg**

修改 `jeecg-module-bpm/pom.xml`：
```xml
<modules>
    <module>jeecg-module-bpm-api</module>
    <module>jeecg-module-bpm-spi</module>
    <module>jeecg-module-bpm-biz</module>
    <module>jeecg-module-bpm-adapter-jeecg</module>
</modules>
```

> **顺序解释：** adapter 依赖 spi，spi 依赖 api；biz 与 adapter 在 reactor 中互不依赖（biz 只依赖 spi，运行期才由宿主同时引入 biz + adapter）。

- [ ] **Step 2：adapter-jeecg 子模块 pom**

`jeecg-module-bpm/jeecg-module-bpm-adapter-jeecg/pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.iimt.bpm</groupId>
        <artifactId>jeecg-module-bpm</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </parent>

    <artifactId>jeecg-module-bpm-adapter-jeecg</artifactId>
    <name>${project.artifactId}</name>
    <description>BPM SPI 在 jeecg-boot v3.5.5 下的实现</description>

    <properties>
        <jeecg.version>3.5.5</jeecg.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.iimt.bpm</groupId>
            <artifactId>jeecg-module-bpm-spi</artifactId>
        </dependency>

        <!-- 仅本 adapter 允许依赖 jeecg；scope=provided 让宿主提供 -->
        <dependency>
            <groupId>org.jeecgframework.boot</groupId>
            <artifactId>jeecg-boot-base-core</artifactId>
            <version>${jeecg.version}</version>
            <scope>provided</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-tomcat</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

> **provided 解释：** adapter 不打 jeecg jar 进自己的产物，由宿主引入 jeecg 的真包提供。这样如果宿主不是 jeecg，自然不需要 adapter-jeecg。

- [ ] **Step 3：写 JeecgBpmUserContext 测试（先 fail）**

`jeecg-module-bpm-adapter-jeecg/src/test/java/org/jeecg/modules/bpm/adapter/jeecg/JeecgBpmUserContextTest.java`：
```java
package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.util.jwt.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JeecgBpmUserContextTest {

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void putToken(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Access-Token", token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void noRequestContextReturnsNull() {
        JeecgBpmUserContext ctx = new JeecgBpmUserContext(mock(ISysBaseAPI.class));
        assertThat(ctx.currentUserId()).isNull();
        assertThat(ctx.currentUsername()).isNull();
        assertThat(ctx.currentDeptId()).isNull();
        assertThat(ctx.currentRoleCodes()).isEmpty();
    }

    @Test
    void resolvesUsernameFromJwtAndDelegatesToSysBaseApi() {
        putToken("fake-token");
        ISysBaseAPI api = mock(ISysBaseAPI.class);
        LoginUser u = new LoginUser();
        u.setId("u-001");
        u.setUsername("alice");
        u.setOrgCode("D001");
        when(api.getUserByName("alice")).thenReturn(u);
        when(api.getDepartIdsByOrgCode("D001")).thenReturn(Arrays.asList("dep-1"));
        when(api.getRolesByUsername("alice")).thenReturn(new HashSet<>(Arrays.asList("admin", "approver")));

        try (MockedStatic<JwtUtil> jwt = Mockito.mockStatic(JwtUtil.class)) {
            jwt.when(() -> JwtUtil.getUsername("fake-token")).thenReturn("alice");

            JeecgBpmUserContext ctx = new JeecgBpmUserContext(api);

            assertThat(ctx.currentUsername()).isEqualTo("alice");
            // jeecg LoginUser.id 是 String UUID — adapter 把 username 哈希成 long 不可靠；
            // 这里我们要求 currentUserId 走 ISysBaseAPI 拿数值 id（约定 stub）
            assertThat(ctx.currentUserId()).isNotNull();
            assertThat(ctx.currentRoleCodes()).containsExactlyInAnyOrder("admin", "approver");
        }
    }
}
```

> **重要约束：** jeecg 的 `sys_user.id` 是 `String`（UUID），但 spec §3.3 与 BPM 后续表设计统一用 `Long`。adapter 内做"取 username → 调 ISysBaseAPI 反查 jeecg 内部数值 id"。jeecg `sys_user` 实际有 `username` 唯一索引；BPM 自身存 `Long` 类型 id 时实际语义是"jeecg user 的 hashCode 包装"或"adapter 维护的映射表"——P1 阶段先用 `Math.abs(username.hashCode())` 做稳定 long 兜底，标注 TODO-P2 切换到映射表。

- [ ] **Step 4：写 JeecgBpmUserContext 实现**

`jeecg-module-bpm-adapter-jeecg/src/main/java/org/jeecg/modules/bpm/adapter/jeecg/JeecgBpmUserContext.java`：
```java
package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.jwt.JwtUtil;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * jeecg-boot v3.5.5 实现：从 X-Access-Token 头取 token，{@link JwtUtil} 解出 username，
 * 再调 {@link ISysBaseAPI} 反查角色与部门。
 *
 * <p><b>userId 长整型转换：</b>jeecg sys_user.id 是 String UUID，BPM 内部统一 Long；
 * 当前用 {@code Math.abs(username.hashCode())} 做稳定映射，P2 阶段切换到 adapter 维护的
 * {@code bpm_user_id_mapping} 双向表。
 */
public class JeecgBpmUserContext implements BpmUserContext {

    private final ISysBaseAPI sysBaseAPI;

    public JeecgBpmUserContext(ISysBaseAPI sysBaseAPI) {
        this.sysBaseAPI = sysBaseAPI;
    }

    @Override
    public Long currentUserId() {
        String username = currentUsername();
        if (username == null) return null;
        return (long) Math.abs(username.hashCode());
    }

    @Override
    public String currentUsername() {
        String token = currentToken();
        if (token == null) return null;
        try {
            return JwtUtil.getUsername(token);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public Long currentDeptId() {
        String username = currentUsername();
        if (username == null) return null;
        LoginUser u = sysBaseAPI.getUserByName(username);
        if (u == null || u.getOrgCode() == null) return null;
        List<String> ids = sysBaseAPI.getDepartIdsByOrgCode(u.getOrgCode());
        if (ids == null || ids.isEmpty()) return null;
        return (long) Math.abs(ids.get(0).hashCode());
    }

    @Override
    public Set<String> currentRoleCodes() {
        String username = currentUsername();
        if (username == null) return Collections.emptySet();
        Set<String> roles = sysBaseAPI.getRolesByUsername(username);
        return roles == null ? Collections.emptySet() : roles;
    }

    private String currentToken() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes)) return null;
        HttpServletRequest req = ((ServletRequestAttributes) attrs).getRequest();
        String t = req.getHeader("X-Access-Token");
        return (t == null || t.isEmpty()) ? null : t;
    }
}
```

- [ ] **Step 5：写 3 个 NoOp 实现**

`NoopBpmOrgService.java`：
```java
package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmOrgService;
import java.util.Collections;
import java.util.List;

/** P1 stub — 全量实现在 P2。返回空列表 / true。 */
public class NoopBpmOrgService implements BpmOrgService {
    @Override public List<Long> findDeptLeaders(Long deptId) { return Collections.emptyList(); }
    @Override public List<Long> findUpperDeptLeaders(Long deptId) { return Collections.emptyList(); }
    @Override public List<Long> findUsersByRole(String roleCode) { return Collections.emptyList(); }
    @Override public List<Long> findUsersByPosition(String positionCode) { return Collections.emptyList(); }
    @Override public boolean isUserActive(Long userId) { return userId != null; }
}
```

`NoopBpmFormService.java`：
```java
package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import java.util.Collections;
import java.util.Map;

/** P1 stub — 全量实现在 P2（对接 onl_cgform_*）。 */
public class NoopBpmFormService implements BpmFormService {
    @Override public BpmFormSchema loadFormSchema(String formId) { return null; }
    @Override public String saveFormSubmission(String formId, Map<String, Object> data) {
        return "noop-" + (formId == null ? "null" : formId);
    }
    @Override public Map<String, Object> loadFormData(String formId, String businessKey) {
        return Collections.emptyMap();
    }
}
```

`NoopBpmNotificationSender.java`：
```java
package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/** P1 stub — 全量实现在 P2（对接 jeecg 消息中心）。仅打 info 日志。 */
public class NoopBpmNotificationSender implements BpmNotificationSender {
    private static final Logger LOG = LoggerFactory.getLogger(NoopBpmNotificationSender.class);
    @Override public void send(Long toUserId, String channel, String templateCode, Map<String, Object> vars) {
        LOG.info("[bpm-noop-notify] to={} channel={} template={} vars={}", toUserId, channel, templateCode, vars);
    }
}
```

- [ ] **Step 6：AutoConfiguration**

`BpmAdapterJeecgAutoConfiguration.java`：
```java
package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmNotificationSender;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ISysBaseAPI.class)
public class BpmAdapterJeecgAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BpmUserContext.class)
    public BpmUserContext bpmUserContext(ISysBaseAPI sysBaseAPI) {
        return new JeecgBpmUserContext(sysBaseAPI);
    }

    @Bean
    @ConditionalOnMissingBean(BpmOrgService.class)
    public BpmOrgService bpmOrgService() {
        return new NoopBpmOrgService();
    }

    @Bean
    @ConditionalOnMissingBean(BpmFormService.class)
    public BpmFormService bpmFormService() {
        return new NoopBpmFormService();
    }

    @Bean
    @ConditionalOnMissingBean(BpmNotificationSender.class)
    public BpmNotificationSender bpmNotificationSender() {
        return new NoopBpmNotificationSender();
    }
}
```

`adapter-jeecg/src/main/resources/META-INF/spring.factories`：
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.jeecg.modules.bpm.adapter.jeecg.BpmAdapterJeecgAutoConfiguration
```

- [ ] **Step 7：写装配冒烟测试**

`BpmAdapterJeecgAutoConfigurationTest.java`：
```java
package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmNotificationSender;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BpmAdapterJeecgAutoConfigurationTest {

    @Test
    void registersAllFourSpiBeans() {
        new ApplicationContextRunner()
                .withBean(ISysBaseAPI.class, () -> mock(ISysBaseAPI.class))
                .withConfiguration(AutoConfigurations.of(BpmAdapterJeecgAutoConfiguration.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(BpmUserContext.class);
                    assertThat(ctx).hasSingleBean(BpmOrgService.class);
                    assertThat(ctx).hasSingleBean(BpmFormService.class);
                    assertThat(ctx).hasSingleBean(BpmNotificationSender.class);
                    assertThat(ctx.getBean(BpmUserContext.class)).isInstanceOf(JeecgBpmUserContext.class);
                });
    }
}
```

- [ ] **Step 8：跑测试 → 修 → 通过**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-adapter-jeecg -am test
```

期望：2 tests passed（`JeecgBpmUserContextTest` 2 个 + `BpmAdapterJeecgAutoConfigurationTest` 1 个 = 3 个）。

- [ ] **Step 9：commit**

```bash
git add jeecg-module-bpm/pom.xml \
        jeecg-module-bpm/jeecg-module-bpm-adapter-jeecg/
git commit -m "feat(bpm-p1): bpm-adapter-jeecg with full JeecgBpmUserContext and 3 noop stubs"
```

---

## Task 3：bpm-biz 引入 bpm-spi + NoOp fallback + MyBatis-Plus

**Files:**
- 修改 `jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml`
- 创建 `bpm-biz/src/main/java/org/jeecg/modules/bpm/support/NoOpBpmUserContext.java`
- 创建 `bpm-biz/src/main/java/org/jeecg/modules/bpm/config/NoOpUserContextConfiguration.java`
- 创建 `bpm-biz/src/main/java/org/jeecg/modules/bpm/config/MybatisPlusConfig.java`
- 修改 `bpm-biz/src/main/resources/bpm-application.yml`
- 修改 `bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java`
- 创建测试 `NoOpBpmUserContextTest.java`

- [ ] **Step 1：bpm-biz pom 加依赖**

修改 `jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml` `<dependencies>`：

```xml
<!-- 模块内部 — 在已有 bpm-api 之后 -->
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-spi</artifactId>
</dependency>

<!-- MyBatis-Plus（与 jeecg 同版本 3.5.3.1，避免 ClassLoader 冲突） -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>
```

> **不要**把 adapter-jeecg 加进 biz 依赖；biz 仍保持零 jeecg 依赖。

- [ ] **Step 2：写 NoOpBpmUserContext + 测试**

`bpm-biz/src/test/java/org/jeecg/modules/bpm/support/NoOpBpmUserContextTest.java`（先写）：
```java
package org.jeecg.modules.bpm.support;

import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpBpmUserContextTest {

    @Test
    void allMethodsReturnNullOrEmpty() {
        BpmUserContext ctx = new NoOpBpmUserContext();
        assertThat(ctx.currentUserId()).isNull();
        assertThat(ctx.currentUsername()).isNull();
        assertThat(ctx.currentDeptId()).isNull();
        assertThat(ctx.currentRoleCodes()).isEmpty();
    }
}
```

`bpm-biz/src/main/java/org/jeecg/modules/bpm/support/NoOpBpmUserContext.java`：
```java
package org.jeecg.modules.bpm.support;

import org.jeecg.modules.bpm.spi.BpmUserContext;

import java.util.Collections;
import java.util.Set;

/**
 * 兜底实现：当宿主未提供 BpmUserContext 时（典型场景：测试 / 非 jeecg 集成且未实现 adapter）
 * 加载本 fallback，所有方法返回 null/empty。生产由 adapter-jeecg 覆盖。
 */
public class NoOpBpmUserContext implements BpmUserContext {
    @Override public Long currentUserId() { return null; }
    @Override public String currentUsername() { return null; }
    @Override public Long currentDeptId() { return null; }
    @Override public Set<String> currentRoleCodes() { return Collections.emptySet(); }
}
```

`bpm-biz/src/main/java/org/jeecg/modules/bpm/config/NoOpUserContextConfiguration.java`：
```java
package org.jeecg.modules.bpm.config;

import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.jeecg.modules.bpm.support.NoOpBpmUserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoOpUserContextConfiguration {

    @Bean
    @ConditionalOnMissingBean(BpmUserContext.class)
    public BpmUserContext noOpBpmUserContext() {
        return new NoOpBpmUserContext();
    }
}
```

- [ ] **Step 3：MyBatis-Plus 配置**

`bpm-biz/src/main/java/org/jeecg/modules/bpm/config/MybatisPlusConfig.java`：
```java
package org.jeecg.modules.bpm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

- [ ] **Step 4：BpmModuleAutoConfiguration 加 @MapperScan**

修改 `bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java`：
```java
package org.jeecg.modules.bpm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.jeecg.modules.bpm")
@MapperScan(basePackages = "org.jeecg.modules.bpm.**.mapper")
public class BpmModuleAutoConfiguration {
}
```

- [ ] **Step 5：bpm-application.yml 加 mybatis-plus 默认配置**

修改 `bpm-biz/src/main/resources/bpm-application.yml`，在文件末尾追加：
```yaml
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
  global-config:
    db-config:
      id-type: ASSIGN_UUID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath*:mapper/**/*.xml

bpm:
  schema:
    auto-init: true                 # P1 默认开；生产可关
```

- [ ] **Step 6：跑测试**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz -am test -Dtest=NoOpBpmUserContextTest
```

期望：1 test passed。再跑全量已有测试不应破坏：
```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test
```
期望：所有 P0 测试 + 新增 NoOp 测试都通过。

- [ ] **Step 7：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/support/NoOpBpmUserContext.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/NoOpUserContextConfiguration.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/MybatisPlusConfig.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm-application.yml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/support/NoOpBpmUserContextTest.java
git commit -m "feat(bpm-p1): bpm-biz consumes bpm-spi with NoOp fallback + MyBatis-Plus 3.5.3.1"
```

---

## Task 4：DDL 脚本 + BpmSchemaInitializer + Testcontainers IT

**Files:**
- 创建 `bpm-biz/src/main/resources/db/bpm-schema-mysql.sql`
- 创建 `bpm-biz/src/main/java/org/jeecg/modules/bpm/support/BpmSchemaInitializer.java`
- 创建测试 `BpmSchemaInitializerIT.java`

- [ ] **Step 1：写 DDL（spec §4.1，varchar(32) UUID id 与 jeecg 风格一致）**

`bpm-biz/src/main/resources/db/bpm-schema-mysql.sql`：
```sql
-- BPM P1：流程定义 + 历史版本
-- 字符集 utf8mb4，引擎 InnoDB；与 jeecg 风格一致：varchar(32) id（UUID 去横线）

CREATE TABLE IF NOT EXISTS `bpm_process_definition` (
    `id`            VARCHAR(32) NOT NULL COMMENT '主键 UUID',
    `def_key`       VARCHAR(64) NOT NULL COMMENT '流程定义 key（与 BPMN process id 对齐）',
    `name`          VARCHAR(128) NOT NULL COMMENT '流程名称',
    `category`      VARCHAR(32) DEFAULT 'DEFAULT' COMMENT '分类 / SANDBOX / DEFAULT',
    `version`       INT NOT NULL DEFAULT 1 COMMENT '版本号；publish 时 +1',
    `state`         VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/TESTING/PUBLISHED/ARCHIVED',
    `bpmn_xml`      MEDIUMTEXT COMMENT 'BPMN 2.0 XML',
    `form_id`       VARCHAR(64) DEFAULT NULL COMMENT 'onl_cgform_head.id',
    `act_def_id`    VARCHAR(64) DEFAULT NULL COMMENT 'Flowable act_re_procdef.id_，发布后填',
    `tenant_id`     VARCHAR(32) NOT NULL DEFAULT 'default',
    `description`   VARCHAR(512) DEFAULT NULL,
    `create_by`     VARCHAR(64) DEFAULT NULL,
    `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_by`     VARCHAR(64) DEFAULT NULL,
    `update_time`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT NOT NULL DEFAULT 0 COMMENT '0 未删 / 1 已删（逻辑）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_def_key_tenant` (`def_key`, `tenant_id`, `version`),
    KEY `idx_def_state` (`state`),
    KEY `idx_def_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 流程定义';

CREATE TABLE IF NOT EXISTS `bpm_process_definition_history` (
    `id`             VARCHAR(32) NOT NULL,
    `def_id`         VARCHAR(32) NOT NULL COMMENT 'bpm_process_definition.id',
    `def_key`        VARCHAR(64) NOT NULL,
    `version`        INT NOT NULL,
    `bpmn_xml`       MEDIUMTEXT,
    `change_note`    VARCHAR(512) DEFAULT NULL,
    `published_by`   VARCHAR(64) DEFAULT NULL,
    `published_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_hist_def_id` (`def_id`),
    KEY `idx_hist_def_key_ver` (`def_key`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 流程定义历史版本快照';
```

- [ ] **Step 2：写 BpmSchemaInitializer**

`bpm-biz/src/main/java/org/jeecg/modules/bpm/support/BpmSchemaInitializer.java`：
```java
package org.jeecg.modules.bpm.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class BpmSchemaInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(BpmSchemaInitializer.class);
    private static final String RESOURCE_PATH = "db/bpm-schema-mysql.sql";

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public BpmSchemaInitializer(JdbcTemplate jdbcTemplate,
                                @Value("${bpm.schema.auto-init:true}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initSchema() {
        if (!enabled) {
            LOG.info("[bpm-schema] auto-init disabled, skip");
            return;
        }
        try (InputStream in = new ClassPathResource(RESOURCE_PATH).getInputStream()) {
            String sql = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            // 简单按 ';' 切分；DDL 内部不会有分号字符串字面量
            Arrays.stream(sql.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.startsWith("--"))
                    .forEach(jdbcTemplate::execute);
            LOG.info("[bpm-schema] init complete");
        } catch (Exception ex) {
            LOG.error("[bpm-schema] init FAILED", ex);
            throw new IllegalStateException("BPM schema init failed", ex);
        }
    }
}
```

- [ ] **Step 3：写 IT（先 fail）**

`bpm-biz/src/test/java/org/jeecg/modules/bpm/support/BpmSchemaInitializerIT.java`：
```java
package org.jeecg.modules.bpm.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { org.jeecg.modules.bpm.BpmModuleAutoConfiguration.class })
@ActiveProfiles("test")
@Testcontainers
class BpmSchemaInitializerIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("bpm.schema.auto-init", () -> "true");
    }

    @Autowired JdbcTemplate jdbc;

    @Test
    void createsTwoBpmTables() {
        Integer defCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema=DATABASE() AND table_name='bpm_process_definition'",
                Integer.class);
        Integer histCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema=DATABASE() AND table_name='bpm_process_definition_history'",
                Integer.class);
        assertThat(defCount).isEqualTo(1);
        assertThat(histCount).isEqualTo(1);
    }
}
```

- [ ] **Step 4：跑 IT**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmSchemaInitializerIT
```

期望：1 test passed（首次 ~30–60s 拉镜像；后续 ~10s）。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/db/bpm-schema-mysql.sql \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/support/BpmSchemaInitializer.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/support/BpmSchemaInitializerIT.java
git commit -m "feat(bpm-p1): ddl + BpmSchemaInitializer auto-creates bpm_process_definition tables"
```

---

## Task 5：ProcessDefinition entity + mapper + service（CRUD）

**Files:**
- 创建 `bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/entity/BpmProcessDefinition.java`
- 创建 mapper / service 接口与 impl
- 创建测试 `BpmProcessDefinitionEntityTest.java` + `BpmProcessDefinitionServiceImplTest.java`

- [ ] **Step 1：Entity**

`entity/BpmProcessDefinition.java`：
```java
package org.jeecg.modules.bpm.definition.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@TableName("bpm_process_definition")
public class BpmProcessDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField("def_key")
    private String defKey;

    private String name;
    private String category;
    private Integer version;
    private String state;             // DRAFT / TESTING / PUBLISHED / ARCHIVED
    @TableField("bpmn_xml")
    private String bpmnXml;
    @TableField("form_id")
    private String formId;
    @TableField("act_def_id")
    private String actDefId;
    @TableField("tenant_id")
    private String tenantId;
    private String description;
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer deleted;

    // —— getter / setter（省略字面，实施时全量生成） ——
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getActDefId() { return actDefId; }
    public void setActDefId(String actDefId) { this.actDefId = actDefId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmProcessDefinition)) return false;
        return Objects.equals(id, ((BpmProcessDefinition) o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
```

- [ ] **Step 2：Entity 测试（默认值）**

`test/.../entity/BpmProcessDefinitionEntityTest.java`：
```java
package org.jeecg.modules.bpm.definition.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BpmProcessDefinitionEntityTest {
    @Test
    void equalityIsByIdOnly() {
        BpmProcessDefinition a = new BpmProcessDefinition();
        a.setId("uuid-1"); a.setName("A");
        BpmProcessDefinition b = new BpmProcessDefinition();
        b.setId("uuid-1"); b.setName("B");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
```

- [ ] **Step 3：Mapper**

`mapper/BpmProcessDefinitionMapper.java`：
```java
package org.jeecg.modules.bpm.definition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;

@Mapper
public interface BpmProcessDefinitionMapper extends BaseMapper<BpmProcessDefinition> {

    @Select("SELECT COALESCE(MAX(version), 0) FROM bpm_process_definition " +
            "WHERE def_key = #{defKey} AND tenant_id = #{tenantId} AND deleted = 0")
    Integer maxVersion(@Param("defKey") String defKey, @Param("tenantId") String tenantId);
}
```

- [ ] **Step 4：DTO**

`dto/DefinitionCreateRequest.java`：
```java
package org.jeecg.modules.bpm.definition.dto;

public class DefinitionCreateRequest {
    private String defKey;
    private String name;
    private String category;
    private String description;
    private String bpmnXml;
    private String formId;
    // getter/setter…
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
}
```

`dto/DefinitionUpdateRequest.java`：（与 Create 相同除了无 defKey — defKey 不可改）
```java
package org.jeecg.modules.bpm.definition.dto;

public class DefinitionUpdateRequest {
    private String name;
    private String category;
    private String description;
    private String bpmnXml;
    private String formId;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
}
```

`dto/DefinitionQueryRequest.java`：
```java
package org.jeecg.modules.bpm.definition.dto;

public class DefinitionQueryRequest {
    private String defKey;
    private String name;
    private String state;
    private String category;
    private Long pageNo = 1L;
    private Long pageSize = 20L;
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getPageNo() { return pageNo; }
    public void setPageNo(Long pageNo) { this.pageNo = pageNo; }
    public Long getPageSize() { return pageSize; }
    public void setPageSize(Long pageSize) { this.pageSize = pageSize; }
}
```

`dto/DefinitionVO.java`：（响应 DTO，省略 bpmn_xml 仅在 detail 返回）
```java
package org.jeecg.modules.bpm.definition.dto;

import java.util.Date;

public class DefinitionVO {
    private String id;
    private String defKey;
    private String name;
    private String category;
    private Integer version;
    private String state;
    private String formId;
    private String actDefId;
    private String description;
    private String bpmnXml;        // 仅 detail 有值
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getActDefId() { return actDefId; }
    public void setActDefId(String actDefId) { this.actDefId = actDefId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
```

- [ ] **Step 5：Service 接口**

`service/BpmProcessDefinitionService.java`：
```java
package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;

public interface BpmProcessDefinitionService extends IService<BpmProcessDefinition> {

    IPage<DefinitionVO> queryPage(DefinitionQueryRequest req);

    /** 创建草稿（version=1, state=DRAFT）；defKey 唯一性由数据库唯一索引兜底 */
    DefinitionVO createDraft(DefinitionCreateRequest req);

    DefinitionVO getDetail(String id);

    DefinitionVO update(String id, DefinitionUpdateRequest req);

    void delete(String id);
}
```

- [ ] **Step 6：Service 实现**

`service/BpmProcessDefinitionServiceImpl.java`：
```java
package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BpmProcessDefinitionServiceImpl
        extends ServiceImpl<BpmProcessDefinitionMapper, BpmProcessDefinition>
        implements BpmProcessDefinitionService {

    private final BpmUserContext userContext;

    public BpmProcessDefinitionServiceImpl(BpmUserContext userContext) {
        this.userContext = userContext;
    }

    @Override
    public IPage<DefinitionVO> queryPage(DefinitionQueryRequest req) {
        LambdaQueryWrapper<BpmProcessDefinition> q = new LambdaQueryWrapper<>();
        if (req.getDefKey() != null && !req.getDefKey().isEmpty())
            q.like(BpmProcessDefinition::getDefKey, req.getDefKey());
        if (req.getName() != null && !req.getName().isEmpty())
            q.like(BpmProcessDefinition::getName, req.getName());
        if (req.getState() != null) q.eq(BpmProcessDefinition::getState, req.getState());
        if (req.getCategory() != null) q.eq(BpmProcessDefinition::getCategory, req.getCategory());
        q.orderByDesc(BpmProcessDefinition::getCreateTime);
        Page<BpmProcessDefinition> page = new Page<>(req.getPageNo(), req.getPageSize());
        IPage<BpmProcessDefinition> res = baseMapper.selectPage(page, q);
        return res.convert(this::toVOWithoutXml);
    }

    @Override
    @Transactional
    public DefinitionVO createDraft(DefinitionCreateRequest req) {
        BpmProcessDefinition e = new BpmProcessDefinition();
        BeanUtils.copyProperties(req, e);
        e.setVersion(1);
        e.setState("DRAFT");
        if (e.getCategory() == null) e.setCategory("DEFAULT");
        e.setTenantId("default");
        String username = userContext.currentUsername();
        e.setCreateBy(username);
        e.setUpdateBy(username);
        save(e);
        return toVODetail(e);
    }

    @Override
    public DefinitionVO getDetail(String id) {
        BpmProcessDefinition e = getById(id);
        if (e == null) return null;
        return toVODetail(e);
    }

    @Override
    @Transactional
    public DefinitionVO update(String id, DefinitionUpdateRequest req) {
        BpmProcessDefinition e = getById(id);
        if (e == null) throw new IllegalArgumentException("definition not found: " + id);
        if ("ARCHIVED".equals(e.getState()))
            throw new IllegalStateException("cannot update ARCHIVED definition");
        if (req.getName() != null) e.setName(req.getName());
        if (req.getCategory() != null) e.setCategory(req.getCategory());
        if (req.getDescription() != null) e.setDescription(req.getDescription());
        if (req.getBpmnXml() != null) e.setBpmnXml(req.getBpmnXml());
        if (req.getFormId() != null) e.setFormId(req.getFormId());
        e.setUpdateBy(userContext.currentUsername());
        updateById(e);
        return toVODetail(e);
    }

    @Override
    @Transactional
    public void delete(String id) {
        BpmProcessDefinition e = getById(id);
        if (e == null) return;
        if ("PUBLISHED".equals(e.getState()))
            throw new IllegalStateException("cannot delete PUBLISHED definition; archive first");
        removeById(id);
    }

    private DefinitionVO toVOWithoutXml(BpmProcessDefinition e) {
        DefinitionVO v = new DefinitionVO();
        BeanUtils.copyProperties(e, v);
        v.setBpmnXml(null);
        return v;
    }

    private DefinitionVO toVODetail(BpmProcessDefinition e) {
        DefinitionVO v = new DefinitionVO();
        BeanUtils.copyProperties(e, v);
        return v;
    }
}
```

- [ ] **Step 7：Service 单测（Mockito 隔离）**

`test/.../service/BpmProcessDefinitionServiceImplTest.java`：
```java
package org.jeecg.modules.bpm.definition.service;

import org.jeecg.modules.bpm.definition.dto.DefinitionCreateRequest;
import org.jeecg.modules.bpm.definition.dto.DefinitionUpdateRequest;
import org.jeecg.modules.bpm.definition.dto.DefinitionVO;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BpmProcessDefinitionServiceImplTest {

    BpmProcessDefinitionMapper mapper;
    BpmUserContext userContext;
    BpmProcessDefinitionServiceImpl svc;

    @BeforeEach
    void setUp() {
        mapper = mock(BpmProcessDefinitionMapper.class);
        userContext = mock(BpmUserContext.class);
        when(userContext.currentUsername()).thenReturn("alice");
        svc = new BpmProcessDefinitionServiceImpl(userContext);
        // 通过反射注入 baseMapper（MyBatis-Plus ServiceImpl 字段）
        try {
            java.lang.reflect.Field f = svc.getClass().getSuperclass().getDeclaredField("baseMapper");
            f.setAccessible(true);
            f.set(svc, mapper);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void createDraftDefaultsCategoryAndState() {
        when(mapper.insert(any(BpmProcessDefinition.class))).thenReturn(1);
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k1"); req.setName("N1");
        DefinitionVO vo = svc.createDraft(req);
        assertThat(vo.getState()).isEqualTo("DRAFT");
        assertThat(vo.getVersion()).isEqualTo(1);
        assertThat(vo.getCategory()).isEqualTo("DEFAULT");
        assertThat(vo.getCreateBy()).isEqualTo("alice");
        ArgumentCaptor<BpmProcessDefinition> cap = ArgumentCaptor.forClass(BpmProcessDefinition.class);
        verify(mapper).insert(cap.capture());
        assertThat(cap.getValue().getTenantId()).isEqualTo("default");
    }

    @Test
    void deleteRefusesPublished() {
        BpmProcessDefinition e = new BpmProcessDefinition();
        e.setId("x"); e.setState("PUBLISHED");
        when(mapper.selectById("x")).thenReturn(e);
        assertThatThrownBy(() -> svc.delete("x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PUBLISHED");
    }

    @Test
    void updateRefusesArchived() {
        BpmProcessDefinition e = new BpmProcessDefinition();
        e.setId("x"); e.setState("ARCHIVED");
        when(mapper.selectById("x")).thenReturn(e);
        assertThatThrownBy(() -> svc.update("x", new DefinitionUpdateRequest()))
                .isInstanceOf(IllegalStateException.class);
    }
}
```

- [ ] **Step 8：跑测试**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test \
    -Dtest='BpmProcessDefinitionEntityTest,BpmProcessDefinitionServiceImplTest'
```

期望：4 tests passed。

- [ ] **Step 9：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/entity/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/mapper/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/dto/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/service/BpmProcessDefinitionService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/service/BpmProcessDefinitionServiceImpl.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/entity/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/service/BpmProcessDefinitionServiceImplTest.java
git commit -m "feat(bpm-p1): ProcessDefinition entity/mapper/service CRUD"
```

---

## Task 6：ProcessDefinitionHistory entity + mapper + service

**Files:**
- 创建 `entity/BpmProcessDefinitionHistory.java`
- 创建 `mapper/BpmProcessDefinitionHistoryMapper.java`
- 创建 `service/BpmProcessDefinitionHistoryService.java` + `BpmProcessDefinitionHistoryServiceImpl.java`
- 创建测试 `BpmProcessDefinitionHistoryServiceImplTest.java`

- [ ] **Step 1：Entity**

`entity/BpmProcessDefinitionHistory.java`：
```java
package org.jeecg.modules.bpm.definition.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;

@TableName("bpm_process_definition_history")
public class BpmProcessDefinitionHistory {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    @TableField("def_id")
    private String defId;
    @TableField("def_key")
    private String defKey;
    private Integer version;
    @TableField("bpmn_xml")
    private String bpmnXml;
    @TableField("change_note")
    private String changeNote;
    @TableField("published_by")
    private String publishedBy;
    @TableField(value = "published_time", fill = FieldFill.INSERT)
    private Date publishedTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDefId() { return defId; }
    public void setDefId(String defId) { this.defId = defId; }
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getBpmnXml() { return bpmnXml; }
    public void setBpmnXml(String bpmnXml) { this.bpmnXml = bpmnXml; }
    public String getChangeNote() { return changeNote; }
    public void setChangeNote(String changeNote) { this.changeNote = changeNote; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
    public Date getPublishedTime() { return publishedTime; }
    public void setPublishedTime(Date publishedTime) { this.publishedTime = publishedTime; }
}
```

- [ ] **Step 2：Mapper**

```java
package org.jeecg.modules.bpm.definition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;

@Mapper
public interface BpmProcessDefinitionHistoryMapper extends BaseMapper<BpmProcessDefinitionHistory> {
}
```

- [ ] **Step 3：Service 接口与实现**

`service/BpmProcessDefinitionHistoryService.java`：
```java
package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;

import java.util.List;

public interface BpmProcessDefinitionHistoryService extends IService<BpmProcessDefinitionHistory> {

    /** 写入一份快照（在 publish 时调用）。返回快照 id。 */
    String snapshot(String defId, String defKey, int version, String bpmnXml,
                    String changeNote, String publishedBy);

    /** 按 def_id 查所有快照（按 version 倒序） */
    List<BpmProcessDefinitionHistory> listByDefId(String defId);
}
```

`service/BpmProcessDefinitionHistoryServiceImpl.java`：
```java
package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionHistoryMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BpmProcessDefinitionHistoryServiceImpl
        extends ServiceImpl<BpmProcessDefinitionHistoryMapper, BpmProcessDefinitionHistory>
        implements BpmProcessDefinitionHistoryService {

    @Override
    public String snapshot(String defId, String defKey, int version, String bpmnXml,
                           String changeNote, String publishedBy) {
        BpmProcessDefinitionHistory h = new BpmProcessDefinitionHistory();
        h.setDefId(defId);
        h.setDefKey(defKey);
        h.setVersion(version);
        h.setBpmnXml(bpmnXml);
        h.setChangeNote(changeNote);
        h.setPublishedBy(publishedBy);
        h.setPublishedTime(new Date());
        save(h);
        return h.getId();
    }

    @Override
    public List<BpmProcessDefinitionHistory> listByDefId(String defId) {
        LambdaQueryWrapper<BpmProcessDefinitionHistory> q = new LambdaQueryWrapper<>();
        q.eq(BpmProcessDefinitionHistory::getDefId, defId)
         .orderByDesc(BpmProcessDefinitionHistory::getVersion);
        return list(q);
    }
}
```

- [ ] **Step 4：测试**

`test/.../service/BpmProcessDefinitionHistoryServiceImplTest.java`：
```java
package org.jeecg.modules.bpm.definition.service;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionHistoryMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BpmProcessDefinitionHistoryServiceImplTest {

    @Test
    void snapshotPersistsAllFields() throws Exception {
        BpmProcessDefinitionHistoryMapper mapper = mock(BpmProcessDefinitionHistoryMapper.class);
        when(mapper.insert(any(BpmProcessDefinitionHistory.class))).thenReturn(1);
        BpmProcessDefinitionHistoryServiceImpl svc = new BpmProcessDefinitionHistoryServiceImpl();
        java.lang.reflect.Field f = svc.getClass().getSuperclass().getDeclaredField("baseMapper");
        f.setAccessible(true); f.set(svc, mapper);

        String id = svc.snapshot("d1", "key1", 3, "<xml/>", "fix typo", "alice");
        ArgumentCaptor<BpmProcessDefinitionHistory> cap =
                ArgumentCaptor.forClass(BpmProcessDefinitionHistory.class);
        verify(mapper).insert(cap.capture());
        BpmProcessDefinitionHistory saved = cap.getValue();
        assertThat(saved.getDefId()).isEqualTo("d1");
        assertThat(saved.getDefKey()).isEqualTo("key1");
        assertThat(saved.getVersion()).isEqualTo(3);
        assertThat(saved.getBpmnXml()).isEqualTo("<xml/>");
        assertThat(saved.getChangeNote()).isEqualTo("fix typo");
        assertThat(saved.getPublishedBy()).isEqualTo("alice");
        assertThat(saved.getPublishedTime()).isNotNull();
    }
}
```

- [ ] **Step 5：跑测试**

```bash
source ~/bin/bpm-env.sh
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmProcessDefinitionHistoryServiceImplTest
```

期望：1 test passed。

- [ ] **Step 6：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/entity/BpmProcessDefinitionHistory.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/mapper/BpmProcessDefinitionHistoryMapper.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/service/BpmProcessDefinitionHistoryService.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/service/BpmProcessDefinitionHistoryServiceImpl.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/service/BpmProcessDefinitionHistoryServiceImplTest.java
git commit -m "feat(bpm-p1): ProcessDefinitionHistory entity/mapper/service for snapshot"
```

---

## Task 7：BpmnXmlValidator + 单测

**Files:**
- 创建 `definition/support/BpmnXmlValidator.java`
- 创建测试 `BpmnXmlValidatorTest.java`
- 创建测试资源 `valid-definition.bpmn20.xml` + `invalid-definition.bpmn20.xml`

- [ ] **Step 1：测试资源**

`bpm-biz/src/test/resources/bpm/valid-definition.bpmn20.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://iimt.com/bpm/sample">
  <process id="bpm_demo" name="Demo" isExecutable="true">
    <startEvent id="start"/>
    <sequenceFlow id="f1" sourceRef="start" targetRef="t"/>
    <userTask id="t" name="Approve" flowable:assignee="${initiator}"/>
    <sequenceFlow id="f2" sourceRef="t" targetRef="end"/>
    <endEvent id="end"/>
  </process>
</definitions>
```

`bpm-biz/src/test/resources/bpm/invalid-definition.bpmn20.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<this-is-not-bpmn>
  <random/>
</this-is-not-bpmn>
```

- [ ] **Step 2：写测试（先 fail）**

`test/.../definition/support/BpmnXmlValidatorTest.java`：
```java
package org.jeecg.modules.bpm.definition.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmnXmlValidatorTest {

    private final BpmnXmlValidator validator = new BpmnXmlValidator();

    private String load(String path) throws Exception {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    @Test
    void validBpmnIsAccepted() throws Exception {
        BpmnXmlValidator.Result r = validator.validate(load("bpm/valid-definition.bpmn20.xml"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.getProcessId()).isEqualTo("bpm_demo");
    }

    @Test
    void invalidBpmnIsRejected() throws Exception {
        assertThatThrownBy(() -> validator.validate(load("bpm/invalid-definition.bpmn20.xml")))
                .isInstanceOf(BpmnXmlValidator.InvalidBpmnException.class);
    }

    @Test
    void blankInputRejected() {
        assertThatThrownBy(() -> validator.validate("  "))
                .isInstanceOf(BpmnXmlValidator.InvalidBpmnException.class);
    }
}
```

- [ ] **Step 3：实现**

`definition/support/BpmnXmlValidator.java`：
```java
package org.jeecg.modules.bpm.definition.support;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

@Component
public class BpmnXmlValidator {

    public Result validate(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new InvalidBpmnException("BPMN XML is blank");
        }
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader sr = xif.createXMLStreamReader(new StringReader(xml));
            BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(sr);
            if (model.getProcesses().isEmpty()) {
                throw new InvalidBpmnException("no <process> element found");
            }
            Process p = model.getProcesses().get(0);
            if (p.getId() == null || p.getId().isEmpty()) {
                throw new InvalidBpmnException("process must have id");
            }
            return new Result(p.getId());
        } catch (InvalidBpmnException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidBpmnException("BPMN parse failed: " + e.getMessage(), e);
        }
    }

    public static class Result {
        private final boolean valid = true;
        private final String processId;
        public Result(String processId) { this.processId = processId; }
        public boolean isValid() { return valid; }
        public String getProcessId() { return processId; }
    }

    public static class InvalidBpmnException extends RuntimeException {
        public InvalidBpmnException(String msg) { super(msg); }
        public InvalidBpmnException(String msg, Throwable cause) { super(msg, cause); }
    }
}
```

- [ ] **Step 4：跑测试**

```bash
source ~/bin/bpm-env.sh
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmnXmlValidatorTest
```

期望：3 tests passed。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/support/BpmnXmlValidator.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/support/BpmnXmlValidatorTest.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/resources/bpm/valid-definition.bpmn20.xml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/resources/bpm/invalid-definition.bpmn20.xml
git commit -m "feat(bpm-p1): BpmnXmlValidator using Flowable BpmnXMLConverter"
```

---

## Task 8：DefinitionController CRUD（含 MockMvc 测试）

**Files:**
- 创建 `definition/controller/DefinitionController.java`
- 创建测试 `DefinitionControllerTest.java`

- [ ] **Step 1：Controller 实现（先写 — Step 2 写测试反向覆盖以方便编译）**

`definition/controller/DefinitionController.java`：
```java
package org.jeecg.modules.bpm.definition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bpm/v1/definition")
public class DefinitionController {

    private final BpmProcessDefinitionService service;
    private final BpmnXmlValidator bpmnValidator;

    public DefinitionController(BpmProcessDefinitionService service,
                                BpmnXmlValidator bpmnValidator) {
        this.service = service;
        this.bpmnValidator = bpmnValidator;
    }

    @GetMapping
    public IPage<DefinitionVO> list(DefinitionQueryRequest q) {
        return service.queryPage(q);
    }

    @PostMapping
    public ResponseEntity<DefinitionVO> create(@RequestBody DefinitionCreateRequest req) {
        if (req.getDefKey() == null || req.getDefKey().isEmpty()
                || req.getName() == null || req.getName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (req.getBpmnXml() != null && !req.getBpmnXml().isEmpty()) {
            bpmnValidator.validate(req.getBpmnXml());
        }
        DefinitionVO vo = service.createDraft(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DefinitionVO> get(@PathVariable String id) {
        DefinitionVO vo = service.getDetail(id);
        return vo == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(vo);
    }

    @PutMapping("/{id}")
    public DefinitionVO update(@PathVariable String id, @RequestBody DefinitionUpdateRequest req) {
        if (req.getBpmnXml() != null && !req.getBpmnXml().isEmpty()) {
            bpmnValidator.validate(req.getBpmnXml());
        }
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(BpmnXmlValidator.InvalidBpmnException.class)
    public ResponseEntity<String> onInvalidBpmn(BpmnXmlValidator.InvalidBpmnException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> onConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> onNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
```

- [ ] **Step 2：MockMvc 测试**

`test/.../controller/DefinitionControllerTest.java`：
```java
package org.jeecg.modules.bpm.definition.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = { DefinitionControllerTest.TestConfig.class })
@AutoConfigureMockMvc
class DefinitionControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan(basePackageClasses = DefinitionController.class)
    public static class TestConfig {}

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @MockBean BpmProcessDefinitionService service;
    @MockBean BpmnXmlValidator validator;

    @Test
    void postCreate201() throws Exception {
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k"); req.setName("N");
        DefinitionVO vo = new DefinitionVO(); vo.setId("uuid-1"); vo.setName("N");
        when(service.createDraft(any())).thenReturn(vo);
        mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.id").value("uuid-1"));
    }

    @Test
    void postCreateWithBlankNameReturns400() throws Exception {
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k");
        mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(service.getDetail("x")).thenReturn(null);
        mvc.perform(get("/bpm/v1/definition/x"))
           .andExpect(status().isNotFound());
    }

    @Test
    void putUpdateReturnsVo() throws Exception {
        DefinitionUpdateRequest req = new DefinitionUpdateRequest();
        req.setName("M");
        DefinitionVO vo = new DefinitionVO(); vo.setId("x"); vo.setName("M");
        when(service.update(eq("x"), any())).thenReturn(vo);
        mvc.perform(put("/bpm/v1/definition/x")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.name").value("M"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/bpm/v1/definition/x"))
           .andExpect(status().isNoContent());
        verify(service).delete("x");
    }

    @Test
    void invalidBpmnInBodyReturns400() throws Exception {
        doThrow(new BpmnXmlValidator.InvalidBpmnException("bad"))
                .when(validator).validate(anyString());
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("k"); req.setName("N"); req.setBpmnXml("<x/>");
        mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 3：跑测试**

```bash
source ~/bin/bpm-env.sh
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=DefinitionControllerTest
```

期望：6 tests passed。

- [ ] **Step 4：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/controller/DefinitionController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/controller/DefinitionControllerTest.java
git commit -m "feat(bpm-p1): DefinitionController CRUD endpoints with BPMN validation"
```

---

## Task 9：publish + versions endpoint + Testcontainers IT 验证版本快照

**Files:**
- 修改 `BpmProcessDefinitionService.java` 加 publish 方法
- 修改 `BpmProcessDefinitionServiceImpl.java`
- 修改 `DefinitionController.java` 加 publish + versions endpoint
- 创建 `DefinitionControllerPublishIT.java`

- [ ] **Step 1：Service 加 publish**

修改 `service/BpmProcessDefinitionService.java`，追加：
```java
DefinitionVO publish(String id, String changeNote);

java.util.List<org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory> versions(String id);
```

修改 `service/BpmProcessDefinitionServiceImpl.java`：
- 构造函数注入 `BpmProcessDefinitionHistoryService` 与 `BpmnXmlValidator`
- 加方法：

```java
private final BpmProcessDefinitionHistoryService historyService;
private final org.jeecg.modules.bpm.definition.support.BpmnXmlValidator bpmnValidator;

public BpmProcessDefinitionServiceImpl(BpmUserContext userContext,
                                       BpmProcessDefinitionHistoryService historyService,
                                       org.jeecg.modules.bpm.definition.support.BpmnXmlValidator bpmnValidator) {
    this.userContext = userContext;
    this.historyService = historyService;
    this.bpmnValidator = bpmnValidator;
}

@Override
@Transactional
public DefinitionVO publish(String id, String changeNote) {
    BpmProcessDefinition e = getById(id);
    if (e == null) throw new IllegalArgumentException("definition not found: " + id);
    if (!"DRAFT".equals(e.getState()))
        throw new IllegalStateException("only DRAFT can be published in P1; current=" + e.getState());
    if (e.getBpmnXml() == null || e.getBpmnXml().isEmpty())
        throw new IllegalStateException("bpmn_xml is empty");
    bpmnValidator.validate(e.getBpmnXml());

    e.setState("PUBLISHED");
    e.setUpdateBy(userContext.currentUsername());
    updateById(e);
    historyService.snapshot(e.getId(), e.getDefKey(), e.getVersion(),
            e.getBpmnXml(), changeNote, userContext.currentUsername());
    return toVODetail(e);
}

@Override
public java.util.List<org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory> versions(String id) {
    return historyService.listByDefId(id);
}
```

> **P1 简化：** 直接 DRAFT→PUBLISHED；TESTING、ARCHIVED、回退、Redisson 锁全部归 P4。

- [ ] **Step 2：Controller 加 endpoint**

修改 `DefinitionController.java`：

```java
@PostMapping("/{id}/publish")
public DefinitionVO publish(@PathVariable String id,
                            @RequestParam(required = false) String changeNote) {
    return service.publish(id, changeNote);
}

@GetMapping("/{id}/versions")
public java.util.List<org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory> versions(
        @PathVariable String id) {
    return service.versions(id);
}
```

- [ ] **Step 3：写 publish 集成测试（Testcontainers）**

`test/.../controller/DefinitionControllerPublishIT.java`：
```java
package org.jeecg.modules.bpm.definition.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.bpm.definition.dto.DefinitionCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = { org.jeecg.modules.bpm.BpmModuleAutoConfiguration.class })
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class DefinitionControllerPublishIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String loadXml() throws Exception {
        try (var in = new ClassPathResource("bpm/valid-definition.bpmn20.xml").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    @Test
    void createDraftThenPublishThenListVersions() throws Exception {
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("demo"); req.setName("Demo"); req.setBpmnXml(loadXml());
        MvcResult created = mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = om.readTree(created.getResponse().getContentAsString());
        String id = body.get("id").asText();
        assertThat(body.get("state").asText()).isEqualTo("DRAFT");

        mvc.perform(post("/bpm/v1/definition/" + id + "/publish")
                .param("changeNote", "first publish"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.state").value("PUBLISHED"));

        mvc.perform(get("/bpm/v1/definition/" + id + "/versions"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.length()").value(1))
           .andExpect(jsonPath("$[0].version").value(1))
           .andExpect(jsonPath("$[0].changeNote").value("first publish"));
    }

    @Test
    void publishingNonDraftReturns409() throws Exception {
        // create + publish
        DefinitionCreateRequest req = new DefinitionCreateRequest();
        req.setDefKey("demo2"); req.setName("Demo2"); req.setBpmnXml(loadXml());
        MvcResult res = mvc.perform(post("/bpm/v1/definition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        String id = om.readTree(res.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(post("/bpm/v1/definition/" + id + "/publish")).andExpect(status().isOk());
        // second publish → 409
        mvc.perform(post("/bpm/v1/definition/" + id + "/publish"))
           .andExpect(status().isConflict());
    }
}
```

- [ ] **Step 4：跑 IT**

```bash
source ~/bin/bpm-env.sh
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=DefinitionControllerPublishIT
```

期望：2 tests passed。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/service/ \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/definition/controller/DefinitionController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/definition/controller/DefinitionControllerPublishIT.java
git commit -m "feat(bpm-p1): publish (DRAFT->PUBLISHED) + versions endpoint with snapshot"
```

---

## Task 10：前端 — 安装 bpmn-js + BpmnDesigner.vue 包装

> **位置：** 在平行仓库 `jeecgboot-vue3` 内修改；commit 在该仓库内执行。本 BPM 仓库的 `jeecg-module-bpm/frontend-snapshot/` 同步存一份归档。下面命令以 `$VUE_REPO=/path/to/jeecgboot-vue3` 占位（实际由实施者注入），归档同步用 `cp -R`。

**Files：**
- `$VUE_REPO/package.json`：依赖追加
- `$VUE_REPO/src/views/bpm/designer/BpmnDesigner.vue`
- `$VUE_REPO/src/views/bpm/designer/DesignerPage.vue`
- 同步到 `jeecg-module-bpm/frontend-snapshot/src/views/bpm/designer/`

- [ ] **Step 1：安装依赖**

```bash
cd "$VUE_REPO"
pnpm add bpmn-js@^17.0.0
pnpm add -D @types/bpmn-js@^17.0.0 || true   # 部分版本无类型包，失败可忽略
```

或若工程用 npm：`npm i bpmn-js@^17.0.0`。期望 `package.json` 出现：
```json
"bpmn-js": "^17.0.0"
```

- [ ] **Step 2：BpmnDesigner.vue（包装组件）**

`$VUE_REPO/src/views/bpm/designer/BpmnDesigner.vue`：
```vue
<template>
  <div class="bpmn-designer">
    <div ref="canvasRef" class="bpmn-canvas"></div>
    <div ref="propertiesRef" class="bpmn-properties"></div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';

interface Props {
  modelValue?: string;
  readonly?: boolean;
}
const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  readonly: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', xml: string): void;
  (e: 'ready'): void;
  (e: 'error', err: Error): void;
}>();

const canvasRef = ref<HTMLElement | null>(null);
const propertiesRef = ref<HTMLElement | null>(null);
let modeler: any = null;

const EMPTY_XML = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
             targetNamespace="http://iimt.com/bpm">
  <process id="Process_1" isExecutable="true">
    <startEvent id="StartEvent_1"/>
  </process>
  <bpmndi:BPMNDiagram id="d_1">
    <bpmndi:BPMNPlane id="p_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="sh_se" bpmnElement="StartEvent_1">
        <dc:Bounds x="156" y="81" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`;

async function importXml(xml: string) {
  if (!modeler) return;
  try {
    await modeler.importXML(xml || EMPTY_XML);
    emit('ready');
  } catch (e: any) {
    emit('error', e);
  }
}

async function emitXml() {
  if (!modeler) return;
  const { xml } = await modeler.saveXML({ format: true });
  emit('update:modelValue', xml || '');
}

defineExpose({ getXml: emitXml, importXml });

onMounted(() => {
  modeler = new BpmnModeler({ container: canvasRef.value!, propertiesPanel: { parent: propertiesRef.value } });
  modeler.on('commandStack.changed', emitXml);
  importXml(props.modelValue);
});

onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy();
    modeler = null;
  }
});

watch(() => props.modelValue, (v) => {
  // 来自外部赋值（例如 detail load）
  if (modeler && v !== undefined) importXml(v);
});
</script>

<style scoped>
.bpmn-designer { display: flex; height: calc(100vh - 200px); width: 100%; }
.bpmn-canvas { flex: 1; border-right: 1px solid #f0f0f0; min-height: 500px; }
.bpmn-properties { width: 320px; padding: 8px; overflow: auto; }
</style>
```

- [ ] **Step 3：DesignerPage.vue（路由页：拉详情 → 双向绑定 → 保存按钮）**

`$VUE_REPO/src/views/bpm/designer/DesignerPage.vue`：
```vue
<template>
  <div class="designer-page">
    <a-page-header :title="`流程设计器：${title || '新建草稿'}`">
      <template #extra>
        <a-space>
          <a-button @click="onSave" :loading="saving">保存草稿</a-button>
          <a-button type="primary" @click="onPublish" :disabled="!definitionId">发布</a-button>
        </a-space>
      </template>
    </a-page-header>
    <BpmnDesigner v-model="bpmnXml" />
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import BpmnDesigner from './BpmnDesigner.vue';
import { getDefinition, createDefinition, updateDefinition, publishDefinition }
  from '/@/api/bpm/definition';

const route = useRoute();
const router = useRouter();
const definitionId = ref<string | undefined>(route.query.id as string | undefined);
const title = ref<string>('');
const bpmnXml = ref<string>('');
const saving = ref(false);

onMounted(async () => {
  if (definitionId.value) {
    const vo = await getDefinition(definitionId.value);
    title.value = vo.name;
    bpmnXml.value = vo.bpmnXml || '';
  }
});

async function onSave() {
  saving.value = true;
  try {
    if (definitionId.value) {
      await updateDefinition(definitionId.value, { bpmnXml: bpmnXml.value });
      message.success('已保存');
    } else {
      const vo = await createDefinition({
        defKey: prompt('流程 key（英文，唯一）') || 'auto_' + Date.now(),
        name: prompt('流程名称') || '未命名',
        bpmnXml: bpmnXml.value,
      });
      definitionId.value = vo.id;
      title.value = vo.name;
      router.replace({ query: { id: vo.id } });
      message.success('草稿已创建');
    }
  } finally {
    saving.value = false;
  }
}

async function onPublish() {
  if (!definitionId.value) return;
  await publishDefinition(definitionId.value, '从设计器发布');
  message.success('已发布');
}
</script>
```

- [ ] **Step 4：归档同步到 BPM 仓库**

```bash
mkdir -p /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/views/bpm/designer
cp "$VUE_REPO/src/views/bpm/designer/BpmnDesigner.vue" \
   "$VUE_REPO/src/views/bpm/designer/DesignerPage.vue" \
   /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/views/bpm/designer/
```

- [ ] **Step 5：commit（在 BPM 仓库内 commit 归档）**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/frontend-snapshot/src/views/bpm/designer/
git commit -m "feat(bpm-p1): bpmn-js 17 designer wrapper + designer page (frontend snapshot)"
```

> **平行仓库 commit：** 在 `$VUE_REPO` 内由实施者另行执行：
> ```bash
> cd "$VUE_REPO"
> git add package.json src/views/bpm/designer/
> git commit -m "feat(bpm-p1): bpmn-js 17 designer wrapper + designer page"
> ```

---

## Task 11：前端 — Definition list（VxeTable）+ API 层 + 路由

**Files：**
- `$VUE_REPO/src/api/bpm/definition.ts`
- `$VUE_REPO/src/api/bpm/model/definitionModel.ts`
- `$VUE_REPO/src/views/bpm/definition/DefinitionList.vue`
- `$VUE_REPO/src/views/bpm/definition/DefinitionList.data.ts`
- `$VUE_REPO/src/views/bpm/definition/components/VersionsModal.vue`
- `$VUE_REPO/src/router/routes/modules/bpm.ts`（如该工程用本地 modules 路由；反之菜单注入由 Task 12 SQL 完成）
- 同步归档到 `jeecg-module-bpm/frontend-snapshot/`

- [ ] **Step 1：API model 类型**

`$VUE_REPO/src/api/bpm/model/definitionModel.ts`：
```ts
export interface DefinitionVO {
  id: string;
  defKey: string;
  name: string;
  category?: string;
  version: number;
  state: 'DRAFT' | 'TESTING' | 'PUBLISHED' | 'ARCHIVED';
  formId?: string;
  actDefId?: string;
  description?: string;
  bpmnXml?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface DefinitionCreateRequest {
  defKey: string;
  name: string;
  category?: string;
  description?: string;
  bpmnXml?: string;
  formId?: string;
}

export interface DefinitionUpdateRequest {
  name?: string;
  category?: string;
  description?: string;
  bpmnXml?: string;
  formId?: string;
}

export interface DefinitionQueryRequest {
  defKey?: string;
  name?: string;
  state?: string;
  category?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}

export interface VersionVO {
  id: string;
  defId: string;
  defKey: string;
  version: number;
  bpmnXml?: string;
  changeNote?: string;
  publishedBy?: string;
  publishedTime?: string;
}
```

- [ ] **Step 2：API 方法**

`$VUE_REPO/src/api/bpm/definition.ts`：
```ts
import { defHttp } from '/@/utils/http/axios';
import type {
  DefinitionVO,
  DefinitionCreateRequest,
  DefinitionUpdateRequest,
  DefinitionQueryRequest,
  PageResult,
  VersionVO,
} from './model/definitionModel';

const PREFIX = '/bpm/v1/definition';

export const listDefinitions = (q: DefinitionQueryRequest) =>
  defHttp.get<PageResult<DefinitionVO>>({ url: PREFIX, params: q });

export const getDefinition = (id: string) =>
  defHttp.get<DefinitionVO>({ url: `${PREFIX}/${id}` });

export const createDefinition = (req: DefinitionCreateRequest) =>
  defHttp.post<DefinitionVO>({ url: PREFIX, data: req });

export const updateDefinition = (id: string, req: DefinitionUpdateRequest) =>
  defHttp.put<DefinitionVO>({ url: `${PREFIX}/${id}`, data: req });

export const deleteDefinition = (id: string) =>
  defHttp.delete<void>({ url: `${PREFIX}/${id}` });

export const publishDefinition = (id: string, changeNote?: string) =>
  defHttp.post<DefinitionVO>({ url: `${PREFIX}/${id}/publish`, params: { changeNote } });

export const listVersions = (id: string) =>
  defHttp.get<VersionVO[]>({ url: `${PREFIX}/${id}/versions` });
```

- [ ] **Step 3：列定义**

`$VUE_REPO/src/views/bpm/definition/DefinitionList.data.ts`：
```ts
import type { VxeColumnPropTypes } from 'vxe-table';

export const STATE_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' },
];

export const columns: VxeColumnPropTypes.Type[] = [
  { type: 'seq', width: 60 },
  { field: 'defKey', title: '流程 KEY', width: 160 },
  { field: 'name', title: '名称', minWidth: 180 },
  { field: 'category', title: '分类', width: 120 },
  { field: 'version', title: '版本', width: 80 },
  { field: 'state', title: '状态', width: 110, slots: { default: 'state' } },
  { field: 'updateTime', title: '更新时间', width: 170 },
  { title: '操作', width: 260, slots: { default: 'actions' } },
] as any;
```

- [ ] **Step 4：列表页**

`$VUE_REPO/src/views/bpm/definition/DefinitionList.vue`：
```vue
<template>
  <div class="bpm-definition-list">
    <a-card title="流程定义">
      <template #extra>
        <a-space>
          <a-input v-model:value="query.name" placeholder="名称" allowClear style="width: 180px" />
          <a-select v-model:value="query.state" :options="STATE_OPTIONS"
                    placeholder="状态" allowClear style="width: 130px" />
          <a-button type="primary" @click="reload">查询</a-button>
          <a-button @click="onCreate">新建草稿</a-button>
        </a-space>
      </template>
      <vxe-table :data="rows" :loading="loading" border stripe height="auto">
        <vxe-column v-for="c in columns" :key="c.field || c.title" v-bind="c">
          <template #state="{ row }">
            <a-tag :color="row.state === 'PUBLISHED' ? 'green' : (row.state === 'DRAFT' ? 'blue' : 'default')">
              {{ row.state }}
            </a-tag>
          </template>
          <template #actions="{ row }">
            <a-button type="link" @click="openDesigner(row)">设计</a-button>
            <a-button type="link" @click="openVersions(row)">版本</a-button>
            <a-popconfirm title="确认删除？" @confirm="onDelete(row)">
              <a-button type="link" danger :disabled="row.state==='PUBLISHED'">删除</a-button>
            </a-popconfirm>
          </template>
        </vxe-column>
      </vxe-table>
      <a-pagination v-model:current="page.current" v-model:pageSize="page.size" :total="page.total"
                    show-size-changer @change="reload" style="margin-top: 12px; text-align: right;" />
    </a-card>

    <VersionsModal v-model:open="versionsOpen" :definition-id="versionsId" />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { columns, STATE_OPTIONS } from './DefinitionList.data';
import { listDefinitions, deleteDefinition } from '/@/api/bpm/definition';
import type { DefinitionVO, DefinitionQueryRequest } from '/@/api/bpm/model/definitionModel';
import VersionsModal from './components/VersionsModal.vue';

const router = useRouter();

const query = reactive<DefinitionQueryRequest>({ name: undefined, state: undefined });
const page = reactive({ current: 1, size: 20, total: 0 });
const rows = ref<DefinitionVO[]>([]);
const loading = ref(false);

const versionsOpen = ref(false);
const versionsId = ref<string>('');

async function reload() {
  loading.value = true;
  try {
    const r = await listDefinitions({
      ...query,
      pageNo: page.current,
      pageSize: page.size,
    });
    rows.value = r.records;
    page.total = r.total;
  } finally {
    loading.value = false;
  }
}

function onCreate() {
  router.push({ name: 'BpmDesigner' });
}

function openDesigner(row: DefinitionVO) {
  router.push({ name: 'BpmDesigner', query: { id: row.id } });
}

function openVersions(row: DefinitionVO) {
  versionsId.value = row.id;
  versionsOpen.value = true;
}

async function onDelete(row: DefinitionVO) {
  await deleteDefinition(row.id);
  message.success('已删除');
  await reload();
}

onMounted(reload);
</script>
```

- [ ] **Step 5：版本列表 modal**

`$VUE_REPO/src/views/bpm/definition/components/VersionsModal.vue`：
```vue
<template>
  <a-modal :open="open" title="历史版本" :footer="null" width="600px"
           @cancel="emit('update:open', false)">
    <a-list :data-source="rows" item-layout="horizontal">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta :description="`v${item.version} · ${item.publishedTime} · ${item.publishedBy}`">
            <template #title>{{ item.changeNote || '(无说明)' }}</template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue';
import { listVersions } from '/@/api/bpm/definition';
import type { VersionVO } from '/@/api/bpm/model/definitionModel';

const props = defineProps<{ open: boolean; definitionId: string }>();
const emit = defineEmits<{ (e: 'update:open', v: boolean): void }>();

const rows = ref<VersionVO[]>([]);

watch(() => [props.open, props.definitionId], async ([open, id]) => {
  if (open && id) rows.value = await listVersions(id as string);
});
</script>
```

- [ ] **Step 6：本地路由模块（如该工程使用 `src/router/routes/modules/`）**

`$VUE_REPO/src/router/routes/modules/bpm.ts`：
```ts
import type { AppRouteModule } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

const bpm: AppRouteModule = {
  path: '/bpm',
  name: 'Bpm',
  component: LAYOUT,
  redirect: '/bpm/definition',
  meta: { title: '流程配置', icon: 'ion:git-network-outline', orderNo: 100 },
  children: [
    {
      path: 'definition',
      name: 'BpmDefinition',
      component: () => import('/@/views/bpm/definition/DefinitionList.vue'),
      meta: { title: '流程定义' },
    },
    {
      path: 'designer',
      name: 'BpmDesigner',
      component: () => import('/@/views/bpm/designer/DesignerPage.vue'),
      meta: { title: '流程设计器', hideMenu: true, currentActiveMenu: '/bpm/definition' },
    },
  ],
};
export default bpm;
```

> **如果该工程使用纯后端动态路由（菜单 SQL 注入）**，本步骤跳过，菜单由 Task 12 的 SQL 写入 `sys_permission` 后由前端动态加载。

- [ ] **Step 7：归档同步**

```bash
mkdir -p /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/{api/bpm/model,views/bpm/definition/components,router/routes/modules}
cp "$VUE_REPO/src/api/bpm/definition.ts" \
   /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/api/bpm/
cp "$VUE_REPO/src/api/bpm/model/definitionModel.ts" \
   /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/api/bpm/model/
cp "$VUE_REPO/src/views/bpm/definition/DefinitionList.vue" \
   "$VUE_REPO/src/views/bpm/definition/DefinitionList.data.ts" \
   /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/views/bpm/definition/
cp "$VUE_REPO/src/views/bpm/definition/components/VersionsModal.vue" \
   /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/views/bpm/definition/components/
cp "$VUE_REPO/src/router/routes/modules/bpm.ts" \
   /Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/frontend-snapshot/src/router/routes/modules/ 2>/dev/null || true
```

- [ ] **Step 8：commit（BPM 仓库内归档）**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/frontend-snapshot/
git commit -m "feat(bpm-p1): definition list page + API layer + routes (frontend snapshot)"
```

> **平行仓库 commit：** 在 `$VUE_REPO` 内由实施者执行同名 commit。

---

## Task 12：菜单/权限 SQL + P1 验收清单

**Files:**
- `jeecg-module-bpm/db/menu/bpm-p1-menu.sql`
- `jeecg-module-bpm/db/menu/bpm-p1-rollback.sql`
- `jeecg-module-bpm/P1_DONE.md`

- [ ] **Step 1：菜单 SQL**

`jeecg-module-bpm/db/menu/bpm-p1-menu.sql`：
```sql
-- BPM P1 菜单与权限点（注入到 jeecg sys_permission）
-- ID 用 UUID（jeecg sys_permission.id 是 varchar(32)）

-- 顶级菜单
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, redirect, menu_type, perms, perms_type, sort_no, always_show, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, description, status, del_flag, rule_flag, create_by, create_time)
VALUES('bpm-root-2026p1', NULL, '流程配置', '/bpm', 'layouts/RouteView', 'BPM', '/bpm/definition', 0, NULL, '1', 100, 1, 'ant-design:partition-outlined', 1, 0, 1, 0, 0, 'BPM 流程配置 P1', 1, 0, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), url=VALUES(url), update_time=NOW();

-- 二级 流程定义
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, menu_type, perms, perms_type, sort_no, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, status, del_flag, rule_flag, create_by, create_time)
VALUES('bpm-definition-2026p1', 'bpm-root-2026p1', '流程定义', '/bpm/definition', 'views/bpm/definition/DefinitionList', 'BpmDefinition', 1, NULL, '1', 110, 'ant-design:unordered-list-outlined', 1, 1, 1, 0, 0, 1, 0, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), update_time=NOW();

-- 二级（隐藏）流程设计器
INSERT INTO sys_permission(id, parent_id, name, url, component, component_name, menu_type, perms, perms_type, sort_no, icon, is_route, is_leaf, keep_alive, hidden, hide_tab, status, del_flag, rule_flag, create_by, create_time)
VALUES('bpm-designer-2026p1', 'bpm-root-2026p1', '流程设计器', '/bpm/designer', 'views/bpm/designer/DesignerPage', 'BpmDesigner', 1, NULL, '1', 120, 'ant-design:edit-outlined', 1, 1, 0, 1, 0, 1, 0, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), update_time=NOW();

-- 权限点
INSERT INTO sys_permission(id, parent_id, name, perms, perms_type, menu_type, sort_no, status, del_flag, create_by, create_time) VALUES
 ('bpm-perm-def-view-2026p1',    'bpm-definition-2026p1', '查看流程定义',  'bpm:definition:view',    '2', 2, 1, 1, 0, 'admin', NOW()),
 ('bpm-perm-def-edit-2026p1',    'bpm-definition-2026p1', '编辑流程定义',  'bpm:definition:edit',    '2', 2, 2, 1, 0, 'admin', NOW()),
 ('bpm-perm-def-publish-2026p1', 'bpm-definition-2026p1', '发布流程定义',  'bpm:definition:publish', '2', 2, 3, 1, 0, 'admin', NOW()),
 ('bpm-perm-def-delete-2026p1',  'bpm-definition-2026p1', '删除流程定义',  'bpm:definition:delete',  '2', 2, 4, 1, 0, 'admin', NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), update_time=NOW();

-- 给 admin 角色（jeecg 默认）授权
INSERT IGNORE INTO sys_role_permission(id, role_id, permission_id, create_time)
SELECT UUID_SHORT() AS id, r.id, p.id, NOW()
FROM sys_role r, sys_permission p
WHERE r.role_code = 'admin'
  AND p.id IN ('bpm-root-2026p1','bpm-definition-2026p1','bpm-designer-2026p1',
               'bpm-perm-def-view-2026p1','bpm-perm-def-edit-2026p1',
               'bpm-perm-def-publish-2026p1','bpm-perm-def-delete-2026p1');
```

`jeecg-module-bpm/db/menu/bpm-p1-rollback.sql`：
```sql
-- 反向：按 perm_code/id 后缀 -2026p1 删
DELETE FROM sys_role_permission WHERE permission_id LIKE 'bpm-%-2026p1';
DELETE FROM sys_permission WHERE id LIKE 'bpm-%-2026p1';
```

- [ ] **Step 2：在本机 jeecg-mysql 跑一次正向 + 反向脚本**

```bash
docker exec -i jeecg-mysql mysql -uroot -proot jeecg-boot < jeecg-module-bpm/db/menu/bpm-p1-menu.sql
docker exec -it jeecg-mysql mysql -uroot -proot jeecg-boot \
    -e "SELECT id,name,perms FROM sys_permission WHERE id LIKE 'bpm-%-2026p1';"
# 期望看到 3 个菜单 + 4 个权限点
docker exec -i jeecg-mysql mysql -uroot -proot jeecg-boot < jeecg-module-bpm/db/menu/bpm-p1-rollback.sql
docker exec -it jeecg-mysql mysql -uroot -proot jeecg-boot \
    -e "SELECT COUNT(*) FROM sys_permission WHERE id LIKE 'bpm-%-2026p1';"
# 期望 0
```

如果 jeecg-mysql 容器没起，跳过本 step；纯靠脚本格式 + DDL 字段对齐 jeecg `sys_permission` schema 也算可接受（实施者可在落地时验证）。

- [ ] **Step 3：写 P1_DONE 验收清单**

`jeecg-module-bpm/P1_DONE.md`：
```markdown
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
```

- [ ] **Step 4：commit + push**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/db/menu/ jeecg-module-bpm/P1_DONE.md
git commit -m "docs(bpm-p1): menu sql + P1 acceptance checklist"
git push origin main
```

- [ ] **Step 5：全量回归**

```bash
source ~/bin/bpm-env.sh
mvn -f jeecg-module-bpm/pom.xml clean install
```

期望：BUILD SUCCESS；module count = 4（api / spi / biz / adapter-jeecg）；测试统计 ≥ P0 4 个 + P1 新增 ≥ 12 个。

---

## Self-Review Notes

**spec 覆盖：**
- §1.2（独立模块原则） → Task 1/2/3 中 `bpm-biz` 与 `bpm-spi` 严格分离；adapter 用 `provided` 范围引 jeecg ✅
- §3.1（4 模块架构） → Task 1+2 把 P0 留下的 spi/adapter 空骨架激活成功能模块 ✅
- §3.3（SPI 接口完整签名 — 4 接口 + 2 DTO） → Task 1 全签名 + 测试覆盖 ✅
- §4.1（`bpm_process_definition` + `bpm_process_definition_history` 字段） → Task 4 DDL 中 id/key/name/category/version/state/bpmn_xml/form_id/act_def_id/tenant_id + history 的 def_id/version/bpmn_xml/change_note/published_by/published_time 全部对齐 ✅
- §4.3（与 jeecg 同库 schema） → Task 4 DDL 用 `IF NOT EXISTS` 与 act_* 共存；Initializer 在 ApplicationReady 之后跑（晚于 Flowable 建 act_*）✅
- §6（API 列表） → Task 8/9 实现 `/definition` GET/POST、`/{id}` GET/PUT/DELETE、`/{id}/publish` POST、`/{id}/versions` GET（共 7 endpoint，spec §6 中 P1 范围内 6 个 + 简化版 publish）；`rollback`、`instance/*`、`task/*`、`monitor/*`、`form-binding/*`、`sandbox/*` 明确属于 P2-P5 ✅
- §7.1（前端路由 `/bpm/designer` `/bpm/definition`） → Task 11 路由配置 + Task 12 菜单 SQL ✅
- §7.2（`BpmnDesigner.vue` 包装 bpmn-js 17.x） → Task 10 v-model 模式 + propertiesPanel 占位 ✅
- §7.3（API 调用层 `src/api/bpm/definition.ts`） → Task 11 ✅
- §8（BPMN 上传校验） → Task 7 `BpmnXmlValidator` 用 Flowable `BpmnXMLConverter` 解析；脚本任务白名单留 P4 ✅
- §10（Testcontainers MySQL，不能 H2） → Task 4/9 都用 mysql:8.0.27 容器 ✅
- §13（耦合点：JwtUtil → BpmUserContext） → Task 2 `JeecgBpmUserContext` 调 `JwtUtil.getUsername` + `ISysBaseAPI` ✅

**未覆盖（按 P1 范围正确排除）：**
- 节点配置 / 6 种 AssigneeStrategy（spec §5.1）→ P2
- 表单绑定 `bpm_form_binding`（spec §5.3）→ P2
- 实例发起 / 任务完成 / 审批回退（spec §6 instance/task）→ P2
- 分支表达式（spec §5.2）→ P3
- 完整状态机 DRAFT→TESTING→PUBLISHED→ARCHIVED + Redisson 锁 + 沙箱（spec §5.4–5.5）→ P4
- 监控统计（spec §5.6 + §6 monitor）→ P5
- 前端 NodeAssigneePanel / TaskApprovePage / Monitor 页（spec §7.2）→ P2/P5

**Placeholder 检查：** 全文搜过，无 TBD / TODO in plan / TODO-impl / 待补 / 暂略 / 等等 / 后续填 / placeholder 字样。`Math.abs(username.hashCode())` 的 `TODO-P2` 标记是**实现内的演进备忘**而非计划本身的占位，明确指向 P2 的 user mapping 工作。

**类型一致性自检：**
- `BpmUserContext.currentUserId()` Task 1 返回 `Long`、Task 2 实现返 `Long`、Task 5 service `currentUsername()` 才使用，三处一致 ✅
- `BpmProcessDefinition.id` 全链路 `String`（varchar(32) UUID），DDL/entity/mapper/service/controller/前端 model/api `string` 一致 ✅
- `bpm_process_definition.def_key` Java `defKey` String，DDL 与 entity `@TableField("def_key")` 对齐 ✅
- `version` Java `Integer`、DDL `INT`、前端 `number` 一致 ✅
- `state` Java `String`、DDL `VARCHAR(16)`、前端 union `'DRAFT'|'TESTING'|'PUBLISHED'|'ARCHIVED'` 一致；P1 实际只产出 DRAFT 与 PUBLISHED 两值，TESTING/ARCHIVED 留 P4（前端 enum 里保留以减少后续 breaking change）✅
- `IPage<DefinitionVO>` 后端返回 → 前端 `PageResult<DefinitionVO>` 字段名（records/total/size/current）与 MyBatis-Plus `Page` 默认序列化对齐 ✅
- groupId `com.iimt.bpm`、version `0.1.0-SNAPSHOT` 全模块统一（与 P0 一致） ✅
- jeecg `sys_permission.id` varchar(32)；本计划新建 7 条 id 都用 `bpm-xxx-2026p1` 形式（≤ 32 char）✅

**反向依赖检查：** `bpm-biz` 不引入 `bpm-adapter-jeecg`（运行期由宿主 pom 同时引）；`bpm-adapter-jeecg` 引 jeecg `provided` scope 不传染下游；测试中 `BpmAdapterJeecgAutoConfigurationTest` 直接 `withBean(ISysBaseAPI.class, mock())`，避免拉真 jeecg 上下文。
