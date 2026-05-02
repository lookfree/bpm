package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;

import java.util.List;

public interface BpmProcessDefinitionHistoryService extends IService<BpmProcessDefinitionHistory> {

    String snapshot(String defId, String defKey, int version, String bpmnXml,
                    String changeNote, String publishedBy);

    List<BpmProcessDefinitionHistory> listByDefId(String defId);
}
