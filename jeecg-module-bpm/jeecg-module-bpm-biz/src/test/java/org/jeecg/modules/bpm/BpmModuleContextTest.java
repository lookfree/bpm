package org.jeecg.modules.bpm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BpmModuleContextTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(BpmModuleAutoConfiguration.class));

    @Test
    void shouldLoadBpmModuleAutoConfiguration() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(BpmModuleAutoConfiguration.class));
    }
}
