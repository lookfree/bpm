package org.jeecg.modules.bpm;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class BpmModuleContextTest {

    @Test
    void isMarkedAsConfiguration() {
        assertThat(BpmModuleAutoConfiguration.class.isAnnotationPresent(Configuration.class)).isTrue();
        assertThat(BpmModuleAutoConfiguration.class.isAnnotationPresent(ComponentScan.class)).isTrue();
    }

    @Test
    void registeredInSpringFactories() throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/META-INF/spring.factories")) {
            assertThat(in).as("spring.factories must be on classpath").isNotNull();
            props.load(in);
        }
        String key = "org.springframework.boot.autoconfigure.EnableAutoConfiguration";
        assertThat(props.getProperty(key))
                .as("BpmModuleAutoConfiguration must be registered as auto-config")
                .contains(BpmModuleAutoConfiguration.class.getName());
    }
}
