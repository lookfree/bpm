package org.jeecg.modules.bpm.monitor.service;

import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceQuery;
import org.jeecg.modules.bpm.monitor.dto.MonitorInstanceVO;
import org.jeecg.modules.bpm.monitor.mapper.MonitorMapper;
import org.jeecg.modules.bpm.spi.BpmOrgService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitorQueryService {

    private final MonitorMapper mapper;
    private final BpmOrgService orgService;

    public MonitorQueryService(MonitorMapper mapper, BpmOrgService orgService) {
        this.mapper = mapper;
        this.orgService = orgService;
    }

    public Map<String, Object> listInstances(MonitorInstanceQuery q) {
        int pageNo = Math.max(1, q.getPageNo());
        int pageSize = Math.max(1, Math.min(200, q.getPageSize()));
        int offset = (pageNo - 1) * pageSize;

        long total = mapper.countInstances(q);
        List<MonitorInstanceVO> rows = mapper.selectInstances(q, offset, pageSize);
        for (MonitorInstanceVO row : rows) {
            if (row.getApplyUserId() != null) {
                row.setApplyUserName(orgService.findUserName(row.getApplyUserId()));
            }
            if (row.getApplyDeptId() != null) {
                row.setApplyDeptName(orgService.findDeptName(row.getApplyDeptId()));
            }
        }
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("records", rows);
        page.put("total", total);
        page.put("pageNo", pageNo);
        page.put("pageSize", pageSize);
        return page;
    }
}
