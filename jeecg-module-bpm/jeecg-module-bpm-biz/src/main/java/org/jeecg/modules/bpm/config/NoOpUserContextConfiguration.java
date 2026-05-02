package org.jeecg.modules.bpm.config;

import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.jeecg.modules.bpm.support.NoOpBpmUserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoOpUserContextConfiguration {

    @Bean
    @ConditionalOnMissingBean(BpmUserContext.class)
    public BpmUserContext noOpBpmUserContext() {
        return new NoOpBpmUserContext();
    }
}
