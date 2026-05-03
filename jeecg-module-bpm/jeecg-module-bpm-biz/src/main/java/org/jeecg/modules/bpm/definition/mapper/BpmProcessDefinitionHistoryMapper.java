package org.jeecg.modules.bpm.definition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;

@Mapper
public interface BpmProcessDefinitionHistoryMapper extends BaseMapper<BpmProcessDefinitionHistory> {

    @Select("SELECT MAX(version) FROM bpm_process_definition_history WHERE def_id = #{defId}")
    Integer selectMaxVersion(@Param("defId") String defId);

    @Select("SELECT * FROM bpm_process_definition_history WHERE def_id = #{defId} AND version = #{version}")
    BpmProcessDefinitionHistory selectByDefIdAndVersion(@Param("defId") String defId, @Param("version") Integer version);
}
