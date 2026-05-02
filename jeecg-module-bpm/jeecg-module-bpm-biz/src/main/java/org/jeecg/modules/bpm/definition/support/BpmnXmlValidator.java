package org.jeecg.modules.bpm.definition.support;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;

@Component
public class BpmnXmlValidator {

    public Result validate(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            throw new InvalidBpmnException("BPMN XML is blank");
        }
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader sr = xif.createXMLStreamReader(new StringReader(xml));
            BpmnModel model = new BpmnXMLConverter().convertToBpmnModel(sr);
            if (model.getProcesses().isEmpty()) {
                throw new InvalidBpmnException("no <process> element found");
            }
            Process p = model.getProcesses().get(0);
            if (p.getId() == null || p.getId().isEmpty()) {
                throw new InvalidBpmnException("process must have id");
            }
            return new Result(p.getId());
        } catch (InvalidBpmnException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidBpmnException("BPMN parse failed: " + e.getMessage(), e);
        }
    }

    public static class Result {
        private final boolean valid = true;
        private final String processId;
        public Result(String processId) { this.processId = processId; }
        public boolean isValid() { return valid; }
        public String getProcessId() { return processId; }
    }

    public static class InvalidBpmnException extends RuntimeException {
        public InvalidBpmnException(String msg) { super(msg); }
        public InvalidBpmnException(String msg, Throwable cause) { super(msg, cause); }
    }
}
