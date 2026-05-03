package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefinitionRollbackService {

    private final BpmProcessDefinitionMapper defMapper;
    private final BpmProcessDefinitionHistoryService historyService;

    public DefinitionRollbackService(BpmProcessDefinitionMapper defMapper,
                                     BpmProcessDefinitionHistoryService historyService) {
        this.defMapper = defMapper;
        this.historyService = historyService;
    }

    /**
     * Rollback bypasses transition matrix by design — it always resets to DRAFT regardless of current state.
     * Only ARCHIVED definitions cannot be rolled back.
     */
    @Transactional
    public void rollback(String defId, Integer targetVersion, String operatorName) {
        BpmProcessDefinition def = defMapper.selectById(defId);
        if (def == null) throw new IllegalArgumentException("definition not found: " + defId);
        if ("ARCHIVED".equals(def.getState())) {
            throw new IllegalStateTransitionException("ARCHIVED", "DRAFT");
        }
        BpmProcessDefinitionHistory snap = historyService.getByVersion(defId, targetVersion);
        if (snap == null) throw new IllegalArgumentException("version not found: " + targetVersion);

        def.setBpmnXml(snap.getBpmnXml());
        def.setState("DRAFT");
        def.setUpdateBy(operatorName);
        // version field stays — next publish will create version+1
        defMapper.updateById(def);
    }
}
