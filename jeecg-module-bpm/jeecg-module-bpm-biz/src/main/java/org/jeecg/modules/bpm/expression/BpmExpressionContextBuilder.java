package org.jeecg.modules.bpm.expression;

import org.jeecg.modules.bpm.spi.BpmFormService;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.jeecg.modules.bpm.spi.BpmUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class BpmExpressionContextBuilder {

    private final BpmUserContext userCtx;
    private final BpmOrgService  orgSvc;
    private final BpmFormService formSvc;
    private final Clock clock;

    @Autowired
    public BpmExpressionContextBuilder(BpmUserContext userCtx, BpmOrgService orgSvc,
                                       BpmFormService formSvc) {
        this(userCtx, orgSvc, formSvc, Clock.systemDefaultZone());
    }

    public BpmExpressionContextBuilder(BpmUserContext userCtx, BpmOrgService orgSvc,
                                       BpmFormService formSvc, Clock clock) {
        this.userCtx = userCtx;
        this.orgSvc  = orgSvc;
        this.formSvc = formSvc;
        this.clock   = clock;
    }

    public Map<String, Object> build(String formId, String businessKey) {
        Map<String, Object> ctx = new HashMap<>();

        // form.*
        Map<String, Object> form = (formId != null && businessKey != null)
                ? new HashMap<>(formSvc.loadFormData(formId, businessKey))
                : new HashMap<>();
        ctx.put("form", form);

        // sys.*
        Instant now = clock.instant();
        ZoneId zone = clock.getZone() != null ? clock.getZone() : ZoneId.systemDefault();
        Map<String, Object> sys = new HashMap<>();
        sys.put("now",   now);
        sys.put("today", LocalDate.ofInstant(now, zone));
        ctx.put("sys", sys);

        // user.*
        Map<String, Object> user = new HashMap<>();
        user.put("id",     userCtx.currentUserId());
        user.put("deptId", userCtx.currentDeptId());
        user.put("roles",  userCtx.currentRoleCodes() != null ? userCtx.currentRoleCodes() : Collections.emptySet());
        ctx.put("user", user);

        return ctx;
    }
}
