package org.jeecg.modules.bpm.definition;

import org.jeecg.modules.bpm.definition.DefinitionLifecycleService.State;
import org.jeecg.modules.bpm.definition.exception.IllegalStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefinitionLifecycleServiceTest {

    private DefinitionLifecycleService svc;

    @BeforeEach
    void setUp() {
        svc = new DefinitionLifecycleService();
    }

    @Test
    void draftToTesting_allowed() {
        assertThatCode(() -> svc.assertAllowed(State.DRAFT, State.TESTING))
                .doesNotThrowAnyException();
    }

    @Test
    void testingToPublished_allowed() {
        assertThatCode(() -> svc.assertAllowed(State.TESTING, State.PUBLISHED))
                .doesNotThrowAnyException();
    }

    @Test
    void testingToDraft_allowed() {
        assertThatCode(() -> svc.assertAllowed(State.TESTING, State.DRAFT))
                .doesNotThrowAnyException();
    }

    @Test
    void publishedToArchived_allowed() {
        assertThatCode(() -> svc.assertAllowed(State.PUBLISHED, State.ARCHIVED))
                .doesNotThrowAnyException();
    }

    @Test
    void draftToPublished_forbidden() {
        assertThatThrownBy(() -> svc.assertAllowed(State.DRAFT, State.PUBLISHED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void archivedToDraft_forbidden() {
        assertThatThrownBy(() -> svc.assertAllowed(State.ARCHIVED, State.DRAFT))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void archivedToPublished_forbidden() {
        assertThatThrownBy(() -> svc.assertAllowed(State.ARCHIVED, State.PUBLISHED))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void publishedToTesting_forbidden() {
        assertThatThrownBy(() -> svc.assertAllowed(State.PUBLISHED, State.TESTING))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void publishedToDraft_forbidden() {
        assertThatThrownBy(() -> svc.assertAllowed(State.PUBLISHED, State.DRAFT))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void draftToDraft_selfLoop_forbidden() {
        assertThatThrownBy(() -> svc.assertAllowed(State.DRAFT, State.DRAFT))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    void nullFrom_throwsIllegalArgument() {
        assertThatThrownBy(() -> svc.assertAllowed(null, State.DRAFT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullTo_throwsIllegalArgument() {
        assertThatThrownBy(() -> svc.assertAllowed(State.DRAFT, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
