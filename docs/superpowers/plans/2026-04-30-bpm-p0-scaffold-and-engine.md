# BPM P0 — 独立模块脚手架 + Flowable 引擎集成 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付一个**独立可发布**的 Maven 工程 `jeecg-module-bpm`，集成 Flowable 6.8.0 工作流引擎；模块本身不依赖 `jeecg-*` artifact，通过 SPI（P1 起补齐）解耦宿主系统；P0 在 jeecg-boot v3.5.5 真实环境中跑通"添加依赖 → 主壳启动 → `/bpm/v1/healthz` 通 → hello-world BPMN 部署+启动+完成"完整链路。

**Architecture：**
- 独立 Maven 工程，parent = `spring-boot-starter-parent 2.7.10`，**不**继承 jeecg-boot-parent
- P0 实际交付 `bpm-api` + `bpm-biz` 两个子模块；`bpm-spi` 与 `bpm-adapter-jeecg` 留给 P1（SPI 在 P0 没有真正消费者）
- bpm-biz 只依赖 Spring Boot starter + Flowable starter；与 jeecg 解耦
- jeecg 集成形态：`mvn install` 出 jar → 在 jeecg-boot v3.5.5 启动模块 pom 中加一条 dependency

**Tech Stack:** Spring Boot 2.7.10 / Flowable 6.8.0 / MyBatis-Plus 3.5.3.1（jeecg 用，bpm 不用）/ Apache Shiro 1.12.0（jeecg 用，bpm 不用）/ MySQL 8.0.27 / Testcontainers 1.19.x / JUnit 5。

**与 spec 对应章节：** `docs/superpowers/specs/2026-04-30-bpm-module-design.md` §1.2、§3.1（4 模块架构）、§3.3（SPI 清单 — P1 真正用）、§3.4（技术选型）、§4.3（Flowable 表配置）、§10（测试策略）。

**前置假设：**
1. 工作目录 `/Users/wuhoujin/Documents/dev/bpm`，git 仓库已推到 `https://github.com/lookfree/bpm`，main 分支
2. 本机 macOS arm64，已装 JDK 11（`/opt/homebrew/opt/openjdk@11`）、Maven 3.9.x、Docker、Git。`source ~/bin/bpm-env.sh` 设置 `JAVA_HOME` + `PATH`
3. 本仓库已 clone jeecg-boot v3.5.5 到 `./jeecg-boot/`（已 `.gitignore`），仅用于 Task 11 集成冒烟，**不参与日常 mvn build**

---

## File Structure（本计划新增/修改的全部文件）

**新增（在仓库 `/Users/wuhoujin/Documents/dev/bpm/jeecg-module-bpm/` 下）：**
```
jeecg-module-bpm/
├── pom.xml                                          # ★ 父 pom，parent=spring-boot-starter-parent 2.7.10
├── jeecg-module-bpm-api/
│   ├── pom.xml
│   └── src/main/java/org/jeecg/modules/bpm/api/
│       └── package-info.java                        # 占位（P0 暂无 DTO）
└── jeecg-module-bpm-biz/
    ├── pom.xml
    └── src/main/
        ├── java/org/jeecg/modules/bpm/
        │   ├── BpmModuleAutoConfiguration.java      # @Configuration
        │   ├── config/FlowableConfig.java           # IdGenerator(UUID) 等
        │   └── controller/BpmHealthController.java  # GET /bpm/v1/healthz
        └── resources/
            ├── META-INF/spring.factories            # EnableAutoConfiguration → BpmModuleAutoConfiguration
            ├── bpm-application.yml                  # Flowable 默认配置
            └── bpm/helloworld.bpmn20.xml
```

**测试新增：**
```
jeecg-module-bpm/jeecg-module-bpm-biz/src/test/
├── java/org/jeecg/modules/bpm/
│   ├── BpmModuleContextTest.java                    # @ApplicationContextRunner 冒烟
│   ├── config/FlowableConfigTest.java               # IdGenerator 测试
│   ├── controller/BpmHealthControllerTest.java      # MockMvc
│   └── engine/HelloWorldFlowIT.java                 # Testcontainers MySQL 完整链路
└── resources/application-test.yml
```

**仓库根目录修改：**
- `INTEGRATION.md`（已存在 — Task 0 已写）：补充"添加 jar 依赖"集成步骤的具体片段
- 无需修改 `.gitignore`、根目录其它文件

**jeecg-boot v3.5.5 工作副本（`./jeecg-boot/`，仅 Task 11 用）：**
- `jeecg-boot-module-system/pom.xml`：临时加 `bpm-biz` dep，验证完成后**不提交**（jeecg-boot 是本仓库 .gitignore 过的本地副本）
- `.../resources/application-dev.yml`：临时引入 bpm 配置 + 数据源
- `.../config/shiro/ShiroConfig.java`：临时给 `/bpm/v1/**` 配 jwt filter

---

## Task 0：（已完成）目录脚手架 + 集成文档

**状态：** ✅ 已在 commit `412a3e3`/`724802e` 完成。本 Task 不需要重新执行；**Task 13 会做一次小扩充**：在已存在的 `jeecg-module-bpm/` 下追加 `jeecg-module-bpm-spi/` 与 `jeecg-module-bpm-adapter-jeecg/` 两个空目录骨架，方便 P1 直接接入。

> 本 plan 的 Task 编号从 1 重新开始，跟上面 Task 0 不冲突。

---

## Task 1：环境核验

**Files:** 创建 `jeecg-module-bpm/COMPATIBILITY.md`

- [ ] **Step 1：核验工具链**

```bash
source ~/bin/bpm-env.sh
java -version 2>&1 | head -1
mvn -v 2>&1 | head -1
docker --version
ls jeecg-boot/pom.xml | head -1
```

期望：JDK 11.x、Maven 3.9.x、Docker、jeecg-boot v3.5.5 副本就绪。

- [ ] **Step 2：写 COMPATIBILITY.md**

`jeecg-module-bpm/COMPATIBILITY.md`：
```markdown
# 兼容性核实结果

| 项 | 值 | 说明 |
|---|---|---|
| 目标 jeecg-boot 版本 | v3.5.5 | 通过 root pom.xml 版本组合唯一识别 |
| BPM 模块 parent | spring-boot-starter-parent 2.7.10 | 不继承 jeecg-boot-parent |
| BPM 模块 Java 编译目标 | 1.8 | 跟 jeecg 保持一致，运行时支持 JDK 8+ |
| BPM 模块本机构建 JDK | 11 (Homebrew arm64) | macOS arm64 上 openjdk@8 brew core 没 arm64 包；JDK 11 编译 1.8 字节码无副作用 |
| Flowable 版本 | 6.8.0 | 与 Spring Boot 2.7.x 已验证兼容 |
| 测试用 MySQL | 8.0.27（Testcontainers） | 与生产环境一致 |
| 已知未确认项 | jeecg `act_*` 表是否已存在 | Task 11 中查 jeecg-boot/db/ 与启动后真实状态 |
```

- [ ] **Step 3：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/COMPATIBILITY.md
git commit -m "docs(bpm-p0): record toolchain & compatibility decisions"
```

---

## Task 2：父 pom（独立工程，不依赖 jeecg-boot-parent）

**Files:** 创建 `jeecg-module-bpm/pom.xml`

- [ ] **Step 1：写父 pom**

`jeecg-module-bpm/pom.xml`：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.10</version>
        <relativePath/>
    </parent>

    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>${project.artifactId}</name>
    <description>独立 BPM 模块（适配 jeecg-boot v3.5.5 等宿主系统）</description>

    <modules>
        <module>jeecg-module-bpm-api</module>
        <module>jeecg-module-bpm-biz</module>
    </modules>

    <properties>
        <java.version>1.8</java.version>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <flowable.version>6.8.0</flowable.version>
        <testcontainers.version>1.19.3</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.flowable</groupId>
                <artifactId>flowable-spring-boot-starter-process</artifactId>
                <version>${flowable.version}</version>
            </dependency>
            <dependency>
                <groupId>com.iimt.bpm</groupId>
                <artifactId>jeecg-module-bpm-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

> **Why `groupId=com.iimt.bpm`：** 跟 jeecg 的 `org.jeecgframework.boot` 区分，避免被误认为是 jeecg 官方模块；后续可改成项目方实际域名。
>
> **Why `version=0.1.0-SNAPSHOT`：** 模块独立版本号，跟 jeecg-boot 版本号脱钩，方便独立发布。

- [ ] **Step 2：构建验证**

```bash
source ~/bin/bpm-env.sh
cd jeecg-module-bpm
mvn -N validate
```

期望：BUILD SUCCESS。`-N` 跳过子模块（它们还没 pom.xml），只验证父 pom。

- [ ] **Step 3：commit**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add jeecg-module-bpm/pom.xml
git commit -m "build(bpm-p0): add standalone parent pom (spring-boot-starter-parent 2.7.10)"
```

---

## Task 3：jeecg-module-bpm-api 子模块

**Files:**
- 创建 `jeecg-module-bpm/jeecg-module-bpm-api/pom.xml`
- 创建 `jeecg-module-bpm/jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/package-info.java`

P0 阶段 api 子模块为空骨架，固化坐标。

- [ ] **Step 1：api 子模块 pom**

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

    <artifactId>jeecg-module-bpm-api</artifactId>
    <name>${project.artifactId}</name>
    <description>BPM 模块对外契约（DTO / 异常 / 公共类型），无 jeecg / Spring 强耦合</description>

    <!-- P0：空模块，无依赖 -->
</project>
```

- [ ] **Step 2：占位文件**

`jeecg-module-bpm-api/src/main/java/org/jeecg/modules/bpm/api/package-info.java`：
```java
/**
 * BPM 模块对外契约（DTO / 异常 / 公共类型）。P0 阶段为空骨架。
 * <p>
 * 此包不依赖 jeecg / Spring，可以安全地被任何 Java 模块引用。
 */
package org.jeecg.modules.bpm.api;
```

- [ ] **Step 3：构建验证**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
source ~/bin/bpm-env.sh
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-api -am clean install
```

期望：BUILD SUCCESS；本机 `~/.m2/repository/com/iimt/bpm/jeecg-module-bpm-api/0.1.0-SNAPSHOT/` 出现 jar。

- [ ] **Step 4：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-api/
git commit -m "build(bpm-p0): scaffold bpm-api submodule (empty placeholder)"
```

---

## Task 4：jeecg-module-bpm-biz 子模块 pom

**Files:** 创建 `jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml`

> **重要约束：** 这个 pom **不能**出现 `org.jeecgframework.boot:*` 任何依赖。bpm-biz 必须保持对 jeecg 零依赖，jeecg 的对接由后续 P1 的 adapter 负责。

- [ ] **Step 1：biz 子模块 pom**

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

    <artifactId>jeecg-module-bpm-biz</artifactId>
    <name>${project.artifactId}</name>
    <description>BPM 引擎实现 + Controller。不依赖 jeecg。</description>

    <dependencies>
        <!-- 模块内部 -->
        <dependency>
            <groupId>com.iimt.bpm</groupId>
            <artifactId>jeecg-module-bpm-api</artifactId>
        </dependency>

        <!-- Spring Boot Web（非可选 — Controller 需要）-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Flowable -->
        <dependency>
            <groupId>org.flowable</groupId>
            <artifactId>flowable-spring-boot-starter-process</artifactId>
            <exclusions>
                <!-- 防止 Flowable 自带 mybatis 与宿主 mybatis-plus 冲突 -->
                <exclusion>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis</artifactId>
                </exclusion>
            </exclusions>
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
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <!-- Spring Boot 2.7.10 BOM 用新坐标 com.mysql:mysql-connector-j（旧 mysql:mysql-connector-java 未管理） -->
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2：构建验证（依赖能解析）**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz -am dependency:resolve
```

期望：BUILD SUCCESS；Flowable + Testcontainers + Spring Boot Web 都被解析下来。

- [ ] **Step 3：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/pom.xml
git commit -m "build(bpm-p0): bpm-biz pom (flowable + testcontainers, NO jeecg deps)"
```

---

## Task 5：自动配置入口（spring.factories + AutoConfiguration）

**Files:**
- 创建 `jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java`
- 创建 `jeecg-module-bpm-biz/src/main/resources/META-INF/spring.factories`
- 创建测试 `BpmModuleContextTest.java`

让 biz 被引入宿主时**零侵入自动加载**——宿主无需改 `@ComponentScan`。

- [ ] **Step 1：写测试**

`jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/BpmModuleContextTest.java`：
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

- [ ] **Step 2：跑测试看其失败**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmModuleContextTest
```

期望：编译失败（找不到 `BpmModuleAutoConfiguration`）。

- [ ] **Step 3：写实现**

`jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java`：
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

`jeecg-module-bpm-biz/src/main/resources/META-INF/spring.factories`：
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
org.jeecg.modules.bpm.BpmModuleAutoConfiguration
```

- [ ] **Step 5：跑测试验证**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmModuleContextTest
```

期望：1 test passed。

- [ ] **Step 6：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/BpmModuleAutoConfiguration.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/META-INF/spring.factories \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/BpmModuleContextTest.java
git commit -m "feat(bpm-p0): module auto-configuration entry point"
```

---

## Task 6：Flowable 配置（IdGenerator + history 默认值）

**Files:**
- 创建 `jeecg-module-bpm-biz/src/main/resources/bpm-application.yml`
- 创建 `jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/FlowableConfig.java`
- 创建测试 `FlowableConfigTest.java`

- [ ] **Step 1：默认配置 yml（被宿主 application.yml 通过 spring.config.import 引入）**

`jeecg-module-bpm-biz/src/main/resources/bpm-application.yml`：
```yaml
flowable:
  database-schema-update: true        # 首次建 act_* 表；生产改 false
  history-level: full                 # 任务全量历史
  async-executor-activate: true       # 异步执行器（用于 timer）
  check-process-definitions: false    # 不自动加载 classpath 下的 bpmn
```

- [ ] **Step 2：写 FlowableConfig 测试**

`jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/config/FlowableConfigTest.java`：
```java
package org.jeecg.modules.bpm.config;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FlowableConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(FlowableConfig.class);

    @Test
    void exposesUuidIdGenerator() {
        runner.run(ctx -> {
            IdGenerator gen = ctx.getBean(IdGenerator.class);
            String first = gen.getNextId();
            String second = gen.getNextId();
            assertThat(first).isNotEqualTo(second);
            assertThat(first).hasSize(36);
        });
    }
}
```

- [ ] **Step 3：跑测试看其失败**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=FlowableConfigTest
```

期望：编译失败。

- [ ] **Step 4：写实现**

`jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/FlowableConfig.java`：
```java
package org.jeecg.modules.bpm.config;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowableConfig {

    @Bean
    public IdGenerator flowableIdGenerator() {
        return new StrongUuidGenerator();
    }
}
```

- [ ] **Step 5：跑测试验证**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=FlowableConfigTest
```

期望：1 test passed。

- [ ] **Step 6：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm-application.yml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/config/FlowableConfig.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/config/FlowableConfigTest.java
git commit -m "feat(bpm-p0): flowable config with uuid id generator + default yml"
```

---

## Task 7：健康检查 endpoint（TDD）

**Files:**
- 创建 `jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/controller/BpmHealthController.java`
- 创建测试 `BpmHealthControllerTest.java`

返回 `{"status":"UP","engine":"flowable","version":"6.8.0","name":"default"}`。

- [ ] **Step 1：写测试**

`jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/BpmHealthControllerTest.java`：

> **注：** 库模块没有 `@SpringBootApplication`，`@WebMvcTest` 找不到 `@SpringBootConfiguration`；用内部 `TestConfig` 显式开 MVC + 扫包是最小可工作模式。

```java
package org.jeecg.modules.bpm.controller;

import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = { BpmHealthControllerTest.TestConfig.class })
@AutoConfigureMockMvc
class BpmHealthControllerTest {

    @Configuration
    @EnableWebMvc
    @ComponentScan("org.jeecg.modules.bpm.controller")
    public static class TestConfig {
    }

    @Autowired MockMvc mvc;

    @MockBean ProcessEngine processEngine;

    @Test
    void healthzReturnsUp() throws Exception {
        Mockito.when(processEngine.getName()).thenReturn("default");
        mvc.perform(get("/bpm/v1/healthz"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("UP"))
           .andExpect(jsonPath("$.engine").value("flowable"))
           .andExpect(jsonPath("$.version").exists())
           .andExpect(jsonPath("$.name").value("default"));
    }
}
```

- [ ] **Step 2：跑测试看其失败**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmHealthControllerTest
```

期望：编译失败。

- [ ] **Step 3：写实现**

`jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/controller/BpmHealthController.java`：
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

- [ ] **Step 4：跑测试验证**

```bash
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=BpmHealthControllerTest
```

期望：1 test passed。

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/java/org/jeecg/modules/bpm/controller/BpmHealthController.java \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/controller/BpmHealthControllerTest.java
git commit -m "feat(bpm-p0): /bpm/v1/healthz returning engine version"
```

---

## Task 8：hello-world BPMN 资源

**Files:** 创建 `jeecg-module-bpm-biz/src/main/resources/bpm/helloworld.bpmn20.xml`

- [ ] **Step 1：写 BPMN**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             targetNamespace="http://iimt.com/bpm/sample"
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
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/main/resources/bpm/helloworld.bpmn20.xml
git commit -m "test(bpm-p0): hello-world bpmn resource"
```

---

## Task 9：集成测试——deploy → start → complete（Testcontainers MySQL）

**Files:**
- 创建 `jeecg-module-bpm-biz/src/test/resources/application-test.yml`
- 创建 `jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/engine/HelloWorldFlowIT.java`

> Flowable 6.8 不官方支持 H2 → 必须真 MySQL。Testcontainers 拉 mysql:8.0.27 镜像即可。

- [ ] **Step 1：测试配置**

`jeecg-module-bpm-biz/src/test/resources/application-test.yml`：
```yaml
flowable:
  database-schema-update: true
  history-level: full
  async-executor-activate: false
spring:
  jpa:
    open-in-view: false
```

- [ ] **Step 2：集成测试**

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
        repositoryService.createDeployment()
                .addClasspathResource("bpm/helloworld.bpmn20.xml")
                .deploy();

        Map<String,Object> vars = new HashMap<>();
        vars.put("initiator", "alice");
        ProcessInstance inst = runtimeService.startProcessInstanceByKey("bpm_helloworld", vars);
        assertThat(inst).isNotNull();
        assertThat(inst.isEnded()).isFalse();

        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(inst.getId()).list();
        assertThat(tasks).hasSize(1);
        Task t = tasks.get(0);
        assertThat(t.getName()).isEqualTo("Say Hello");
        assertThat(t.getAssignee()).isEqualTo("alice");

        taskService.complete(t.getId());

        ProcessInstance ended = runtimeService.createProcessInstanceQuery()
                .processInstanceId(inst.getId()).singleResult();
        assertThat(ended).isNull();
    }
}
```

- [ ] **Step 3：跑测试**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml -pl jeecg-module-bpm-biz test -Dtest=HelloWorldFlowIT
```

期望：1 test passed（首次运行可能下载 mysql:8.0.27 镜像，~30–60s）。

- [ ] **Step 4：常见失败处理**

| 失败模式 | 解决 |
|---|---|
| `Could not find a valid Docker environment` | 启动 Docker Desktop 或 colima |
| `Timeout starting container` | Docker 资源不足，调大 Docker 内存到 ≥4GB |
| `LiquibaseException: ... master.changelog` | Flowable 版本问题，确认 6.8.0 |
| `org.flowable... unsupported database type 'h2'` | 配置漏了，确认 spring.datasource.* 通过 DynamicPropertySource 注入 |

- [ ] **Step 5：commit**

```bash
git add jeecg-module-bpm/jeecg-module-bpm-biz/src/test/resources/application-test.yml \
        jeecg-module-bpm/jeecg-module-bpm-biz/src/test/java/org/jeecg/modules/bpm/engine/HelloWorldFlowIT.java
git commit -m "test(bpm-p0): integration test deploy/start/complete hello-world"
```

---

## Task 10：mvn install 出 jar，验证可用作依赖

**Files:** 无新增。验证 `bpm-biz` jar 在本地 m2 仓库就位。

- [ ] **Step 1：从根目录全量 install**

```bash
source ~/bin/bpm-env.sh
cd /Users/wuhoujin/Documents/dev/bpm
mvn -f jeecg-module-bpm/pom.xml clean install -DskipTests
```

期望：BUILD SUCCESS；3 个 artifact 都装进 m2：
```bash
ls ~/.m2/repository/com/iimt/bpm/jeecg-module-bpm{,-api,-biz}/0.1.0-SNAPSHOT/*.jar 2>/dev/null
ls ~/.m2/repository/com/iimt/bpm/jeecg-module-bpm/0.1.0-SNAPSHOT/*.pom
```

期望看到 3 个 jar / pom 文件。

- [ ] **Step 2：跑全量测试一次**

```bash
mvn -f jeecg-module-bpm/pom.xml test
```

期望：所有测试通过（4 个：BpmModuleContextTest、FlowableConfigTest、BpmHealthControllerTest、HelloWorldFlowIT）。

- [ ] **Step 3：commit（无文件改动则跳过此 step）**

无文件改动；本任务只做验证。

---

## Task 11：在真 jeecg-boot v3.5.5 中冒烟集成

**Files：** 仅修改 `./jeecg-boot/`（本地副本，不入仓库）；**不修改 BPM 仓库内任何文件**。

> **目标：** 证明 bpm-biz jar 能被 jeecg-boot v3.5.5 启动模块直接当依赖使用，启动后 `/bpm/v1/healthz`（带 jwt）能 200。

> **MySQL 准备：** 本机需要一个 MySQL 8.x 实例供 jeecg 用。可以用 Docker：
> ```bash
> docker run -d --name jeecg-mysql -e MYSQL_ROOT_PASSWORD=root \
>     -e MYSQL_DATABASE=jeecg-boot -p 3306:3306 mysql:8.0.27
> ```
> 等 30 秒就绪后导入 jeecg-boot/db/jeecgboot-mysql-5.7.sql：
> ```bash
> docker exec -i jeecg-mysql mysql -uroot -proot jeecg-boot < jeecg-boot/db/jeecgboot-mysql-5.7.sql
> ```

- [ ] **Step 1：定位 jeecg-boot 启动模块**

```bash
ls jeecg-boot/jeecg-boot-module-system/pom.xml jeecg-boot/jeecg-boot-starter*/pom.xml jeecg-boot/jeecg-system-start*/pom.xml 2>&1
```

期望找到唯一的"启动模块" pom（v3.5.5 一般是 `jeecg-boot-module-system`）。

- [ ] **Step 2：在启动模块 pom 中加 bpm-biz 依赖**

```xml
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-biz</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

- [ ] **Step 3：在 jeecg-boot 启动模块 application-dev.yml 顶部加入 bpm 配置**

```yaml
spring:
  config:
    import:
      - classpath:bpm-application.yml
```

或如果 jeecg 当前使用 `spring.profiles.include`，按其风格调整。

- [ ] **Step 4：在 jeecg-boot ShiroConfig 中放行 bpm 路径**

```bash
grep -rln 'filterChainDefinitionMap.put' jeecg-boot/ --include='*.java'
```

打开找到的 `ShiroConfig.java`，在已有 `filterChainDefinitionMap.put("/sys/**", "jwt");` 之类语句旁追加：
```java
filterChainDefinitionMap.put("/bpm/v1/**", "jwt");
```

- [ ] **Step 5：构建 + 启动 jeecg-boot**

```bash
source ~/bin/bpm-env.sh
cd jeecg-boot
mvn -pl jeecg-boot-module-system -am clean install -DskipTests
cd jeecg-boot-module-system
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

期望启动日志看到：
- `o.flowable.engine.impl.ProcessEngineImpl - ProcessEngine default created`
- `s.b.w.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 8080`
- 没有 ERROR 级别栈

- [ ] **Step 6：验证 healthz**

```bash
# 不带 token：401
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/jeecg-boot/bpm/v1/healthz
# 期望：401

# 登录拿 token：
TOKEN=$(curl -sS -X POST 'http://localhost:8080/jeecg-boot/sys/login' \
  -H 'Content-Type: application/json' \
  --data '{"username":"admin","password":"123456","captcha":"x","checkKey":"y"}' | jq -r '.result.token')
echo "TOKEN=$TOKEN"

# 带 token：
curl -sS -H "X-Access-Token: $TOKEN" http://localhost:8080/jeecg-boot/bpm/v1/healthz
```

期望最后一条返回：
```json
{"status":"UP","engine":"flowable","version":"6.8.0","name":"default"}
```

- [ ] **Step 7：验证 act_* 表已创建**

```bash
docker exec -it jeecg-mysql mysql -uroot -proot jeecg-boot -e "SHOW TABLES LIKE 'act_%';" | wc -l
```

期望 ≥ 26（25 张 act_* + 表头一行）。

- [ ] **Step 8：把 ShiroConfig 改动 + pom 改动写到 INTEGRATION.md 作为参考片段**

更新 `INTEGRATION.md`，把"## 4. 修改启动模块依赖"段下原 placeholder 替换为具体已验证的片段：

```markdown
## 4. 修改启动模块依赖
在 jeecg-boot v3.5.5 的 jeecg-boot-module-system/pom.xml `<dependencies>` 中追加：
```xml
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-biz</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 5. 配置 Flowable
在 application-dev.yml 顶部加：
```yaml
spring:
  config:
    import:
      - classpath:bpm-application.yml
```

## 6. 配置 Shiro 放行
在 ShiroConfig.java 找到 filterChainDefinitionMap 配置，加入：
```java
filterChainDefinitionMap.put("/bpm/v1/**", "jwt");
```

## 7. 准备数据源
确保 jeecg datasource 指向的 MySQL 8.x 已启动。Flowable 会在首次启动时自动建 act_* 表。
```

- [ ] **Step 9：commit INTEGRATION.md 更新**

```bash
cd /Users/wuhoujin/Documents/dev/bpm
git add INTEGRATION.md
git commit -m "docs(bpm-p0): integration steps verified against jeecg-boot v3.5.5"
```

---

## Task 12：P0 验收清单

**Files：** 创建 `jeecg-module-bpm/P0_DONE.md`

- [ ] **Step 1：写验收清单**

`jeecg-module-bpm/P0_DONE.md`：
```markdown
# P0 验收清单 ✅

## 独立性
- [x] BPM 父 pom parent = `spring-boot-starter-parent 2.7.10`，不继承 `jeecg-boot-parent`
- [x] `bpm-biz` pom 依赖中无任何 `org.jeecgframework.boot:*` artifact
- [x] BPM 工程独立 `mvn install` 可发布到本地 m2

## 引擎集成
- [x] Flowable 6.8.0 在测试中启动成功，act_* 表（25 张）自动创建
- [x] StrongUuidGenerator 通过单测
- [x] hello-world BPMN：deploy → start → complete 链路通过 Testcontainers MySQL 验证

## 健康检查
- [x] `BpmHealthController` 单测通过
- [x] 真 jeecg-boot v3.5.5 启动后，`/jeecg-boot/bpm/v1/healthz` 不带 token = 401，带 token = 200，返回引擎版本

## 文档
- [x] INTEGRATION.md 包含已验证的 4 步集成片段（pom/yml/Shiro/数据源）
- [x] COMPATIBILITY.md 记录工具链与版本决策

## 下一步（P1）
- 补 `bpm-spi` 接口（BpmUserContext / BpmOrgService / BpmFormService / BpmNotificationSender）
- 写 `bpm-adapter-jeecg` 实现，让 bpm-biz 通过 SPI 拿用户/部门数据
- 流程定义 CRUD + 前端 BPMN 设计器
```

- [ ] **Step 2：commit + push**

```bash
git add jeecg-module-bpm/P0_DONE.md
git commit -m "docs(bpm-p0): P0 acceptance checklist"
git push origin main
```

---

## Self-Review Notes

**spec 覆盖：**
- §1.2（独立模块原则） → Task 2、4 体现 ✅
- §3.1（4 模块架构） → Task 0 + 13 已 scaffold api/biz；spi/adapter 在 P1（已声明）✅
- §3.3（SPI 清单） → P0 暂不引入；明确属于 P1（Q.E.D.：Task 12 P0_DONE 列出）✅
- §3.4（技术选型 — Flowable 6.8） → Task 4、6、9 ✅
- §4.3（Flowable 表配置 — schema-update/history/async） → Task 6 ✅
- §10（Testcontainers MySQL） → Task 9 ✅

**未覆盖（按 P0 范围正确排除）：**
- BPMN 设计器、流程定义 CRUD、节点人员策略、表单绑定、版本生命周期、监控页面 — 属于 P1~P5
- 前端 Vue 改动 — P1+
- `bpm_*` 业务表 — P1+
- SPI 接口实现与 adapter — P1（首个使用者：AssigneeResolver）

**Placeholder 检查：** 无 TBD/TODO/FIXME/待补充/待定/稍后/未来补 字样。

**类型一致性：**
- `BpmModuleAutoConfiguration` Task 5 定义、Task 9 测试引用 ✅
- `FlowableConfig` Task 6 定义、测试同名 ✅
- `BpmHealthController` Task 7 路径 `/bpm/v1/healthz`，Task 11 真实环境验证同路径 ✅
- groupId 统一 `com.iimt.bpm`，version `0.1.0-SNAPSHOT` ✅
