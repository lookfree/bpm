package org.jeecg.modules.bpm;

import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.jeecg.modules.bpm.support.NoOpBpmUserContext;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
    basePackages = "org.jeecg.modules.bpm",
    excludeFilters = @Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = org.jeecg.modules.bpm.config.NoOpUserContextConfiguration.class
    )
)
@MapperScan(basePackages = {
        "org.jeecg.modules.bpm.**.mapper",
        "org.jeecg.modules.bpm.sandbox",
        "org.jeecg.modules.bpm.scheduler.cleanup"
})
@AutoConfigureAfter(name = "org.jeecg.modules.bpm.adapter.jeecg.BpmAdapterJeecgAutoConfiguration")
public class BpmModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BpmUserContext.class)
    public BpmUserContext noOpBpmUserContext() {
        return new NoOpBpmUserContext();
    }
}
