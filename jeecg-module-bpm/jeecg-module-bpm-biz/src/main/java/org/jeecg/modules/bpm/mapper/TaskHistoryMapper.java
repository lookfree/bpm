package org.jeecg.modules.bpm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.bpm.domain.entity.TaskHistory;

import java.util.List;

@Mapper
public interface TaskHistoryMapper extends BaseMapper<TaskHistory> {

    @Select("SELECT COUNT(1) > 0 FROM bpm_task_history WHERE act_task_id = #{taskId} AND action = #{action}")
    boolean existsActionForTask(@Param("taskId") String taskId, @Param("action") String action);

    @Select("SELECT * FROM bpm_task_history WHERE inst_id = #{instId} ORDER BY op_time ASC")
    List<TaskHistory> selectByInstId(@Param("instId") String instId);
}
