package org.jeecg.modules.bpm.definition.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BpmProcessDefinitionEntityTest {
    @Test
    void equalityIsByIdOnly() {
        BpmProcessDefinition a = new BpmProcessDefinition();
        a.setId("uuid-1"); a.setName("A");
        BpmProcessDefinition b = new BpmProcessDefinition();
        b.setId("uuid-1"); b.setName("B");
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
