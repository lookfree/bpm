package org.jeecg.modules.bpm.definition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;

@Mapper
public interface BpmProcessDefinitionMapper extends BaseMapper<BpmProcessDefinition> {

    @Select("SELECT COALESCE(MAX(version), 0) FROM bpm_process_definition " +
            "WHERE def_key = #{defKey} AND tenant_id = #{tenantId} AND deleted = 0")
    Integer maxVersion(@Param("defKey") String defKey, @Param("tenantId") String tenantId);
}
