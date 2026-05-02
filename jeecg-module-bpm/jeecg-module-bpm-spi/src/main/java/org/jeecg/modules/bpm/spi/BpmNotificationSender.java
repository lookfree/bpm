package org.jeecg.modules.bpm.spi;

import java.util.Map;

public interface BpmNotificationSender {
    /**
     * @param toUserId 接收人
     * @param channel ∈ {DING, EMAIL, INTERNAL}
     * @param templateCode 模板 code（jeecg sys_message_template）
     * @param vars 模板变量
     */
    void send(Long toUserId, String channel, String templateCode, Map<String, Object> vars);
}
