package org.jeecg.modules.bpm.monitor.mapper;

import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceQuery;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceVO;
import org.jeecg.modules.bpm.monitor.dto.StatsByDefinitionRow;
import org.jeecg.modules.bpm.monitor.dto.StatsByNodeRow;
import org.jeecg.modules.bpm.monitor.dto.StatsByApplyDeptRow;
import org.jeecg.modules.bpm.monitor.dto.StatsByApplyDeptTrendRow;
import org.jeecg.modules.bpm.monitor.dto.StatsQuery;
import org.jeecg.modules.bpm.scheduler.service.OverdueTaskRow;

import java.util.List;

public interface MonitorMapper {

    List<MonitorInstanceVO> selectInstances(@Param("q") MonitorInstanceQuery q,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    long countInstances(@Param("q") MonitorInstanceQuery q);

    List<StatsByDefinitionRow> selectStatsByDefinition(@Param("q") StatsQuery q);

    List<StatsByNodeRow> selectStatsByNode(@Param("q") StatsQuery q);

    List<StatsByApplyDeptRow> selectStatsByApplyDept(@Param("q") StatsQuery q);

    List<StatsByApplyDeptTrendRow> selectStatsByApplyDeptOverTime(@Param("q") StatsQuery q);

    List<OverdueTaskRow> selectOverdueRunningTasks();
}
