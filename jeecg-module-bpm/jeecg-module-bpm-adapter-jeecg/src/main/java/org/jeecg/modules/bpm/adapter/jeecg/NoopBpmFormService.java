package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;

import java.util.Collections;
import java.util.Map;

public class NoopBpmFormService implements BpmFormService {
    @Override public BpmFormSchema loadFormSchema(String formId) { return null; }

    @Override public String saveFormSubmission(String formId, Map<String, Object> data) {
        return "noop-" + (formId == null ? "null" : formId);
    }

    @Override public Map<String, Object> loadFormData(String formId, String businessKey) {
        return Collections.emptyMap();
    }
}
