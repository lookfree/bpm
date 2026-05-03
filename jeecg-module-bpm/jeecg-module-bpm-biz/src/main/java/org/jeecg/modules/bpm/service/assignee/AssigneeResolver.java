package org.jeecg.modules.bpm.service.assignee;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AssigneeResolver {
    private static final Logger log = LoggerFactory.getLogger(AssigneeResolver.class);

    private final Map<String, AssigneeStrategy> strategies;
    private final ObjectMapper json;

    public AssigneeResolver(Map<String, AssigneeStrategy> strategies, ObjectMapper json) {
        Map<String, AssigneeStrategy> indexed = new HashMap<>();
        strategies.values().forEach(s -> indexed.put(s.type(), s));
        this.strategies = indexed;
        this.json = json;
    }

    public List<Long> resolve(String strategyJson, ResolveContext ctx) {
        if (strategyJson == null || strategyJson.isBlank()) return Collections.emptyList();
        Map<String, Object> doc;
        try {
            doc = json.readValue(strategyJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("malformed assignee_strategy json: {}", strategyJson, e);
            return Collections.emptyList();
        }
        String type = (String) doc.get("type");
        if (type == null) return Collections.emptyList();
        AssigneeStrategy s = strategies.get(type);
        if (s == null) {
            log.warn("no AssigneeStrategy registered for type={}", type);
            return Collections.emptyList();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) doc.getOrDefault("payload", Collections.emptyMap());
        ResolveContext enriched = ctx.toBuilder().strategyPayload(payload).build();
        List<Long> raw = s.resolve(enriched);
        return new ArrayList<>(new LinkedHashSet<>(raw));
    }
}
