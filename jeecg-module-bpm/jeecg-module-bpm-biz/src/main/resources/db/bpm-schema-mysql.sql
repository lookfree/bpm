-- BPM P1：流程定义 + 历史版本

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

-- ============================================================
-- P2: 节点配置 + 人员策略 + 表单绑定 + 实例元数据 + 任务历史
-- ============================================================

CREATE TABLE IF NOT EXISTS `bpm_node_config` (
  `id`                 VARCHAR(32)   NOT NULL COMMENT '主键 UUID',
  `def_id`             VARCHAR(32)   NOT NULL COMMENT '关联 bpm_process_definition.id',
  `node_id`            VARCHAR(64)   NOT NULL COMMENT 'BPMN element id',
  `assignee_strategy`  TEXT          NULL     COMMENT '人员策略 JSON {type, payload}',
  `multi_mode`         VARCHAR(16)   NULL     COMMENT 'SEQUENCE/PARALLEL/ANY，P2 仅落库不消费',
  `form_perm`          TEXT          NULL     COMMENT '节点表单权限 JSON（spec §5.3）',
  `timeout_hours`      INT           NULL     COMMENT '节点超时小时数',
  `timeout_action`     VARCHAR(16)   NULL     COMMENT 'REMIND/AUTO_PASS/ESCALATE',
  `create_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_node_config_def_node` (`def_id`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 节点配置';

CREATE TABLE IF NOT EXISTS `bpm_assignee_strategy` (
  `id`           VARCHAR(32)   NOT NULL COMMENT '主键 UUID',
  `name`         VARCHAR(128)  NOT NULL COMMENT '策略名称',
  `type`         VARCHAR(32)   NOT NULL COMMENT 'USER/ROLE/DEPT_LEADER/UPPER_DEPT/FORM_FIELD/SCRIPT',
  `payload`      TEXT          NULL     COMMENT '策略参数 JSON',
  `create_by`    VARCHAR(64)   NULL,
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bpm_assignee_strategy_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 人员策略词典';

CREATE TABLE IF NOT EXISTS `bpm_form_binding` (
  `id`           VARCHAR(32)   NOT NULL COMMENT '主键 UUID',
  `def_id`       VARCHAR(32)   NOT NULL COMMENT '关联 bpm_process_definition.id',
  `form_id`      VARCHAR(64)   NOT NULL COMMENT '关联 onl_cgform_head.id',
  `purpose`      VARCHAR(16)   NOT NULL COMMENT 'APPLY/APPROVE/ARCHIVE',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_form_binding_def_form_purpose` (`def_id`, `form_id`, `purpose`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 流程-表单绑定';

CREATE TABLE IF NOT EXISTS `bpm_instance_meta` (
  `id`              VARCHAR(32)   NOT NULL COMMENT '主键 UUID',
  `act_inst_id`     VARCHAR(64)   NOT NULL COMMENT 'Flowable act_ru_execution.id',
  `def_id`          VARCHAR(32)   NOT NULL,
  `def_version`     INT           NOT NULL,
  `business_key`    VARCHAR(128)  NULL,
  `apply_user_id`   BIGINT        NULL,
  `apply_dept_id`   BIGINT        NULL,
  `state`           VARCHAR(16)   NOT NULL COMMENT 'RUNNING/COMPLETED/CANCELLED/SANDBOX',
  `start_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time`        DATETIME      NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_instance_meta_act_inst` (`act_inst_id`),
  KEY `idx_bpm_instance_meta_apply_user` (`apply_user_id`),
  KEY `idx_bpm_instance_meta_def` (`def_id`, `def_version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 流程实例元数据';

CREATE TABLE IF NOT EXISTS `bpm_task_history` (
  `id`            VARCHAR(32)   NOT NULL COMMENT '主键 UUID',
  `act_task_id`   VARCHAR(64)   NOT NULL COMMENT 'Flowable act_hi_taskinst.id',
  `inst_id`       VARCHAR(32)   NOT NULL COMMENT '关联 bpm_instance_meta.id',
  `node_id`       VARCHAR(64)   NOT NULL,
  `assignee_id`   BIGINT        NULL,
  `action`        VARCHAR(16)   NOT NULL COMMENT 'APPROVE/REJECT/TRANSFER/COUNTERSIGN（P2 只用前两种）',
  `comment`       VARCHAR(1024) NULL,
  `attachments`   TEXT          NULL,
  `op_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bpm_task_history_task_action` (`act_task_id`, `action`),
  KEY `idx_bpm_task_history_inst` (`inst_id`),
  KEY `idx_bpm_task_history_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 任务审批历史（幂等：act_task_id + action 唯一）';

-- ============================================================
-- P4: category index + sandbox run table
-- ============================================================

CREATE INDEX idx_def_category_state ON bpm_process_definition (category, state);

CREATE TABLE IF NOT EXISTS `bpm_sandbox_run` (
    `id`           BIGINT PRIMARY KEY AUTO_INCREMENT,
    `def_id_draft` VARCHAR(64) NOT NULL,
    `runner_id`    BIGINT NOT NULL,
    `result`       VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    `log`          MEDIUMTEXT NULL,
    `start_time`   DATETIME NOT NULL,
    `end_time`     DATETIME NULL,
    INDEX `idx_sandbox_def` (`def_id_draft`),
    INDEX `idx_sandbox_runner` (`runner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BPM 沙箱运行记录';
