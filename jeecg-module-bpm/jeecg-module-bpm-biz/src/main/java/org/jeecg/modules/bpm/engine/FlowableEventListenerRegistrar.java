package org.jeecg.modules.bpm.engine;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.flowable.task.service.delegate.DelegateTask;
import org.jeecg.modules.bpm.flow.NoMatchingFlowHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class FlowableEventListenerRegistrar {

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> bpmTaskListenerRegistrar(
            AssigneeAssignmentTaskListener assigneeListener,
            NoMatchingFlowHandler noMatchingFlowHandler) {
        return cfg -> {
            FlowableEventListener wrapper = new FlowableEventListener() {
                @Override
                public void onEvent(FlowableEvent event) {
                    if (event instanceof FlowableEngineEntityEvent) {
                        Object entity = ((FlowableEngineEntityEvent) event).getEntity();
                        if (entity instanceof DelegateTask) {
                            assigneeListener.notify((DelegateTask) entity);
                        }
                    }
                }

                @Override
                public boolean isFailOnException() {
                    return false;
                }

                @Override
                public boolean isFireOnTransactionLifecycleEvent() {
                    return false;
                }

                @Override
                public String getOnTransaction() {
                    return null;
                }
            };

            Map<String, List<FlowableEventListener>> map = new HashMap<>();
            map.put(FlowableEngineEventType.TASK_CREATED.name(), Collections.singletonList(wrapper));
            map.put(FlowableEngineEventType.JOB_EXECUTION_FAILURE.name(),
                    Collections.singletonList(noMatchingFlowHandler));
            cfg.setTypedEventListeners(map);
        };
    }
}
