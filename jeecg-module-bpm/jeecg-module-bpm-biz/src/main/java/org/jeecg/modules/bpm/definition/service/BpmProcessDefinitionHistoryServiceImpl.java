package org.jeecg.modules.bpm.definition.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.bpm.definition.entity.BpmProcessDefinitionHistory;
import org.jeecg.modules.bpm.definition.mapper.BpmProcessDefinitionHistoryMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BpmProcessDefinitionHistoryServiceImpl
        extends ServiceImpl<BpmProcessDefinitionHistoryMapper, BpmProcessDefinitionHistory>
        implements BpmProcessDefinitionHistoryService {

    @Override
    public String snapshot(String defId, String defKey, int version, String bpmnXml,
                           String changeNote, String publishedBy) {
        BpmProcessDefinitionHistory h = new BpmProcessDefinitionHistory();
        h.setDefId(defId);
        h.setDefKey(defKey);
        h.setVersion(version);
        h.setBpmnXml(bpmnXml);
        h.setChangeNote(changeNote);
        h.setPublishedBy(publishedBy);
        h.setPublishedTime(new Date());
        save(h);
        return h.getId();
    }

    @Override
    public List<BpmProcessDefinitionHistory> listByDefId(String defId) {
        LambdaQueryWrapper<BpmProcessDefinitionHistory> q = new LambdaQueryWrapper<>();
        q.eq(BpmProcessDefinitionHistory::getDefId, defId)
         .orderByDesc(BpmProcessDefinitionHistory::getVersion);
        return list(q);
    }
}
