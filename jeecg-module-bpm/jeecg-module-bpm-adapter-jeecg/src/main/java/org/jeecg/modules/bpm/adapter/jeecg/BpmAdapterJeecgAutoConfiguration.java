package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmNotificationSender;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(ISysBaseAPI.class)
public class BpmAdapterJeecgAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BpmUserContext.class)
    public BpmUserContext bpmUserContext(ISysBaseAPI sysBaseAPI) {
        return new JeecgBpmUserContext(sysBaseAPI);
    }

    @Bean
    @ConditionalOnMissingBean(BpmOrgService.class)
    public BpmOrgService bpmOrgService(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return new BpmOrgServiceJeecgImpl(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(BpmFormService.class)
    public BpmFormService bpmFormService(org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        return new BpmFormServiceJeecgImpl(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(BpmNotificationSender.class)
    public BpmNotificationSender bpmNotificationSender() {
        return new NoopBpmNotificationSender();
    }
}
