package org.jeecg.modules.bpm.scheduler.cleanup;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HistoryCleanupMapper {

    @Delete("DELETE FROM bpm_task_history WHERE inst_id IN (" +
            "  SELECT id FROM bpm_instance_meta" +
            "  WHERE state IN ('COMPLETED','CANCELLED','REJECTED')" +
            "  AND end_time < #{cutoff}" +
            ")")
    int deleteTaskHistory(@Param("cutoff") java.time.LocalDateTime cutoff);

    @Delete("DELETE FROM bpm_instance_meta" +
            " WHERE state IN ('COMPLETED','CANCELLED','REJECTED')" +
            " AND end_time < #{cutoff}")
    int deleteInstanceMeta(@Param("cutoff") java.time.LocalDateTime cutoff);
}
