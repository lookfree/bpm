package org.jeecg.modules.bpm.multi;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class MultiInstanceXmlRewriter {

    private static final String BPMN_NS     = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    private static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    public String rewrite(String bpmnXml, Map<String, MultiModeConfig> nodeConfigs) {
        if (nodeConfigs == null || nodeConfigs.isEmpty()) return bpmnXml;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(
                    new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)));

            NodeList userTasks = doc.getElementsByTagNameNS(BPMN_NS, "userTask");
            for (int i = 0; i < userTasks.getLength(); i++) {
                Element ut = (Element) userTasks.item(i);
                String id = ut.getAttribute("id");
                MultiModeConfig cfg = nodeConfigs.get(id);
                if (cfg == null) continue;
                injectMultiInstance(doc, ut, id, cfg);
            }

            StringWriter sw = new StringWriter();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            throw new IllegalStateException("rewrite multiInstance failed", e);
        }
    }

    private void injectMultiInstance(Document doc, Element userTask, String nodeId, MultiModeConfig cfg) {
        Element mi = doc.createElementNS(BPMN_NS, "multiInstanceLoopCharacteristics");
        mi.setAttribute("isSequential", "SEQUENCE".equals(cfg.mode()) ? "true" : "false");
        mi.setAttributeNS(FLOWABLE_NS, "flowable:collection", "${bpm_assignees_" + nodeId + "}");
        mi.setAttributeNS(FLOWABLE_NS, "flowable:elementVariable", "assignee");

        Element completion = doc.createElementNS(BPMN_NS, "completionCondition");
        completion.setTextContent(buildCompletionCondition(cfg));
        mi.appendChild(completion);

        // Override assignee attribute to use elementVariable
        userTask.setAttributeNS(FLOWABLE_NS, "flowable:assignee", "${assignee}");

        Node first = userTask.getFirstChild();
        if (first != null) userTask.insertBefore(mi, first);
        else userTask.appendChild(mi);
    }

    private String buildCompletionCondition(MultiModeConfig cfg) {
        switch (cfg.mode()) {
            case "PARALLEL": return "${nrOfCompletedInstances/nrOfInstances >= 1.0}";
            case "ANY":      return "${nrOfCompletedInstances >= 1}";
            case "SEQUENCE": return "${nrOfCompletedInstances == nrOfInstances}";
            default: throw new IllegalArgumentException("unknown multi mode: " + cfg.mode());
        }
    }
}
