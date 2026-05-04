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
