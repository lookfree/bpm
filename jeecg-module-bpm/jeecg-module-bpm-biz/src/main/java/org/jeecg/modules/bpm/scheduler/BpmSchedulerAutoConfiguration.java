package org.jeecg.modules.bpm.scheduler;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(BpmSchedulerProperties.class)
public class BpmSchedulerAutoConfiguration {
}
