package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bpm.definition.dto.*;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.definition.support.BpmnXmlValidator;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BpmProcessDefinitionServiceImpl
        extends ServiceImpl<BpmProcessDefinitionMapper, BpmProcessDefinition>
        implements BpmProcessDefinitionService {

    private final BpmUserContext userContext;
    private final BpmProcessDefinitionHistoryService historyService;
    private final BpmnXmlValidator bpmnValidator;

    public BpmProcessDefinitionServiceImpl(BpmUserContext userContext,
                                           BpmProcessDefinitionHistoryService historyService,
                                           BpmnXmlValidator bpmnValidator) {
        this.userContext = userContext;
        this.historyService = historyService;
        this.bpmnValidator = bpmnValidator;
    }

    @Override
    public IPage<DefinitionVO> queryPage(DefinitionQueryRequest req) {
        LambdaQueryWrapper<BpmProcessDefinition> q = new LambdaQueryWrapper<>();
        if (req.getDefKey() != null && !req.getDefKey().isEmpty())
            q.like(BpmProcessDefinition::getDefKey, req.getDefKey());
        if (req.getName() != null && !req.getName().isEmpty())
            q.like(BpmProcessDefinition::getName, req.getName());
        if (req.getState() != null) q.eq(BpmProcessDefinition::getState, req.getState());
        if (req.getCategory() != null) q.eq(BpmProcessDefinition::getCategory, req.getCategory());
        q.orderByDesc(BpmProcessDefinition::getCreateTime);
        Page<BpmProcessDefinition> page = new Page<>(req.getPageNo(), req.getPageSize());
        IPage<BpmProcessDefinition> res = baseMapper.selectPage(page, q);
        return res.convert(this::toVOWithoutXml);
    }

    @Override
    @Transactional
    public DefinitionVO createDraft(DefinitionCreateRequest req) {
        BpmProcessDefinition e = new BpmProcessDefinition();
        BeanUtils.copyProperties(req, e);
        e.setVersion(1);
        e.setState("DRAFT");
        if (e.getCategory() == null) e.setCategory("DEFAULT");
        e.setTenantId("default");
        String username = userContext.currentUsername();
        e.setCreateBy(username);
        e.setUpdateBy(username);
        save(e);
        return toVODetail(e);
    }

    @Override
    public DefinitionVO getDetail(String id) {
        BpmProcessDefinition e = getById(id);
        if (e == null) return null;
        return toVODetail(e);
    }

    @Override
    @Transactional
    public DefinitionVO update(String id, DefinitionUpdateRequest req) {
        BpmProcessDefinition e = getById(id);
        if (e == null) throw new IllegalArgumentException("definition not found: " + id);
        if ("ARCHIVED".equals(e.getState()))
            throw new IllegalStateException("cannot update ARCHIVED definition");
        if (req.getName() != null) e.setName(req.getName());
        if (req.getCategory() != null) e.setCategory(req.getCategory());
        if (req.getDescription() != null) e.setDescription(req.getDescription());
        if (req.getBpmnXml() != null) e.setBpmnXml(req.getBpmnXml());
        if (req.getFormId() != null) e.setFormId(req.getFormId());
        e.setUpdateBy(userContext.currentUsername());
        updateById(e);
        return toVODetail(e);
    }

    @Override
    @Transactional
    public void delete(String id) {
        BpmProcessDefinition e = getById(id);
        if (e == null) return;
        if ("PUBLISHED".equals(e.getState()))
            throw new IllegalStateException("cannot delete PUBLISHED definition; archive first");
        removeById(id);
    }

    @Override
    @Transactional
    public DefinitionVO publish(String id, String changeNote) {
        BpmProcessDefinition e = getById(id);
        if (e == null) throw new IllegalArgumentException("definition not found: " + id);
        if (!"DRAFT".equals(e.getState()))
            throw new IllegalStateException("only DRAFT can be published in P1; current=" + e.getState());
        if (e.getBpmnXml() == null || e.getBpmnXml().isEmpty())
            throw new IllegalStateException("bpmn_xml is empty");
        bpmnValidator.validate(e.getBpmnXml());

        e.setState("PUBLISHED");
        e.setUpdateBy(userContext.currentUsername());
        updateById(e);
        historyService.snapshot(e.getId(), e.getDefKey(), e.getVersion(),
                e.getBpmnXml(), changeNote, userContext.currentUsername());
        return toVODetail(e);
    }

    @Override
    public List<BpmProcessDefinitionHistory> versions(String id) {
        return historyService.listByDefId(id);
    }

    private DefinitionVO toVOWithoutXml(BpmProcessDefinition e) {
        DefinitionVO v = new DefinitionVO();
        BeanUtils.copyProperties(e, v);
        v.setBpmnXml(null);
        return v;
    }

    private DefinitionVO toVODetail(BpmProcessDefinition e) {
        DefinitionVO v = new DefinitionVO();
        BeanUtils.copyProperties(e, v);
        return v;
    }
}
