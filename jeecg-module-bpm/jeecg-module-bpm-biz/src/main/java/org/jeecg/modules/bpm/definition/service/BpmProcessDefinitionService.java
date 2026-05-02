package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;

public interface BpmProcessDefinitionService extends IService<BpmProcessDefinition> {

    IPage<DefinitionVO> queryPage(DefinitionQueryRequest req);

    DefinitionVO createDraft(DefinitionCreateRequest req);

    DefinitionVO getDetail(String id);

    DefinitionVO update(String id, DefinitionUpdateRequest req);

    void delete(String id);
}
