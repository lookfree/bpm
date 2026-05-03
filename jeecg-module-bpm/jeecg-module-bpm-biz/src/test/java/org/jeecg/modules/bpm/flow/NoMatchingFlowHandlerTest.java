package org.jeecg.modules.bpm.flow;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NoMatchingFlowHandlerTest {

    @Test
    void delegatesToManualReviewWhenNoMatchingFlow() {
        ManualReviewTaskCreator creator = mock(ManualReviewTaskCreator.class);
        NoMatchingFlowHandler handler = new NoMatchingFlowHandler(creator);

        handler.onEvent(new StubJobFailureEvent("inst_1",
                new RuntimeException("No outgoing sequence flow of the exclusive gateway 'gw' could be selected")));

        verify(creator).createForInstance(eq("inst_1"), contains("No outgoing sequence flow"));
    }

    @Test
    void ignoresUnrelatedJobFailures() {
        ManualReviewTaskCreator creator = mock(ManualReviewTaskCreator.class);
        NoMatchingFlowHandler handler = new NoMatchingFlowHandler(creator);

        handler.onEvent(new StubJobFailureEvent("inst_1", new RuntimeException("connection lost")));

        verifyNoInteractions(creator);
    }

    @Test
    void ignoresNonJobFailureEvents() {
        ManualReviewTaskCreator creator = mock(ManualReviewTaskCreator.class);
        NoMatchingFlowHandler handler = new NoMatchingFlowHandler(creator);

        org.flowable.common.engine.api.delegate.event.FlowableEvent evt =
                mock(org.flowable.common.engine.api.delegate.event.FlowableEvent.class);
        when(evt.getType()).thenReturn(FlowableEngineEventType.TASK_CREATED);

        handler.onEvent(evt);
        verifyNoInteractions(creator);
    }

    /** Minimal stub that implements FlowableEvent + FlowableEngineEvent with a cause. */
    static class StubJobFailureEvent implements
            org.flowable.common.engine.api.delegate.event.FlowableEvent,
            FlowableEngineEvent {

        private final String processInstanceId;
        private final Throwable cause;

        StubJobFailureEvent(String processInstanceId, Throwable cause) {
            this.processInstanceId = processInstanceId;
            this.cause = cause;
        }

        @Override public FlowableEngineEventType getType() { return FlowableEngineEventType.JOB_EXECUTION_FAILURE; }
        @Override public String getProcessDefinitionId() { return null; }
        @Override public String getProcessInstanceId() { return processInstanceId; }
        @Override public String getExecutionId() { return null; }
        @Override public String getScopeType() { return null; }
        @Override public String getScopeId() { return null; }
        @Override public String getSubScopeId() { return null; }
        @Override public String getScopeDefinitionId() { return null; }

        public Throwable getCause() { return cause; }
    }
}
