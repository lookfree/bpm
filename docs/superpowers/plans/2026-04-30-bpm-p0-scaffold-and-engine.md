# BPM P0 — 模块脚手架 + Flowable 引擎集成 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 jeecg-boot v0.1.8 工程中新增 `jeecg-module-bpm` 多模块骨架，集成 Flowable 6.8.0 工作流引擎与现有 Shiro+JWT 鉴权链路打通，能在 manage.iimt.org.cn 兼容的部署形态下跑通"部署 hello-world BPMN → 启动实例 → 完成 UserTask"完整链路。

**Architecture:** Maven 父子模块（`jeecg-module-bpm-api` / `jeecg-module-bpm-biz`），Flowable 与 jeecg 共用同一个 MySQL datasource，引擎通过 `flowable-spring-boot-starter` 自动装配，`/bpm/v1/**` 路径在 Shiro `ShiroConfig` 中按 jeecg 既有 `/sys/**` 风格挂 JWT 过滤器。P0 不引入任何 `bpm_*` 业务表，仅依赖 Flowable 自带 25 张 `act_*` 表。

**Tech Stack:** Spring Boot 2.7.10 / Spring Cloud 2021.0.3 / MyBatis-Plus 3.5.3.1 / Apache Shiro 1.12.0 / JWT 3.11.0 / Flowable 6.8.0 / MySQL 8.0.27 / Testcontainers 1.19.x / JUnit 5。

**与设计文档对应章节：** `docs/superpowers/specs/2026-04-30-bpm-module-design.md` §2 P0、§3、§4.3、§6（仅 healthz）、§10。

**前置约定（执行 plan 前必须满足）：**
1. 已有 jeecg-boot 源码本地工作副本，目录结构含 `jeecg-boot-base-core/`、`jeecg-module-system/`、`jeecg-boot-module-system/`（或同名 starter 模块）。Task 1 给出获取与版本对齐方法。
2. 本机能连通一个 MySQL 8.x（用于本地验证；CI 用 Testcontainers）。
3. 本机 JDK 8（jeecg 2.7.x 默认）或 JDK 11，Maven 3.6+。
4. 本仓库 `/Users/wuhoujin/Documents/dev/bpm` 已是一个 git 工作副本，远端 `https://github.com/lookfree/bpm`。

---

## File Structure（本计划新增/修改的全部文件）

**新增（在 jeecg-boot 源码工作副本根目录下）：**
```
jeecg-module-bpm/
├── pom.xml                                          # 父 pom，packaging=pom
├── jeecg-module-bpm-api/
│   ├── pom.xml
│   └── src/main/java/org/jeecg/modules/bpm/api/      # 占位包（后续 phase 放 DTO/Feign）
│       └── package-info.java
└── jeecg-module-bpm-biz/
    ├── pom.xml
    └── src/main/
        ├── java/org/jeecg/modules/bpm/
        │   ├── BpmModuleAutoConfiguration.java       # @Configuration 让 starter 自动加载
        │   ├── config/
        │   │   ├── FlowableConfig.java               # 引擎自定义（id 生成器、historyLevel）
        │   │   └── BpmShiroPathRegistrar.java        # 注册 /bpm/v1/** 给主壳 Shiro 配置
        │   └── controller/
        │       └── BpmHealthController.java          # GET /bpm/v1/healthz
        └── resources/
            ├── META-INF/
            │   └── spring.factories                  # EnableAutoConfiguration → BpmModuleAutoConfiguration
            ├── bpm-application.yml                   # 模块默认配置（被主壳引用）
            └── bpm/
                └── helloworld.bpmn20.xml             # 用于联调的最小流程
```

**修改（在 jeecg-boot 源码工作副本下）：**
- `pom.xml` （根 pom）— `<modules>` 新增 `jeecg-module-bpm`
- `jeecg-boot-module-system/pom.xml` 或 `jeecg-system-start/pom.xml`（哪个是当前发布产物的启动模块，Task 1 中确认） — `<dependencies>` 新增 `jeecg-module-bpm-biz`
- `jeecg-boot-module-system/src/main/resources/application.yml` 或 `application-dev.yml` — 新增 `flowable:` 段
- 主壳 `ShiroConfig.java`（路径 jeecg 不固定，Task 8 会 grep 定位） — 在 `filterChainDefinitionMap` 中放入 `/bpm/v1/**`

**测试新增：**
```
jeecg-module-bpm/jeecg-module-bpm-biz/src/test/
├── java/org/jeecg/modules/bpm/
│   ├── BpmModuleContextTest.java                    # Spring 上下文加载冒烟
│   ├── controller/BpmHealthControllerTest.java      # MockMvc 验证 healthz
│   └── engine/HelloWorldFlowIT.java                 # Testcontainers MySQL + 完整链路
└── resources/
    └── application-test.yml                         # Test 环境覆盖（关闭 Quartz、关闭 Nacos）
```

**最终交付到 `/Users/wuhoujin/Documents/dev/bpm/` 仓库的内容：**
- 上面所有 `jeecg-module-bpm/` 目录树（作为子树拷贝/或 git submodule，Task 0 决策）
- `docs/superpowers/plans/2026-04-30-bpm-p0-scaffold-and-engine.md`（即本文件）

---

## Task 0：仓库布局决策与初始化

**Files:**
- 创建：`/Users/wuhoujin/Documents/dev/bpm/INTEGRATION.md`（解释源码集成方式）
- 修改：`/Users/wuhoujin/Documents/dev/bpm/.gitignore`（新增 `jeecg-boot/` 整树忽略，仅追踪 `jeecg-module-bpm/`）

仓库 `lookfree/bpm` 当前只放需求文档，需要决定：本仓库交付的是**模块独立工程**（只含 `jeecg-module-bpm/`，由集成方手动 drop 到 jeecg-boot），还是**fork 整个 jeecg-boot**。本计划**采用前者**——模块独立工程，理由：（1）jeecg-boot 体积大（>200MB），后续合并 upstream 升级困难；（2）模块独立更便于多客户复用；（3）现网 manage.iimt.org.cn 已部署 v0.1.8，集成方知道自己的版本。

- [ ] **Step 1：写 INTEGRATION.md 说明集成步骤**

```markdown
# BPM 模块集成方法

本仓库交付的是 jeecg-boot 的一个独立 Maven 子模块 `jeecg-module-bpm`，
不包含 jeecg-boot 主体源码。集成方需自行准备 jeecg-boot v0.1.8
（或对应版本）源码，然后按以下步骤接入。

## 1. 准备 jeecg-boot
git clone https://github.com/jeecgboot/jeecg-boot.git
cd jeecg-boot
git checkout v3.6.3   # 与 jeecgboot-vue3 v0.1.8 对应的服务端 tag，需现场确认（C1）

## 2. 把本模块放入
将本仓库 `jeecg-module-bpm/` 整目录拷贝到 jeecg-boot 根目录：
cp -r /path/to/bpm/jeecg-module-bpm /path/to/jeecg-boot/

## 3. 修改 jeecg-boot 根 pom.xml
在 <modules> 下新增：
  <module>jeecg-module-bpm</module>

## 4. 修改启动模块依赖
在 jeecg-boot-module-system/pom.xml（或 jeecg-system-start/pom.xml，依发行版而定）
的 <dependencies> 中新增：
  <dependency>
    <groupId>org.jeecgframework.boot</groupId>
    <artifactId>jeecg-module-bpm-biz</artifactId>
    <version>${project.version}</version>
  </dependency>

## 5. 配置数据源 & Flowable
见 docs/superpowers/plans/2026-04-30-bpm-p0-scaffold-and-engine.md Task 7。

## 6. 配置 Shiro 放行路径
见 Task 8。
```

- [ ] **Step 2：更新 .gitignore（如果需要本地放 jeecg-boot 源码做联调）**

```bash
cat >> /Users/wuhoujin/Documents/dev/bpm/.gitignore <<'EOF'

# jeecg-boot 源码副本（仅本地用于联调，不入仓库）
/jeecg-boot/
EOF
```

- [ ] **Step 3：在 /Users/wuhoujin/Documents/dev/bpm/ 下初始化 jeecg-module-bpm 目录骨架**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
mkdir -p jeecg-module-bpm/jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api
mkdir -p jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/{config,controller,engine}
mkdir -p jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/{META-INF,bpm}
mkdir -p jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/{controller,engine}
mkdir -p jeecg-module-bpm/jeecg-module-bpm-biz/src/test/resources
```

- [ ] **Step 4：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add INTEGRATION.md .gitignore jeecg-module-bpm/
git commit -m "chore(bpm-p0): scaffold module directory & integration doc"
```

> **集成方在 jeecg-boot 工作副本中执行后续 Task。本仓库内只持有 `jeecg-module-bpm/` 子树。下文出现 `${JEECG_HOME}` 指 jeecg-boot 工作副本根目录；`${BPM_HOME}` 指本仓库的 `jeecg-module-bpm/`。**

---

## Task 1：jeecg-boot 版本对齐与本地构建冒烟

**Files:**
- 读取：`${JEECG_HOME}/pom.xml`（确认 jeecgboot.version、parent pom 坐标）
- 读取：`${JEECG_HOME}/jeecg-boot-module-system/pom.xml`（确认启动模块名）
- 读取：`${JEECG_HOME}/db/`（确认 SQL 脚本，用于查 `act_*` 是否已带）
- 创建：`${BPM_HOME}/COMPATIBILITY.md`（记录确认结果）

- [ ] **Step 1：定位 jeecgboot-vue3 v0.1.8 对应的服务端 tag**

```bash
cd ${JEECG_HOME}
git tag --sort=-v:refname | head -20
# 期望看到 v3.6.3 / v3.6.2 / v3.6.1 等。jeecgboot-vue3 README 通常会写
# "服务端依赖 jeecg-boot v3.6.x"。如无法确定，按以下方式回退：
git log --oneline --all | grep -i "v0.1.8\|3.6"
```

记录确认到的 tag 到 `${BPM_HOME}/COMPATIBILITY.md`：
```markdown
# 兼容性核实结果

| 项 | 值 | 来源 |
|---|---|---|
| jeecgboot-vue3 版本 | v0.1.8 | manage.iimt.org.cn 浏览器侧观察 |
| jeecg-boot 服务端 tag | v3.6.3（待管理员确认）| 对应关系 |
| Spring Boot | 2.7.10 | jeecg-boot/pom.xml 顶部 properties |
| 启动模块 | jeecg-boot-module-system / jeecg-system-start（任选其一）| ${JEECG_HOME}/<module>/pom.xml |
| 现存 act_* 表 | 是/否 | grep -r 'act_re_procdef' ${JEECG_HOME}/db/ |
| 现存 Activiti 集成 | 是/否 | grep -ri 'activiti' ${JEECG_HOME}/jeecg-boot-module-system/src/main/resources/ |
```

- [ ] **Step 2：本地构建冒烟（不带 BPM 模块）**

```bash
cd ${JEECG_HOME}
mvn clean install -DskipTests -pl jeecg-boot-module-system -am
```

期望：BUILD SUCCESS。如失败，定位是 jeecg 自身依赖问题（与 BPM 无关），优先按 jeecg-boot 官方文档处理；本任务暂停。

- [ ] **Step 3：启动主壳，浏览器访问 swagger 确认基线工作**

```bash
cd ${JEECG_HOME}/jeecg-boot-module-system
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 在新终端：
curl -sS http://localhost:8080/jeecg-boot/sys/getCheckCode | head
```

期望：返回 JSON（不必关注内容），HTTP 200。证明基线服务能起来。

- [ ] **Step 4：把 COMPATIBILITY.md 提交回 BPM 仓库**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/COMPATIBILITY.md
git commit -m "docs(bpm-p0): record jeecg-boot version & build smoke result"
```

---

## Task 2：父 pom（jeecg-module-bpm/pom.xml）

**Files:**
- 创建：`${BPM_HOME}/pom.xml`

- [ ] **Step 1：写父 pom**

`${BPM_HOME}/pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.jeecgframework.boot</groupId>
        <artifactId>jeecg-boot-parent</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>jeecg-module-bpm</artifactId>
    <packaging>pom</packaging>
    <name>${project.artifactId}</name>
    <description>BPM 业务流程配置模块（jeecg 子模块）</description>

    <modules>
        <module>jeecg-module-bpm-api</module>
        <module>jeecg-module-bpm-biz</module>
    </modules>

    <properties>
        <flowable.version>6.8.0</flowable.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.flowable</groupId>
                <artifactId>flowable-spring-boot-starter-process</artifactId>
                <version>${flowable.version}</version>
            </dependency>
            <dependency>
                <groupId>org.jeecgframework.boot</groupId>
                <artifactId>jeecg-module-bpm-api</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

> **Why parent=jeecg-boot-parent：** jeecg 用 `${revision}` 占位符做版本统一，子模块继承父 pom 即可拿到 jeecg 的所有 dependencyManagement（Spring Boot、MyBatis-Plus、Shiro 等），避免版本漂移。`jeecg-boot-parent` 的 artifactId 在不同 jeecg 发行版可能写作 `jeecg-boot-base-core` 的 parent；以 Task 1 中 grep `<parent>` 的实际坐标为准。

- [ ] **Step 2：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/pom.xml
git commit -m "build(bpm-p0): add bpm parent pom with flowable dependency mgmt"
```

---

## Task 3：jeecg-module-bpm-api 子模块（占位）

**Files:**
- 创建：`${BPM_HOME}/jeecg-module-bpm-api/pom.xml`
- 创建：`${BPM_HOME}/jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/package-info.java`

api 模块在 P0 是空骨架，给 P2+ 放 DTO/Feign。现在建立它是为了固化 Maven 坐标。

- [ ] **Step 1：写 api 子模块 pom**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.jeecgframework.boot</groupId>
        <artifactId>jeecg-module-bpm</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>jeecg-module-bpm-api</artifactId>
    <name>${project.artifactId}</name>

    <dependencies>
        <dependency>
            <groupId>org.jeecgframework.boot</groupId>
            <artifactId>jeecg-system-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2：写 package-info 占位**

`${BPM_HOME}/jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/package-info.java`：
```java
/**
 * BPM 模块跨工程契约（DTO / Feign 接口）。P0 阶段为空。
 */
package org.jeecg.modules.bpm.api;
```

- [ ] **Step 3：构建验证**

```bash
cd ${JEECG_HOME}
mvn -pl jeecg-module-bpm/jeecg-module-bpm-api -am clean install -DskipTests
```

期望：BUILD SUCCESS。

- [ ] **Step 4：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/jeecg-module-bpm-api/
git commit -m "build(bpm-p0): scaffold jeecg-module-bpm-api submodule"
```

---

## Task 4：jeecg-module-bpm-biz 子模块 pom

**Files:**
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/pom.xml`

- [ ] **Step 1：写 biz 子模块 pom**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.jeecgframework.boot</groupId>
        <artifactId>jeecg-module-bpm</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>jeecg-module-bpm-biz</artifactId>
    <name>${project.artifactId}</name>

    <dependencies>
        <!-- 模块内部 -->
        <dependency>
            <groupId>org.jeecgframework.boot</groupId>
            <artifactId>jeecg-module-bpm-api</artifactId>
        </dependency>

        <!-- jeecg 系统模块（拿 Shiro/JWT/User 上下文）-->
        <dependency>
            <groupId>org.jeecgframework.boot</groupId>
            <artifactId>jeecg-system-local-api</artifactId>
        </dependency>

        <!-- Flowable 流程引擎 -->
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter-process</artifactId>
            <exclusions>
                <!-- jeecg 已用 mybatis-plus，排除 flowable 自带 mybatis 防冲突 -->
                <exclusion>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.19.3</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2：构建验证**

```bash
cd ${JEECG_HOME}
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz -am clean install -DskipTests
```

期望：BUILD SUCCESS。Flowable 6.8.0 应能解析下来。

- [ ] **Step 3：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml
git commit -m "build(bpm-p0): bpm-biz pom with flowable + testcontainers"
```

---

## Task 5：自动配置入口（spring.factories + AutoConfiguration）

**Files:**
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java`
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/main/resources/META-INF/spring.factories`

让 biz 被引入主启动模块时**零侵入自动加载**——不需要改主壳的 `@ComponentScan`。

- [ ] **Step 1：先写测试**

`${BPM_HOME}/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/BpmModuleContextTest.java`：
```java
package org.jeecg.modules.bpm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BpmModuleContextTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(BpmModuleAutoConfiguration.class));

    @Test
    void shouldLoadBpmModuleAutoConfiguration() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(BpmModuleAutoConfiguration.class));
    }
}
```

- [ ] **Step 2：跑测试看它失败**

```bash
cd ${JEECG_HOME}
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz test -Dtest=BpmModuleContextTest
```

期望：编译失败，找不到 `BpmModuleAutoConfiguration`。

- [ ] **Step 3：写最小实现**

`${BPM_HOME}/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java`：
```java
package org.jeecg.modules.bpm;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.jeecg.modules.bpm")
public class BpmModuleAutoConfiguration {
}
```

- [ ] **Step 4：注册到 spring.factories**

`${BPM_HOME}/jeecg-module-bpm-biz/src/main/resources/META-INF/spring.factories`：
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.jeecg.modules.bpm.BpmModuleAutoConfiguration
```

- [ ] **Step 5：跑测试验证通过**

```bash
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz test -Dtest=BpmModuleContextTest
```

期望：BUILD SUCCESS，1 test passed。

- [ ] **Step 6：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/META-INF/spring.factories \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/BpmModuleContextTest.java
git commit -m "feat(bpm-p0): add module auto-configuration entry point"
```

---

## Task 6：把 BPM 接入 jeecg 主启动模块

**Files:**
- 修改：`${JEECG_HOME}/pom.xml`（根 pom）
- 修改：`${JEECG_HOME}/jeecg-boot-module-system/pom.xml`（启动模块）

> **注意：** 启动模块的实际 artifactId 由 Task 1 确认。下文以 `jeecg-boot-module-system` 为占位。

- [ ] **Step 1：根 pom 注册 module**

打开 `${JEECG_HOME}/pom.xml`，在 `<modules>` 段新增一行（**保持原有顺序，仅追加**）：
```xml
<modules>
    ... 已有 ...
    <module>jeecg-module-bpm</module>
</modules>
```

- [ ] **Step 2：启动模块 pom 加依赖**

打开 `${JEECG_HOME}/jeecg-boot-module-system/pom.xml`，在 `<dependencies>` 中新增：
```xml
<dependency>
    <groupId>org.jeecgframework.boot</groupId>
    <artifactId>jeecg-module-bpm-biz</artifactId>
    <version>${revision}</version>
</dependency>
```

- [ ] **Step 3：从根目录全量构建**

```bash
cd ${JEECG_HOME}
mvn clean install -DskipTests
```

期望：BUILD SUCCESS。看到 `jeecg-module-bpm`、`jeecg-module-bpm-api`、`jeecg-module-bpm-biz` 都被编译。

- [ ] **Step 4：启动主壳，确认未损坏**

```bash
cd ${JEECG_HOME}/jeecg-boot-module-system
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

观察启动日志：
- 不应有 NoSuchBeanDefinitionException、CircularDependency 之类异常
- 应当看到 `o.flowable.engine.impl.ProcessEngineImpl` 之类的 Flowable 启动日志（Task 7 之后才会真正成功；本步骤如果 Flowable 因 datasource 配置缺失而抛错则在 Task 7 处理）

如果启动**因 Flowable 配置而失败**——继续走 Task 7。如果**因其它原因失败**——回退本步骤的 pom 改动，定位问题。

- [ ] **Step 5：commit（在 jeecg-boot 工作副本中）**

> 这一步的 commit 在 jeecg-boot 工作副本中进行，不在本 BPM 仓库。集成方按其 jeecg-boot fork 的流程提交。

---

## Task 7：Flowable 配置（数据源 + 历史 + Shiro 排除）

**Files:**
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/main/resources/bpm-application.yml`
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/FlowableConfig.java`
- 修改：`${JEECG_HOME}/jeecg-boot-module-system/src/main/resources/application-dev.yml`

- [ ] **Step 1：写 Flowable 配置文档（YAML 模板）**

`${BPM_HOME}/jeecg-module-bpm-biz/src/main/resources/bpm-application.yml`：
```yaml
# 默认 BPM 模块配置；主壳 application.yml 通过 spring.config.import 引入。
flowable:
  database-schema-update: true        # 首次启动建 act_* 表；生产改 false / none
  history-level: full                 # 保留任务全量历史
  async-executor-activate: true       # 异步执行器，用于 timer/超时
  check-process-definitions: false    # 不自动加载 classpath 下的 bpmn 文件
  process:
    servlet:
      load-on-startup: -1
  # 与 jeecg 共用同一个 datasource，无需单独配置
```

- [ ] **Step 2：在 jeecg 主壳启动配置中引入**

`${JEECG_HOME}/jeecg-boot-module-system/src/main/resources/application-dev.yml`，最顶部已存在的 `spring:` 段加入 `config.import`：
```yaml
spring:
  config:
    import:
      - classpath:bpm-application.yml
  # ... 已有内容
```

如果文件已使用 `spring.profiles.include`，按 jeecg 现有风格用 `include: bpm` 形式（同时新建 `application-bpm.yml` 复制 bpm-application.yml 内容）。**先尝试 `config.import` 方案，如不兼容回退到 profile-include。**

- [ ] **Step 3：写 FlowableConfig（自定义 ID 生成器与 historyLevel 二次确认）**

先写测试：
`${BPM_HOME}/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/config/FlowableConfigTest.java`：
```java
package org.jeecg.modules.bpm.config;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.engine.ProcessEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FlowableConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(FlowableConfig.class);

    @Test
    void exposesUuidIdGenerator() {
        runner.run(ctx -> {
            IdGenerator gen = ctx.getBean(IdGenerator.class);
            String first = gen.getNextId();
            String second = gen.getNextId();
            assertThat(first).isNotEqualTo(second);
            assertThat(first).hasSize(36); // UUID
        });
    }
}
```

跑测试看其失败：
```bash
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz test -Dtest=FlowableConfigTest
```

期望：编译失败（FlowableConfig 不存在）。

- [ ] **Step 4：写实现**

`${BPM_HOME}/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/FlowableConfig.java`：
```java
package org.jeecg.modules.bpm.config;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowableConfig {

    /**
     * 默认基于 db sequence；分布式部署下用 UUID 防冲突。
     */
    @Bean
    public IdGenerator flowableIdGenerator() {
        return new StrongUuidGenerator();
    }
}
```

- [ ] **Step 5：跑测试验证**

```bash
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz test -Dtest=FlowableConfigTest
```

期望：1 test passed。

- [ ] **Step 6：启动主壳验证 act_* 表自动建出来**

```bash
cd ${JEECG_HOME}/jeecg-boot-module-system
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 在 MySQL 里检查：
mysql -ujeecg -p jeecg-boot -e "SHOW TABLES LIKE 'act_%';" | wc -l
```

期望：返回行数 ≥ 25（Flowable 6.8 表数）。

- [ ] **Step 7：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm-application.yml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/FlowableConfig.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/config/FlowableConfigTest.java
git commit -m "feat(bpm-p0): wire flowable engine, history-level=full, uuid id gen"
```

---

## Task 8：Shiro 路径注册——`/bpm/v1/**` 走 JWT

**Files:**
- 修改：`${JEECG_HOME}/jeecg-boot-base/jeecg-boot-base-tools/src/main/java/org/jeecg/config/shiro/ShiroConfig.java`（路径以 grep 实际为准）

> **预备工作：** grep 找到 jeecg 中所有 `filterChainDefinitionMap.put(`：
> ```bash
> grep -rn "filterChainDefinitionMap.put" ${JEECG_HOME}/ --include="*.java" | head -5
> ```
> 找到唯一的 `ShiroConfig.java` 路径，对照下文。

jeecg 的现有 `ShiroConfig` 用一个长 if-else 或 list 注册放行路径与 JWT 路径。我们**只需在受 JWT 保护的段加入 `/bpm/v1/**`** 即可，不需要新写 Filter。

- [ ] **Step 1：在 ShiroConfig 中找到注册段**

阅读 `${JEECG_HOME}/.../shiro/ShiroConfig.java`，定位 `filterChainDefinitionMap` 添加项。jeecg 大多形如：
```java
filterChainDefinitionMap.put("/sys/**", "jwt");
filterChainDefinitionMap.put("/test/**", "anon");
```

- [ ] **Step 2：新增一行**

```java
filterChainDefinitionMap.put("/bpm/v1/**", "jwt");
```

如果现网用了 `org.jeecg.config.shiro.filters.JwtFilter` 自定义名，按现有命名为准。

- [ ] **Step 3：构建 + 启动验证**

```bash
cd ${JEECG_HOME}
mvn install -DskipTests -pl jeecg-boot-module-system -am
cd jeecg-boot-module-system
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
# 不带 token 访问，应当 401（被 JWT filter 拦截）：
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/jeecg-boot/bpm/v1/healthz
```

期望：401（即使 healthz endpoint 还没有，filter 已经先拦——这正是我们要的）。

> **暂时**还没有 healthz 实现。Task 9 写它。

- [ ] **Step 4：在 jeecg-boot 工作副本中 commit**

> 同 Task 6，集成方按 jeecg-boot fork 提交流程操作。

---

## Task 9：健康检查 endpoint（TDD）

**Files:**
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/controller/BpmHealthController.java`
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/BpmHealthControllerTest.java`

返回 `{"status":"UP","engine":"flowable","version":"6.8.0"}`。这个 endpoint 是 P0 唯一对外的接口，用于运维确认引擎已经上线。

- [ ] **Step 1：写测试**

```java
package org.jeecg.modules.bpm.controller;

import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {
        org.jeecg.modules.bpm.BpmModuleAutoConfiguration.class
})
@AutoConfigureMockMvc
class BpmHealthControllerTest {

    @Autowired MockMvc mvc;

    @MockBean ProcessEngine processEngine;

    @Test
    void healthzReturnsUp() throws Exception {
        Mockito.when(processEngine.getName()).thenReturn("default");
        mvc.perform(get("/bpm/v1/healthz"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"))
           .andExpect(jsonPath("$.engine").value("flowable"))
           .andExpect(jsonPath("$.version").exists());
    }
}
```

- [ ] **Step 2：跑测试看其失败**

```bash
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz test -Dtest=BpmHealthControllerTest
```

期望：404 或编译失败。

- [ ] **Step 3：写实现**

```java
package org.jeecg.modules.bpm.controller;

import org.flowable.engine.ProcessEngine;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/bpm/v1")
public class BpmHealthController {

    private final ProcessEngine processEngine;

    public BpmHealthController(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    @GetMapping("/healthz")
    public Map<String, Object> healthz() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("engine", "flowable");
        body.put("version", ProcessEngine.VERSION);
        body.put("name", processEngine.getName());
        return body;
    }
}
```

- [ ] **Step 4：跑测试验证通过**

```bash
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz test -Dtest=BpmHealthControllerTest
```

期望：1 test passed。

- [ ] **Step 5：手动验证（启动主壳后）**

```bash
# 没 token：
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/jeecg-boot/bpm/v1/healthz
# 期望：401

# 拿一个 dev token（用 jeecg /sys/login admin/123456 登录拿 X-Access-Token），然后：
TOKEN=<paste-token>
curl -sS -H "X-Access-Token: $TOKEN" http://localhost:8080/jeecg-boot/bpm/v1/healthz
```

期望：HTTP 200，body 含 `"status":"UP"`、`"engine":"flowable"`。

- [ ] **Step 6：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/controller/BpmHealthController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/BpmHealthControllerTest.java
git commit -m "feat(bpm-p0): add /bpm/v1/healthz returning engine version"
```

---

## Task 10：hello-world BPMN 资源

**Files:**
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/main/resources/bpm/helloworld.bpmn20.xml`

最小流程：StartEvent → UserTask("Hello") → EndEvent。

- [ ] **Step 1：写 BPMN XML**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://jeecg.org/bpm/sample"
             id="defs_helloworld">

    <process id="bpm_helloworld" name="BPM Hello World" isExecutable="true">

        <startEvent id="start"/>

        <sequenceFlow id="flow1" sourceRef="start" targetRef="task_hello"/>

        <userTask id="task_hello" name="Say Hello" flowable:assignee="${initiator}"/>

        <sequenceFlow id="flow2" sourceRef="task_hello" targetRef="end"/>

        <endEvent id="end"/>
    </process>
</definitions>
```

- [ ] **Step 2：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm/helloworld.bpmn20.xml
git commit -m "test(bpm-p0): add hello-world bpmn resource"
```

---

## Task 11：集成测试——部署 → 启动 → 完成（Testcontainers + MySQL）

**Files:**
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/test/resources/application-test.yml`
- 创建：`${BPM_HOME}/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/engine/HelloWorldFlowIT.java`

> **为什么 Testcontainers MySQL 而不是 H2：** Flowable 6.8 不再官方支持 H2（连接 string 报错或表创建脚本不全）。spec §10 已记录此约束。

- [ ] **Step 1：写测试配置**

`${BPM_HOME}/jeecg-module-bpm-biz/src/test/resources/application-test.yml`：
```yaml
flowable:
  database-schema-update: true
  history-level: full
  async-executor-activate: false   # 测试不需要异步执行器
spring:
  jpa:
    open-in-view: false
  quartz:
    enabled: false
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
```

- [ ] **Step 2：写集成测试**

```java
package org.jeecg.modules.bpm.engine;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        org.jeecg.modules.bpm.BpmModuleAutoConfiguration.class
})
@Testcontainers
class HelloWorldFlowIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.27")
            .withDatabaseName("bpm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired RepositoryService repositoryService;
    @Autowired RuntimeService runtimeService;
    @Autowired TaskService taskService;

    @Test
    void deployStartCompleteHelloWorld() {
        // deploy
        repositoryService.createDeployment()
                .addClasspathResource("bpm/helloworld.bpmn20.xml")
                .deploy();

        // start
        Map<String,Object> vars = new HashMap<>();
        vars.put("initiator", "alice");
        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_helloworld", vars);
        assertThat(inst).isNotNull();
        assertThat(inst.isEnded()).isFalse();

        // there should be one user task
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(1);
        Task t = tasks.get(0);
        assertThat(t.getName()).isEqualTo("Say Hello");
        assertThat(t.getAssignee()).isEqualTo("alice");

        // complete
        taskService.complete(t.getId());

        // should be ended
        ProcessInstance ended = runtimeService.createProcessInstanceQuery()
                .processInstanceId(inst.getId()).singleResult();
        assertThat(ended).isNull(); // moved to history
    }
}
```

- [ ] **Step 3：跑测试，看其失败（因 Docker / 配置问题）**

```bash
mvn -pl jeecg-module-bpm/jeecg-module-bpm-biz test \
    -Dtest=HelloWorldFlowIT \
    -Dspring.profiles.active=test
```

第一次跑可能失败原因：
- Docker daemon 未启动 → 启动 Docker Desktop / colima
- Maven 没找到 Testcontainers 的 mysql-connector → Task 4 已加 test scope，重跑 `mvn install` 一次
- application-test.yml 没生效 → 加 `@ActiveProfiles("test")` 注解到测试类（如必要）

- [ ] **Step 4：直到测试通过**

期望：1 test passed in approximately 30s（首次拉 mysql:8.0.27 镜像可能更慢）。

- [ ] **Step 5：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/test/resources/application-test.yml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/engine/HelloWorldFlowIT.java
git commit -m "test(bpm-p0): integration test deploys and runs hello-world flow"
```

---

## Task 12：手工冒烟（在真实 jeecg 主壳里跑 hello-world）

**Files:**
- 不修改任何文件——纯验证步骤

把所有前面任务串起来跑一次端到端，确保 P0 真的就绪。

- [ ] **Step 1：启动 jeecg 主壳**

```bash
cd ${JEECG_HOME}/jeecg-boot-module-system
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动日志应当看到：
- `flowable.engine.impl.ProcessEngineImpl - ProcessEngine default created`
- 没有任何 ERROR 级别 stack

- [ ] **Step 2：登录拿 token**

```bash
curl -sS -X POST 'http://localhost:8080/jeecg-boot/sys/login' \
  -H 'Content-Type: application/json' \
  --data '{"username":"admin","password":"123456","captcha":"x","checkKey":"y"}'
```

从返回中取 `result.token`，记为 `$TOKEN`。

- [ ] **Step 3：访问 healthz**

```bash
curl -sS -H "X-Access-Token: $TOKEN" http://localhost:8080/jeecg-boot/bpm/v1/healthz
```

期望返回：
```json
{"status":"UP","engine":"flowable","version":"6.8.0","name":"default"}
```

- [ ] **Step 4：通过 Flowable Java API 部署 hello-world，再用 RepositoryService 查询**

由于 P0 没做"部署 BPMN"的 HTTP 接口（那是 P1），这一步用一个 Spring Boot CommandLineRunner 临时验证更快——但**不要**把它合并进主分支。

替代方案：**直接验证 act_re_procdef 表里 startup 时是否被 Flowable 自动加载了 classpath:bpm/*.bpmn20.xml**。配置 `flowable.process.definition-locations=classpath:/bpm/*.bpmn20.xml` 或直接走 ServiceTask 让 Flowable 在 startup 自动扫 `processes/` 目录。

**最小化做法：在 BpmHealthController 临时加一个 `POST /bpm/v1/_dev/deploy-hello` 端点**，它调用 `repositoryService.createDeployment().addClasspathResource("bpm/helloworld.bpmn20.xml").deploy()`，然后**在 P1 第一个任务里删除这个 dev endpoint**。本计划暂不写它的代码——P0 验证由 `HelloWorldFlowIT` 集成测试覆盖即足够。

**结论：** Step 4 改为 "确认 act_re_procdef 表存在且为空"：
```bash
mysql -ujeecg -p jeecg-boot -e "SELECT count(*) FROM act_re_procdef;"
```
期望：0（数字本身不重要，能查到表证明引擎和数据库都通）。

- [ ] **Step 5：写 P0 验收清单**

`${BPM_HOME}/P0_DONE.md`：
```markdown
# P0 验收清单 ✅

- [x] jeecg-module-bpm 多模块编译通过
- [x] BpmModuleAutoConfiguration 被主壳自动加载
- [x] Flowable 6.8.0 启动，act_* 表（25 张）自动建出
- [x] /bpm/v1/healthz 不带 token = 401，带 token = 200
- [x] HelloWorldFlowIT（Testcontainers MySQL）通过
- [x] 主壳启动日志无 ERROR

## 下一步
P1：流程定义 CRUD + BPMN 设计器（前端）
```

- [ ] **Step 6：final commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/P0_DONE.md
git commit -m "docs(bpm-p0): P0 acceptance checklist"
git push origin main
```

---

## Self-Review Notes（plan 完成度自检）

**Spec coverage：**
- §2 P0（脚手架 + 引擎）→ 由 Task 2~9 全覆盖 ✅
- §3 模块结构（api/biz 拆分）→ Task 2~4 ✅
- §3.3 选型（Flowable 6.8）→ Task 4、7 ✅
- §4.3 Flowable 表配置（schema-update、history-level=full、async-executor）→ Task 7 ✅
- §6 API（healthz 不在 §6 列表，但属于 §1.3 引擎自检语义）→ Task 9 ✅
- §10 测试（Testcontainers MySQL）→ Task 11 ✅
- §13 与 jeecg 耦合点 ShiroConfig/Quartz/RabbitMQ → Task 8 ShiroConfig；Quartz/RabbitMQ 在 P0 不动 ✅

**未覆盖（按 P0 范围正确排除）：**
- BPMN 设计器、流程定义 CRUD、节点人员策略、表单绑定、版本生命周期、监控页面 — 属于 P1~P5
- 前端 Vue 改动 — 属于 P1+
- `bpm_*` 业务表 — 属于 P1+

**Placeholder 检查：** 全文 grep "TBD|TODO|FIXME|待补充|待定|稍后" 无命中（Task 12 Step 4 中提到的"P1 第一个任务里删除这个 dev endpoint"是对**未来 plan** 的注记，本身不是占位符）。

**类型一致性：** `BpmModuleAutoConfiguration` 在 Task 5 定义，Task 9 测试中引用，名字与包路径一致 ✅；`FlowableConfig` 在 Task 7 定义、测试同名 ✅；`BpmHealthController` 路径 `/bpm/v1/healthz` 在 Task 8、9、12 一致引用 ✅。

**已知不确定项（执行时需即时回填）：**
1. jeecg-boot 版本对齐结果（Task 1 输出 → 本计划 `${JEECG_HOME}` 即定）
2. ShiroConfig 类全限定名（Task 8 grep 后即定）
3. 启动模块 artifactId（`jeecg-boot-module-system` vs `jeecg-system-start`，Task 1 确认）

这些不属于 plan 缺陷——是 jeecg 不同发行版差异，必须靠现场确认。Task 1 的 COMPATIBILITY.md 是正式记录处。
