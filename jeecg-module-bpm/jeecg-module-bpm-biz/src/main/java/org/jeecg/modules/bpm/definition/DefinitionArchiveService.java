package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefinitionArchiveService {

    private final BpmProcessDefinitionMapper definitionMapper;
    private final DefinitionLifecycleService lifecycle;

    public DefinitionArchiveService(BpmProcessDefinitionMapper definitionMapper,
                                    DefinitionLifecycleService lifecycle) {
        this.definitionMapper = definitionMapper;
        this.lifecycle = lifecycle;
    }

    @Transactional
    public void archive(String defId, String operatorName) {
        BpmProcessDefinition def = definitionMapper.selectById(defId);
        if (def == null) throw new IllegalArgumentException("definition not found: " + defId);
        DefinitionLifecycleService.State from = DefinitionLifecycleService.State.valueOf(def.getState());
        lifecycle.assertAllowed(from, DefinitionLifecycleService.State.ARCHIVED);
        def.setState("ARCHIVED");
        def.setUpdateBy(operatorName);
        definitionMapper.updateById(def);
    }
}
