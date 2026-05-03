package org.jeecg.modules.bpm.multi;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class MultiInstanceXmlRewriterTest {

    private static final String INPUT_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
            "xmlns:flowable=\"http://flowable.org/bpmn\" " +
            "targetNamespace=\"x\" id=\"d\">" +
            "  <process id=\"p\" isExecutable=\"true\">" +
            "    <userTask id=\"t1\" name=\"Approve\"/>" +
            "  </process>" +
            "</definitions>";

    @Test
    void rewritesParallelMultiInstance() {
        MultiInstanceXmlRewriter r = new MultiInstanceXmlRewriter();
        String out = r.rewrite(INPUT_XML, Map.of("t1", new MultiModeConfig("PARALLEL")));
        assertThat(out).contains("multiInstanceLoopCharacteristics");
        assertThat(out).contains("isSequential=\"false\"");
        // completion condition contains nrOfCompletedInstances (>= may be escaped as &gt;=)
        assertThat(out).containsAnyOf("nrOfCompletedInstances/nrOfInstances", "nrOfCompletedInstances");
        assertThat(out).contains("bpm_assignees_t1");
        assertThat(out).contains("assignee");
    }

    @Test
    void rewritesAnyMultiInstance() {
        String out = new MultiInstanceXmlRewriter().rewrite(INPUT_XML, Map.of("t1", new MultiModeConfig("ANY")));
        // XML serializer escapes '>' as '&gt;' inside text content
        assertThat(out).satisfiesAnyOf(
                s -> assertThat(s).contains("nrOfCompletedInstances >= 1"),
                s -> assertThat(s).contains("nrOfCompletedInstances &gt;= 1")
        );
        assertThat(out).contains("isSequential=\"false\"");
    }

    @Test
    void rewritesSequenceMultiInstance() {
        String out = new MultiInstanceXmlRewriter().rewrite(INPUT_XML, Map.of("t1", new MultiModeConfig("SEQUENCE")));
        assertThat(out).contains("isSequential=\"true\"");
        assertThat(out).contains("nrOfCompletedInstances == nrOfInstances");
    }

    @Test
    void leavesXmlUntouchedWhenNoConfig() {
        String out = new MultiInstanceXmlRewriter().rewrite(INPUT_XML, Map.of());
        assertThat(out).doesNotContain("multiInstanceLoopCharacteristics");
    }
}
