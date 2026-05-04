package org.jeecg.modules.bpm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "org.jeecg.modules.bpm")
@MapperScan(basePackages = {
        "org.jeecg.modules.bpm.**.mapper",
        "org.jeecg.modules.bpm.sandbox"
})
public class BpmModuleAutoConfiguration {
}
