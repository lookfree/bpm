package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmNotificationSender;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BpmAdapterJeecgAutoConfigurationTest {

    @Test
    void registersAllFourSpiBeans() {
        new ApplicationContextRunner()
                .withBean(ISysBaseAPI.class, () -> mock(ISysBaseAPI.class))
                .withConfiguration(AutoConfigurations.of(BpmAdapterJeecgAutoConfiguration.class))
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(BpmUserContext.class);
                    assertThat(ctx).hasSingleBean(BpmOrgService.class);
                    assertThat(ctx).hasSingleBean(BpmFormService.class);
                    assertThat(ctx).hasSingleBean(BpmNotificationSender.class);
                    assertThat(ctx.getBean(BpmUserContext.class)).isInstanceOf(JeecgBpmUserContext.class);
                });
    }
}
