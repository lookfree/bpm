package org.jeecg.modules.bpm.definition.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmnXmlValidatorTest {

    private final BpmnXmlValidator validator = new BpmnXmlValidator();

    private String load(String path) throws Exception {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }

    @Test
    void validBpmnIsAccepted() throws Exception {
        BpmnXmlValidator.Result r = validator.validate(load("bpm/valid-definition.bpmn20.xml"));
        assertThat(r.isValid()).isTrue();
        assertThat(r.getProcessId()).isEqualTo("bpm_demo");
    }

    @Test
    void invalidBpmnIsRejected() throws Exception {
        assertThatThrownBy(() -> validator.validate(load("bpm/invalid-definition.bpmn20.xml")))
                .isInstanceOf(BpmnXmlValidator.InvalidBpmnException.class);
    }

    @Test
    void blankInputRejected() {
        assertThatThrownBy(() -> validator.validate("  "))
                .isInstanceOf(BpmnXmlValidator.InvalidBpmnException.class);
    }
}
