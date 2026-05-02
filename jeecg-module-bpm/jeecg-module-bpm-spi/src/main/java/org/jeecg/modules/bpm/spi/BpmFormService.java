package org.jeecg.modules.bpm.spi;

import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;

import java.util.Map;

public interface BpmFormService {
    /** 加载表单 schema；未找到返回 null */
    BpmFormSchema loadFormSchema(String formId);

    /** 保存一次表单提交，返回业务键（business_key），与 act_ru_execution.business_key_ 关联 */
    String saveFormSubmission(String formId, Map<String, Object> data);

    /** 加载已有表单数据（审批节点回显） */
    Map<String, Object> loadFormData(String formId, String businessKey);
}
