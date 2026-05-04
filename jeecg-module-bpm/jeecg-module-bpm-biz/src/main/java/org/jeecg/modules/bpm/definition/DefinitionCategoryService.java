package org.jeecg.modules.bpm.definition;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinition;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionMapper;
import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.domain.entity.NodeConfig;
import org.jeecg.modules.bpm.mapper.FormBindingMapper;
import org.jeecg.modules.bpm.mapper.NodeConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class DefinitionCategoryService {

    private final BpmProcessDefinitionMapper defMapper;
    private final NodeConfigMapper nodeConfigMapper;
    private final FormBindingMapper formBindingMapper;

    public DefinitionCategoryService(BpmProcessDefinitionMapper defMapper,
                                     NodeConfigMapper nodeConfigMapper,
                                     FormBindingMapper formBindingMapper) {
        this.defMapper = defMapper;
        this.nodeConfigMapper = nodeConfigMapper;
        this.formBindingMapper = formBindingMapper;
    }

    @Transactional
    public String cloneAsSandbox(String prodDefId, String operatorName) {
        BpmProcessDefinition src = defMapper.selectById(prodDefId);
        if (src == null) throw new IllegalArgumentException("definition not found: " + prodDefId);
        if ("ARCHIVED".equals(src.getState())) {
            throw new IllegalStateException("cannot clone an archived definition");
        }

        BpmProcessDefinition copy = new BpmProcessDefinition();
        copy.setDefKey(src.getDefKey() + "_sandbox_" + System.currentTimeMillis());
        copy.setName(src.getName() + " [Sandbox]");
        copy.setCategory("SANDBOX");
        copy.setState("DRAFT");
        copy.setVersion(0);
        copy.setBpmnXml(src.getBpmnXml());
        copy.setFormId(src.getFormId());
        copy.setDescription(src.getDescription());
        copy.setTenantId(src.getTenantId());
        copy.setCreateBy(operatorName);
        copy.setUpdateBy(operatorName);
        copy.setCreateTime(new Date());
        copy.setUpdateTime(new Date());
        defMapper.insert(copy);

        // Clone NodeConfig rows
        LambdaQueryWrapper<NodeConfig> ncQ = new LambdaQueryWrapper<>();
        ncQ.eq(NodeConfig::getDefId, prodDefId);
        List<NodeConfig> nodes = nodeConfigMapper.selectList(ncQ);
        for (NodeConfig n : nodes) {
            NodeConfig nc = new NodeConfig();
            nc.setDefId(copy.getId());
            nc.setNodeId(n.getNodeId());
            nc.setAssigneeStrategy(n.getAssigneeStrategy());
            if (n.getMultiMode() != null) nc.setMultiMode(n.getMultiMode());
            if (n.getFormPerm() != null) nc.setFormPerm(n.getFormPerm());
            if (n.getTimeoutHours() != null) nc.setTimeoutHours(n.getTimeoutHours());
            if (n.getTimeoutAction() != null) nc.setTimeoutAction(n.getTimeoutAction());
            nodeConfigMapper.insert(nc);
        }

        // Clone FormBinding rows
        LambdaQueryWrapper<FormBinding> fbQ = new LambdaQueryWrapper<>();
        fbQ.eq(FormBinding::getDefId, prodDefId);
        List<FormBinding> bindings = formBindingMapper.selectList(fbQ);
        for (FormBinding b : bindings) {
            FormBinding fb = new FormBinding();
            fb.setDefId(copy.getId());
            fb.setFormId(b.getFormId());
            fb.setPurpose(b.getPurpose());
            formBindingMapper.insert(fb);
        }

        return copy.getId();
    }

    public List<BpmProcessDefinition> listByCategory(String category) {
        LambdaQueryWrapper<BpmProcessDefinition> q = new LambdaQueryWrapper<>();
        q.eq(BpmProcessDefinition::getCategory, category);
        q.orderByDesc(BpmProcessDefinition::getCreateTime);
        return defMapper.selectList(q);
    }

    public List<BpmProcessDefinition> listAll() {
        return defMapper.selectList(null);
    }
}
