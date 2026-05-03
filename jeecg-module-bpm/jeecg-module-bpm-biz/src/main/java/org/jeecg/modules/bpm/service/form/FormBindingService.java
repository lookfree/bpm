package org.jeecg.modules.bpm.service.form;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.bpm.domain.entity.FormBinding;
import org.jeecg.modules.bpm.mapper.FormBindingMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FormBindingService {

    private final FormBindingMapper mapper;

    public FormBindingService(FormBindingMapper mapper) {
        this.mapper = mapper;
    }

    public String bind(String defId, String formId, String purpose) {
        // Check idempotency first
        FormBinding existing = mapper.selectOne(
                new LambdaQueryWrapper<FormBinding>()
                        .eq(FormBinding::getDefId, defId)
                        .eq(FormBinding::getFormId, formId)
                        .eq(FormBinding::getPurpose, purpose)
        );
        if (existing != null) return existing.getId();

        FormBinding fb = new FormBinding();
        fb.setId(UUID.randomUUID().toString().replace("-", ""));
        fb.setDefId(defId);
        fb.setFormId(formId);
        fb.setPurpose(purpose);
        try {
            mapper.insert(fb);
        } catch (DuplicateKeyException e) {
            // race condition — re-query
            existing = mapper.selectOne(
                    new LambdaQueryWrapper<FormBinding>()
                            .eq(FormBinding::getDefId, defId)
                            .eq(FormBinding::getFormId, formId)
                            .eq(FormBinding::getPurpose, purpose)
            );
            return existing != null ? existing.getId() : fb.getId();
        }
        return fb.getId();
    }

    public List<FormBinding> listByDef(String defId) {
        return mapper.selectList(
                new LambdaQueryWrapper<FormBinding>().eq(FormBinding::getDefId, defId)
        );
    }

    public void unbind(String bindingId) {
        mapper.deleteById(bindingId);
    }
}
