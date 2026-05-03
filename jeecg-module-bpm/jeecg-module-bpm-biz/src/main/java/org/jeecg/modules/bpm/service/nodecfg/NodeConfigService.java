package org.jeecg.modules.bpm.service.nodecfg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.bpm.domain.entity.NodeConfig;
import org.jeecg.modules.bpm.mapper.NodeConfigMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class NodeConfigService {

    private final NodeConfigMapper mapper;

    public NodeConfigService(NodeConfigMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<NodeConfig> findByActDefAndNode(String actDefId, String nodeId) {
        NodeConfig result = mapper.selectOne(
                new LambdaQueryWrapper<NodeConfig>()
                        .eq(NodeConfig::getDefId, actDefId)
                        .eq(NodeConfig::getNodeId, nodeId)
        );
        return Optional.ofNullable(result);
    }

    public NodeConfig save(NodeConfig cfg) {
        if (cfg.getId() == null) {
            mapper.insert(cfg);
        } else {
            mapper.updateById(cfg);
        }
        return cfg;
    }
}
