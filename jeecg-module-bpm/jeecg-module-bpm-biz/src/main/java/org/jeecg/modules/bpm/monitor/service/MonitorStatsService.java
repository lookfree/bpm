package org.jeecg.modules.bpm.monitor.service;

import org.jeecg.modules.bpm.monitor.dto.StatsByDefinitionRow;
import org.jeecg.modules.bpm.monitor.dto.StatsByNodeRow;
import org.jeecg.modules.bpm.monitor.dto.StatsByApplyDeptRow;
import org.jeecg.modules.bpm.monitor.dto.StatsByApplyDeptTrendRow;
import org.jeecg.modules.bpm.monitor.dto.StatsQuery;
import org.jeecg.modules.bpm.monitor.dto.StatsResponse;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class MonitorStatsService {

    private final MonitorMapper mapper;
    private final BpmOrgService orgService;

    public MonitorStatsService(MonitorMapper mapper, BpmOrgService orgService) {
        this.mapper = mapper;
        this.orgService = orgService;
    }

    public StatsResponse compute(StatsQuery q, Set<String> scopes) {
        StatsResponse r = new StatsResponse();
        boolean all = scopes == null || scopes.isEmpty();

        if (all || scopes.contains("byDefinition")) {
            List<StatsByDefinitionRow> rows = mapper.selectStatsByDefinition(q);
            for (StatsByDefinitionRow row : rows) {
                long total = row.getInstanceCount();
                row.setCompletionRate(total == 0 ? 0.0 : (double) row.getCompletedCount() / total);
                row.setOverdueRate(total == 0 ? 0.0 : (double) row.getOverdueCount() / total);
            }
            r.setByDefinition(rows);
        }

        if (all || scopes.contains("byNode")) {
            List<StatsByNodeRow> rows = mapper.selectStatsByNode(q);
            for (StatsByNodeRow row : rows) {
                row.setOverdueRate(row.getTaskCount() == 0 ? 0.0
                        : (double) row.getOverdueCount() / row.getTaskCount());
            }
            r.setByNode(rows);
        }

        if (all || scopes.contains("byApplyDept")) {
            List<StatsByApplyDeptRow> rows = mapper.selectStatsByApplyDept(q);
            for (StatsByApplyDeptRow row : rows) {
                if (row.getApplyDeptId() != null) {
                    row.setApplyDeptName(orgService.findDeptName(row.getApplyDeptId()));
                }
            }
            r.setByApplyDept(rows);
        }

        if (all || scopes.contains("byApplyDeptOverTime")) {
            List<StatsByApplyDeptTrendRow> rows = mapper.selectStatsByApplyDeptOverTime(q);
            for (StatsByApplyDeptTrendRow row : rows) {
                if (row.getApplyDeptId() != null) {
                    row.setApplyDeptName(orgService.findDeptName(row.getApplyDeptId()));
                }
            }
            r.setByApplyDeptOverTime(rows);
        }

        return r;
    }
}
