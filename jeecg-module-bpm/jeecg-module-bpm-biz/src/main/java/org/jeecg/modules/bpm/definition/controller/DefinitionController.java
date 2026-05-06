package org.jeecg.modules.bpm.definition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.bpm.common.BpmResult;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bpm/v1/definition")
public class DefinitionController {

    private final BpmProcessDefinitionService service;
    private final BpmnXmlValidator bpmnValidator;

    public DefinitionController(BpmProcessDefinitionService service,
                                BpmnXmlValidator bpmnValidator) {
        this.service = service;
        this.bpmnValidator = bpmnValidator;
    }

    @GetMapping
    public BpmResult<IPage<DefinitionVO>> list(DefinitionQueryRequest q) {
        return BpmResult.ok(service.queryPage(q));
    }

    @PostMapping
    public BpmResult<DefinitionVO> create(@RequestBody DefinitionCreateRequest req) {
        if (req.getDefKey() == null || req.getDefKey().isEmpty()
                || req.getName() == null || req.getName().isEmpty()) {
            return BpmResult.error("defKey 和 name 不能为空");
        }
        if (req.getBpmnXml() != null && !req.getBpmnXml().isEmpty()) {
            bpmnValidator.validate(req.getBpmnXml());
        }
        return BpmResult.ok(service.createDraft(req));
    }

    @GetMapping("/{id}")
    public BpmResult<DefinitionVO> get(@PathVariable String id) {
        DefinitionVO vo = service.getDetail(id);
        return vo == null ? BpmResult.error("流程定义不存在") : BpmResult.ok(vo);
    }

    @PutMapping("/{id}")
    public BpmResult<DefinitionVO> update(@PathVariable String id, @RequestBody DefinitionUpdateRequest req) {
        if (req.getBpmnXml() != null && !req.getBpmnXml().isEmpty()) {
            bpmnValidator.validate(req.getBpmnXml());
        }
        return BpmResult.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BpmResult<Void> delete(@PathVariable String id) {
        service.delete(id);
        return BpmResult.ok();
    }

    @PostMapping("/{id}/publish")
    public BpmResult<DefinitionVO> publish(@PathVariable String id,
                                        @RequestParam(required = false) String changeNote) {
        return BpmResult.ok(service.publish(id, changeNote));
    }

    @GetMapping("/{id}/versions")
    public BpmResult<List<BpmProcessDefinitionHistory>> versions(@PathVariable String id) {
        return BpmResult.ok(service.versions(id));
    }

    /** 无需登录，调试用，上线前删除 */
    @GetMapping("/debug-count")
    public String debugCount() {
        return "count=" + service.count();
    }

    @GetMapping("/debug-list")
    public Object debugList() {
        DefinitionQueryRequest q = new DefinitionQueryRequest();
        return service.queryPage(q);
    }

    @GetMapping("/debug-list-wrapped")
    public BpmResult<?> debugListWrapped() {
        DefinitionQueryRequest q = new DefinitionQueryRequest();
        return BpmResult.ok(service.queryPage(q));
    }

    @ExceptionHandler(BpmnXmlValidator.InvalidBpmnException.class)
    public BpmResult<String> onInvalidBpmn(BpmnXmlValidator.InvalidBpmnException e) {
        return BpmResult.error(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public BpmResult<String> onConflict(IllegalStateException e) {
        return BpmResult.error(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BpmResult<String> onNotFound(IllegalArgumentException e) {
        return BpmResult.error(e.getMessage());
    }
}