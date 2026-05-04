package org.jeecg.modules.bpm.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.bpm.domain.entity.InstanceMeta;
import org.jeecg.modules.bpm.mapper.InstanceMetaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BpmNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(BpmNotificationDispatcher.class);

    private final InstanceMetaMapper instanceMetaMapper;

    public BpmNotificationDispatcher(InstanceMetaMapper instanceMetaMapper) {
        this.instanceMetaMapper = instanceMetaMapper;
    }

    /**
     * Dispatches a notification for a BPM instance. Silently no-ops for SANDBOX instances.
     */
    public void notify(String actInstId, String channel, String templateCode, Map<String, Object> vars) {
        InstanceMeta meta = instanceMetaMapper.selectOne(
            new LambdaQueryWrapper<InstanceMeta>()
                .eq(InstanceMeta::getActInstId, actInstId)
        );
        if (meta != null && "SANDBOX".equals(meta.getState())) {
            log.info("[BPM] Notification suppressed for sandbox instance {}", actInstId);
            return;
        }
        // PROD path — extend here when a real notification sender is integrated
        log.info("[BPM] Would notify instance={} channel={} template={}", actInstId, channel, templateCode);
    }
}
