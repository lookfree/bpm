package org.jeecg.modules.bpm.config;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.dto.BpmFormSchema;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 兜底 SPI 实现：当宿主模块未提供真实 BpmOrgService / BpmFormService 时（典型：单元/集成测试）
 * 加载本 noop fallback。生产环境由 adapter-jeecg 覆盖。
 */
@Configuration
public class NoOpSpiConfiguration {

    @Bean
    @ConditionalOnMissingBean(BpmOrgService.class)
    public BpmOrgService noOpBpmOrgService() {
        return new BpmOrgService() {
            @Override public List<Long> findDeptLeaders(Long deptId) { return Collections.emptyList(); }
            @Override public List<Long> findUpperDeptLeaders(Long deptId) { return Collections.emptyList(); }
            @Override public List<Long> findUsersByRole(String roleCode) { return Collections.emptyList(); }
            @Override public List<Long> findUsersByPosition(String positionCode) { return Collections.emptyList(); }
            @Override public boolean isUserActive(Long userId) { return true; }
            @Override public String findUserName(Long userId) { return null; }
            @Override public String findDeptName(Long deptId) { return null; }
            @Override public Long findUserMainDeptId(Long userId) { return null; }
        };
    }

    @Bean
    @ConditionalOnMissingBean(BpmFormService.class)
    public BpmFormService noOpBpmFormService() {
        return new BpmFormService() {
            @Override public BpmFormSchema loadFormSchema(String formId) { return null; }
            @Override public String saveFormSubmission(String formId, Map<String, Object> data) { return formId + "_noop"; }
            @Override public Map<String, Object> loadFormData(String formId, String businessKey) { return Collections.emptyMap(); }
        };
    }
}
