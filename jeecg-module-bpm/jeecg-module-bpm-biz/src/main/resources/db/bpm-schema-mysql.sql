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
