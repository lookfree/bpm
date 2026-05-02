package org.jeecg.modules.bpm.adapter.jeecg;

import org.jeecg.modules.bpm.spi.BpmNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NoopBpmNotificationSender implements BpmNotificationSender {
    private static final Logger LOG = LoggerFactory.getLogger(NoopBpmNotificationSender.class);

    @Override
    public void send(Long toUserId, String channel, String templateCode, Map<String, Object> vars) {
        LOG.info("[bpm-noop-notify] to={} channel={} template={} vars={}", toUserId, channel, templateCode, vars);
    }
}
