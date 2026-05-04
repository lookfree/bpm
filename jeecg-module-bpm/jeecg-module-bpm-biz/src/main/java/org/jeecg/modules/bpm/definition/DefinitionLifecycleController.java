package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionHistoryService;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bpm/v1/definition")
public class DefinitionLifecycleController {

    private final BpmProcessDefinitionService definitionService;
    private final DefinitionArchiveService archiveService;
    private final DefinitionRollbackService rollbackService;
    private final BpmProcessDefinitionHistoryService historyService;
    private final BpmUserContext userContext;
    private final DefinitionCategoryService categoryService;

    public DefinitionLifecycleController(BpmProcessDefinitionService definitionService,
                                         DefinitionArchiveService archiveService,
                                         DefinitionRollbackService rollbackService,
                                         BpmProcessDefinitionHistoryService historyService,
                                         BpmUserContext userContext,
                                         DefinitionCategoryService categoryService) {
        this.definitionService = definitionService;
        this.archiveService = archiveService;
        this.rollbackService = rollbackService;
        this.historyService = historyService;
        this.userContext = userContext;
        this.categoryService = categoryService;
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

    @PostMapping("/{id}/clone-as-sandbox")
    public Map<String, String> cloneAsSandbox(@PathVariable String id) {
        String sandboxDefId = categoryService.cloneAsSandbox(id, userContext.currentUsername());
        Map<String, String> result = new HashMap<>();
        result.put("sandboxDefId", sandboxDefId);
        return result;
    }
}
