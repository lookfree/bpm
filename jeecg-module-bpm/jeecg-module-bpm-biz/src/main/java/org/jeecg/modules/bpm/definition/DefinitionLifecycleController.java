package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionHistoryService;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bpm/v1/definition")
public class DefinitionLifecycleController {

    private final BpmProcessDefinitionService definitionService;
    private final DefinitionArchiveService archiveService;
    private final DefinitionRollbackService rollbackService;
    private final BpmProcessDefinitionHistoryService historyService;
    private final BpmUserContext userContext;

    public DefinitionLifecycleController(BpmProcessDefinitionService definitionService,
                                         DefinitionArchiveService archiveService,
                                         DefinitionRollbackService rollbackService,
                                         BpmProcessDefinitionHistoryService historyService,
                                         BpmUserContext userContext) {
        this.definitionService = definitionService;
        this.archiveService = archiveService;
        this.rollbackService = rollbackService;
        this.historyService = historyService;
        this.userContext = userContext;
    }

    @PostMapping("/{id}/archive")
    public void archive(@PathVariable String id) {
        archiveService.archive(id, userContext.currentUsername());
    }

    @PostMapping("/{id}/rollback")
    public void rollback(@PathVariable String id,
                         @RequestParam Integer targetVersion) {
        rollbackService.rollback(id, targetVersion, userContext.currentUsername());
    }
}
