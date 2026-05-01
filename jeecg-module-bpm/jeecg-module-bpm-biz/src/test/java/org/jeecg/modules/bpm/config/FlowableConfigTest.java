package org.jeecg.modules.bpm.config;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FlowableConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(FlowableConfig.class);

    @Test
    void exposesUuidIdGenerator() {
        runner.run(ctx -> {
            IdGenerator gen = ctx.getBean(IdGenerator.class);
            String first = gen.getNextId();
            String second = gen.getNextId();
            assertThat(first).isNotEqualTo(second);
            assertThat(first).hasSize(36);
        });
    }
}
