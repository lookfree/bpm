# jeecg-module-bpm

基于 [jeecg-boot](https://github.com/jeecgboot/jeecg-boot) v3.5.5 的业务流程管理（BPM）扩展模块，以独立 Maven 子模块的形式无侵入地集成到现有 jeecg 项目中。

底层流程引擎使用 [Flowable](https://www.flowable.com/)，前端设计器使用 bpmn.js。

---

## 功能概览

| 功能 | 说明 |
|------|------|
| 图形化流程设计 | 基于 bpmn.js 的可视化 BPMN 设计器，支持拖拽建模 |
| 流程版本管理 | 草稿 → 测试 → 发布 → 归档 四状态生命周期，保留历史版本快照 |
| 节点人员配置 | 支持指定用户、角色、部门主管、上级部门、表单字段、脚本六种审批人策略 |
| 分支条件规则 | 使用 Aviator 表达式引擎配置分支条件，支持沙箱隔离与超时保护 |
| 表单绑定 | 支持为流程绑定申请表单、审批表单、归档表单（三种用途） |
| 流程发起 | 提交表单数据发起流程实例 |
| 待办 / 已办 | 个人待办任务列表与审批操作（同意 / 拒绝 / 转交） |
| 流程监控 | 运维人员可查看运行中的流程实例，支持人工干预 |
| 多实例节点 | 支持会签（AND）和或签（OR）模式 |

---

## 模块结构

```
jeecg-module-bpm/
├── jeecg-module-bpm-spi          # SPI 接口层（无框架依赖）
│   └── BpmUserContext            # 用户上下文接口
│   └── BpmFormService            # 表单服务接口
│   └── BpmNotificationSender     # 消息通知接口
│
├── jeecg-module-bpm-biz          # 核心业务实现
│   ├── definition/               # 流程定义管理（CRUD、发布、版本）
│   ├── domain/                   # 领域实体（NodeConfig、FormBinding、InstanceMeta 等）
│   ├── service/assignee/         # 审批人策略（6 种策略实现）
│   ├── service/form/             # 表单绑定与权限合并
│   ├── service/instance/         # 流程实例发起
│   ├── engine/                   # Flowable 监听器与事件注册
│   ├── expression/               # Aviator 表达式引擎（分支条件）
│   └── multi/                    # 多实例（会签/或签）XML 重写
│
├── jeecg-module-bpm-adapter-jeecg  # jeecg 适配器（实现 SPI 接口）
│   └── JeecgBpmUserContext         # 读取 jeecg JWT 中的登录用户
│
└── jeecg-module-bpm-api          # 对外公共 API（预留）
```

---

## 数据库表

模块新增 7 张业务表（前缀 `bpm_`），Flowable 引擎自动创建 25 张 `act_*` 表：

| 表名 | 说明 |
|------|------|
| `bpm_process_definition` | 流程定义主表 |
| `bpm_process_definition_history` | 流程版本历史快照 |
| `bpm_node_config` | 节点审批人策略配置 |
| `bpm_form_binding` | 流程与表单的绑定关系 |
| `bpm_instance_meta` | 流程实例元数据 |
| `bpm_task_history` | 任务审批历史记录 |
| `bpm_assignee_strategy_def` | 自定义审批人策略定义 |

---

## 流程生命周期

```
DRAFT（草稿）
  │  设计 BPMN、配置节点审批人
  ▼
TESTING（测试中）  ← 第一次点击"发布"
  │  沙箱测试、验证流程
  ▼
PUBLISHED（已发布）  ← 第二次点击"发布"（部署到 Flowable）
  │
  ▼
ARCHIVED（已归档）  ← 下线旧版本
```

---

## 审批人策略

在流程设计器中，为每个 UserTask 节点配置审批人，支持以下策略：

| 策略类型 | 说明 | payload 示例 |
|----------|------|--------------|
| `USER` | 指定固定用户 | `{"userIds":["zhangsan","lisi"]}` |
| `ROLE` | 按角色分配 | `{"roleCode":"manager"}` |
| `DEPT_LEADER` | 发起人的部门主管 | `{}` |
| `UPPER_DEPT` | 上级部门主管 | `{}` |
| `FORM_FIELD` | 从表单字段取审批人 | `{"field":"approver"}` |
| `SCRIPT` | Aviator 脚本动态计算 | `{"script":"user.dept == 'A'"}` |

---

## REST API 一览

所有接口均需 `X-Access-Token` JWT 鉴权。

### 流程定义
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/bpm/v1/definition` | 分页查询流程定义列表 |
| POST | `/bpm/v1/definition` | 新建草稿 |
| GET | `/bpm/v1/definition/{id}` | 获取定义详情（含 BPMN XML） |
| PUT | `/bpm/v1/definition/{id}` | 更新草稿 |
| DELETE | `/bpm/v1/definition/{id}` | 删除草稿 |
| POST | `/bpm/v1/definition/{id}/publish` | 发布（草稿→测试→发布） |
| GET | `/bpm/v1/definition/{id}/versions` | 历史版本列表 |

### 流程实例
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/bpm/v1/instance` | 发起流程实例 |

### 任务
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/bpm/v1/task/todo` | 当前用户待办列表 |
| GET | `/bpm/v1/task/done` | 当前用户已办列表 |
| POST | `/bpm/v1/task/{taskId}/approve` | 审批操作（同意/拒绝/转交） |

### 表单绑定
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/bpm/v1/form-binding` | 查询绑定列表 |
| POST | `/bpm/v1/form-binding` | 新建绑定 |
| DELETE | `/bpm/v1/form-binding/{id}` | 删除绑定 |

---

## 前端页面

前端代码位于 jeecgboot-vue3 项目的 `src/views/bpm/` 目录：

| 路径 | 页面 | 说明 |
|------|------|------|
| `/bpm/definition` | 流程定义列表 | 查询、新建、删除流程定义 |
| `/bpm/designer` | 流程设计器 | BPMN 设计、保存草稿、发布 |
| `/bpm/form-binding` | 表单绑定 | 为流程绑定申请/审批/归档表单 |
| `/bpm/todo` | 我的待办 | 查看并处理待审批任务 |
| `/bpm/done` | 我的已办 | 查看已审批任务历史 |
| `/bpm/monitor` | 流程监控 | 运维人员查看实例状态，支持人工干预 |
| `/bpm/stats` | 统计分析 | 流程数据统计面板 |

---

## 快速集成

详细步骤参见 [INTEGRATION.md](./INTEGRATION.md)，概要如下：

**1. 安装 BPM 模块到本地 Maven 仓库**
```bash
git clone https://github.com/lookfree/bpm.git
cd bpm/jeecg-module-bpm
mvn clean install -DskipTests
```

**2. 在 jeecg-system-start 的 pom.xml 中添加依赖**
```xml
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-biz</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>com.iimt.bpm</groupId>
    <artifactId>jeecg-module-bpm-adapter-jeecg</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

**3. 在 application-dev.yml 中引入 BPM 配置**
```yaml
spring:
  config:
    import:
      - classpath:bpm-application.yml
```

**4. 在 ShiroConfig.java 中添加路由过滤**
```java
filterChainDefinitionMap.put("/bpm/v1/**", "jwt");
```

**5. 启动并验证**
```bash
# 查看启动日志是否包含
# "ProcessEngine default created"

# 健康检查（无需登录）
curl http://localhost:8080/jeecg-boot/bpm/v1/healthz
```

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 流程引擎 | Flowable 6.x |
| 表达式引擎 | Aviator 5.x |
| ORM | MyBatis-Plus 3.x |
| 后端框架 | Spring Boot 2.7 / jeecg-boot 3.5.5 |
| 前端框架 | Vue 3 + Ant Design Vue 3.x |
| 流程图设计 | bpmn.js |

---

## 环境要求

- JDK 8 或 11
- Maven 3.6+
- MySQL 8.x（与 jeecg 共享 schema）
- Redis 5+
- jeecg-boot v3.5.5
