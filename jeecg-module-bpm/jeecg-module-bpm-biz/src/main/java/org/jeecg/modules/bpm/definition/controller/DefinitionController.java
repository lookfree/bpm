package org.jeecg.modules.bpm.definition.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.service.BpmProcessDefinitionService;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public IPage<DefinitionVO> list(DefinitionQueryRequest q) {
        return service.queryPage(q);
    }

    @PostMapping
    public ResponseEntity<DefinitionVO> create(@RequestBody DefinitionCreateRequest req) {
        if (req.getDefKey() == null || req.getDefKey().isEmpty()
                || req.getName() == null || req.getName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (req.getBpmnXml() != null && !req.getBpmnXml().isEmpty()) {
            bpmnValidator.validate(req.getBpmnXml());
        }
        DefinitionVO vo = service.createDraft(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(vo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DefinitionVO> get(@PathVariable String id) {
        DefinitionVO vo = service.getDetail(id);
        return vo == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(vo);
    }

    @PutMapping("/{id}")
    public DefinitionVO update(@PathVariable String id, @RequestBody DefinitionUpdateRequest req) {
        if (req.getBpmnXml() != null && !req.getBpmnXml().isEmpty()) {
            bpmnValidator.validate(req.getBpmnXml());
        }
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public DefinitionVO publish(@PathVariable String id,
                                @RequestParam(required = false) String changeNote) {
        return service.publish(id, changeNote);
    }

    @GetMapping("/{id}/versions")
    public List<BpmProcessDefinitionHistory> versions(@PathVariable String id) {
        return service.versions(id);
    }

    @ExceptionHandler(BpmnXmlValidator.InvalidBpmnException.class)
    public ResponseEntity<String> onInvalidBpmn(BpmnXmlValidator.InvalidBpmnException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> onConflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> onNotFound(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }
}
