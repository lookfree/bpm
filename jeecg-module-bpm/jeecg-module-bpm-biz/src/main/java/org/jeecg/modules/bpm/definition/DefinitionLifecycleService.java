package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;

@Service
public class DefinitionLifecycleService {

    public enum State {
        DRAFT, TESTING, PUBLISHED, ARCHIVED
    }

    private static final Map<State, EnumSet<State>> ALLOWED;

    static {
        ALLOWED = new java.util.HashMap<>();
        ALLOWED.put(State.DRAFT, EnumSet.of(State.TESTING));
        ALLOWED.put(State.TESTING, EnumSet.of(State.PUBLISHED, State.DRAFT));
        ALLOWED.put(State.PUBLISHED, EnumSet.of(State.ARCHIVED));
        ALLOWED.put(State.ARCHIVED, EnumSet.noneOf(State.class));
    }

    public void assertAllowed(State from, State to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        EnumSet<State> targets = ALLOWED.get(from);
        if (targets == null || !targets.contains(to)) {
            throw new IllegalStateTransitionException(from.name(), to.name());
        }
    }

    public boolean isAllowed(State from, State to) {
        if (from == null || to == null) {
            return false;
        }
        EnumSet<State> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }
}
