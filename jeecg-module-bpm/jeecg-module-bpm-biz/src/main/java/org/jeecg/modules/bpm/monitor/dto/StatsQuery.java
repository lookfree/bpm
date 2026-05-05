package org.jeecg.modules.bpm.monitor.dto;

import java.time.LocalDateTime;

public class StatsQuery {
    private LocalDateTime startTimeFrom;
    private LocalDateTime startTimeTo;
    private String defKey;

    public LocalDateTime getStartTimeFrom() { return startTimeFrom; }
    public void setStartTimeFrom(LocalDateTime startTimeFrom) { this.startTimeFrom = startTimeFrom; }
    public LocalDateTime getStartTimeTo() { return startTimeTo; }
    public void setStartTimeTo(LocalDateTime startTimeTo) { this.startTimeTo = startTimeTo; }
    public String getDefKey() { return defKey; }
    public void setDefKey(String defKey) { this.defKey = defKey; }
}
