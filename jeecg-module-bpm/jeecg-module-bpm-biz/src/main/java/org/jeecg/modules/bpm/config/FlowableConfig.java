package org.jeecg.modules.bpm.config;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.persistence.StrongUuidGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlowableConfig {

    @Bean
    public IdGenerator flowableIdGenerator() {
        return new StrongUuidGenerator();
    }
}
