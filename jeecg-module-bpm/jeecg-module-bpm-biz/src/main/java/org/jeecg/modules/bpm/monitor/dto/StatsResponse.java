package org.jeecg.modules.bpm.monitor.dto;

import java.util.ArrayList;
import java.util.List;

public class StatsResponse {
    private List<StatsByDefinitionRow> byDefinition = new ArrayList<>();
    private List<StatsByNodeRow> byNode = new ArrayList<>();
    private List<StatsByApplyDeptRow> byApplyDept = new ArrayList<>();
    private List<StatsByApplyDeptTrendRow> byApplyDeptOverTime = new ArrayList<>();

    public List<StatsByDefinitionRow> getByDefinition() { return byDefinition; }
    public void setByDefinition(List<StatsByDefinitionRow> byDefinition) { this.byDefinition = byDefinition; }
    public List<StatsByNodeRow> getByNode() { return byNode; }
    public void setByNode(List<StatsByNodeRow> byNode) { this.byNode = byNode; }
    public List<StatsByApplyDeptRow> getByApplyDept() { return byApplyDept; }
    public void setByApplyDept(List<StatsByApplyDeptRow> byApplyDept) { this.byApplyDept = byApplyDept; }
    public List<StatsByApplyDeptTrendRow> getByApplyDeptOverTime() { return byApplyDeptOverTime; }
    public void setByApplyDeptOverTime(List<StatsByApplyDeptTrendRow> byApplyDeptOverTime) { this.byApplyDeptOverTime = byApplyDeptOverTime; }
}
