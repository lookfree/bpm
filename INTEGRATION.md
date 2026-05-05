# BPM 模块集成方法

本仓库交付 jeecg-boot 的一个独立 Maven 子模块 `jeecg-module-bpm`，
不包含 jeecg-boot 主体源码。集成方需自行准备 **jeecg-boot v3.5.5** 源码，
按以下步骤接入。本文片段在 P0 阶段已通过本地实际启动 jeecg-boot v3.5.5 验证。

## 前置条件

- JDK 8 或 11
- Maven 3.6+
- MySQL 8.x（jeecg 与 BPM 共用同一 schema）
- Redis 5+（jeecg-boot 自身依赖；BPM 暂不引入额外 Redis 用法）

> 版本核实方法：根据 manage.iimt.org.cn 部署使用的 Spring Boot 2.7.10 +
> MyBatis-Plus 3.5.3.1 + Shiro 1.12.0 + JWT 3.11.0 + Knife4j 3.0.3 +
> FastJSON 1.2.83 + Aliyun OSS 3.11.2 等版本组合，比对 jeecg-boot 各 tag 的
> 顶层 `pom.xml`，**v3.5.5** 唯一全匹配。

## 1. 准备 jeecg-boot

```bash
git clone --depth 1 -b v3.5.5 https://github.com/jeecgboot/jeecg-boot.git
cd jeecg-boot
```

## 2. 把 BPM 模块装入本地 Maven 仓库

在 BPM 仓库内执行：

```bash
cd /path/to/bpm/jeecg-module-bpm
mvn clean install -DskipTests
```

这会将三件 artifact 安装到 `~/.m2/repository/com/iimt/bpm/`：
- `jeecg-module-bpm` (parent pom)
- `jeecg-module-bpm-api`
- `jeecg-module-bpm-biz`

> BPM 工程独立：parent = `spring-boot-starter-parent 2.7.10`，**不**继承 `jeecg-boot-parent`，依赖里**不**含任何 `org.jeecgframework.boot:*` artifact。

## 3. 在 jeecg-boot 启动模块加 bpm-biz 依赖

v3.5.5 启动模块路径为 `jeecg-module-system/jeecg-system-start`。
在 `jeecg-module-system/jeecg-system-start/pom.xml` 的 `<dependencies>` 中追加：

```xml
<!-- BPM 模块（独立 Maven 工程，本地 m2 安装） -->
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-biz</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<!-- Redisson: BPM 分布式锁所需，jeecg-boot 不自带，需显式声明 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson</artifactId>
    <version>3.23.5</version>
</dependency>
```

> Redisson 在 bpm-biz 中是 `optional` 依赖（避免强制引入），因此部署模块需在此显式声明。
> 仅引入 `redisson`（非 `redisson-spring-boot-starter`）以避免 Redisson 接管 Spring 的 Redis 自动配置，与 jeecg-boot 自身的 Lettuce/Jedis 共存。

## 4. 引入 BPM 配置文件

在 `jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml`
（以及其他 profile yml）顶部 `spring:` 块下加入：

```yaml
spring:
  config:
    import:
      - classpath:bpm-application.yml
```

`bpm-application.yml` 由 `jeecg-module-bpm-biz` jar 提供（classpath 根目录），
内含 Flowable 默认配置：
- `flowable.database-schema-update: true`（首次启动自动建 act_* 表，生产改 false）
- `flowable.history-level: full`（任务全量历史）
- `flowable.async-executor-activate: true`（用于 timer 定时事件）
- `flowable.check-process-definitions: false`（不自动加载 classpath 下的 bpmn 文件）

## 5. 配置 Shiro 放行

打开 `jeecg-boot-base-core/src/main/java/org/jeecg/config/shiro/ShiroConfig.java`，
在最末尾的 `filterChainDefinitionMap.put("/**", "jwt");` 之前追加：

```java
// BPM 模块路径走 jwt 校验
filterChainDefinitionMap.put("/bpm/v1/**", "jwt");
```

> 实际上 jeecg 的 `/**` catch-all 已把 `/bpm/v1/**` 纳入 jwt 过滤；显式声明只为可读性。

## 6. 数据源

复用 jeecg 自身的 master 数据源即可，BPM 不引入第二个 DataSource。
确保 jeecg 配置的 MySQL 8.x 已启动。

Flowable 在首次启动时自动建 38 张 `ACT_*` 表（在同一 schema 内，与 jeecg 业务表共存）。

## 7. 构建 + 启动 + 验证

> **Java 版本**：jeecg-boot v3.5.5 依赖 Lombok，需 JDK 8 或 11 构建。JDK 17+ 因 Lombok 注解处理变更会导致构建失败。

```bash
# 若系统默认 JDK >= 17，需指定 JDK 11：
export JAVA_HOME=/path/to/jdk11

cd jeecg-boot
mvn -pl jeecg-module-system/jeecg-system-start -am clean install -DskipTests
cd jeecg-module-system/jeecg-system-start
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动日志期望出现：
- `o.flowable.engine.impl.ProcessEngineImpl - ProcessEngine default created`
- `s.b.w.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 8080`
- 启动时间约 9–12 秒（含 Flowable 引擎初始化）

> **Mapper 重复注册 WARN**：若 jeecg-boot 根 `@MapperScan` 扫描了 BPM 包，启动时会出现
> `Skipping MapperFactoryBean ... Bean already defined with the same name` 警告，属正常现象，不影响功能。

健康检查：
```bash
# 不带 token：401（证明 BPM 路由已注册）
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/jeecg-boot/bpm/v1/healthz
# 期望：401

# 登录拿 token，再带 token 调用：
curl -sS -H "X-Access-Token: $TOKEN" http://localhost:8080/jeecg-boot/bpm/v1/healthz
# 期望：{"status":"UP","engine":"flowable","version":"6.8.0","name":"default"}
```

验证 ACT_* 表：
```bash
mysql -uroot -proot jeecg-boot -e "SHOW TABLES LIKE 'ACT_%';" | wc -l
# 期望 ≥ 38（Flowable 6.8.0 含 event-registry 引擎）
```

## 8. P5 监控运维模块补充配置

P5 监控运维模块新增了调度任务（超时检查 + 历史清理），默认配置：

```yaml
# bpm-application.yml 已包含以下默认值，无需额外配置
bpm:
  scheduler:
    timeout:
      cron: "0 */5 * * * ?"    # 每 5 分钟扫超时任务
    history-cleanup:
      cron: "0 0 3 * * ?"       # 每天凌晨 3 点清历史
      retentionDays: 90         # 保留 90 天
```

P5 新增 API 路径：
- `GET /bpm/v1/monitor/instances` — 流程实例监控列表（分页 + 过滤）
- `GET /bpm/v1/monitor/instances/{id}/diagram` — 流程图 XML + 活跃节点
- `GET /bpm/v1/monitor/stats` — 统计聚合（按定义/节点/部门/趋势）
- `POST /bpm/v1/monitor/instances/{id}/intervene` — 强制干预（完成/取消/改派）
