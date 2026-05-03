package org.jeecg.modules.bpm.flow;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for JOB_EXECUTION_FAILURE events. If the failure is due to
 * "No outgoing sequence flow", creates a manual review task via ManualReviewTaskCreator.
 */
@Component
public class NoMatchingFlowHandler extends AbstractFlowableEngineEventListener {

    private static final String NO_OUTGOING_MARKER = "No outgoing sequence flow";

    private final ManualReviewTaskCreator creator;

    public NoMatchingFlowHandler(ManualReviewTaskCreator creator) {
        this.creator = creator;
    }

    @Override
    public void onEvent(FlowableEvent event) {
        if (event.getType() != FlowableEngineEventType.JOB_EXECUTION_FAILURE) return;

        String processInstanceId = null;
        if (event instanceof FlowableEngineEvent) {
            processInstanceId = ((FlowableEngineEvent) event).getProcessInstanceId();
        }

        String causeMsg = null;
        try {
            java.lang.reflect.Method m = event.getClass().getMethod("getCause");
            Throwable cause = (Throwable) m.invoke(event);
            if (cause != null) causeMsg = cause.getMessage();
        } catch (Exception ignored) {
            // getCause() not available on this event implementation
        }

        if (causeMsg == null || !causeMsg.contains(NO_OUTGOING_MARKER)) return;
        if (processInstanceId == null) return;
        creator.createForInstance(processInstanceId, causeMsg);
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }
}
