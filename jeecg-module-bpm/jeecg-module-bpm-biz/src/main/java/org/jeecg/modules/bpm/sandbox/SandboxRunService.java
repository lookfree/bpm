package org.jeecg.modules.bpm.sandbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class SandboxRunService {

    private final SandboxRunMapper mapper;

    public SandboxRunService(SandboxRunMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public Long start(String defId, Long runnerId) {
        SandboxRun r = new SandboxRun();
        r.setDefIdDraft(defId);
        r.setRunnerId(runnerId);
        r.setResult("RUNNING");
        r.setLog("");
        r.setStartTime(new Date());
        mapper.insert(r);
        return r.getId();
    }

    @Transactional
    public void appendLog(Long runId, String line) {
        SandboxRun r = mapper.selectById(runId);
        if (r == null) throw new IllegalArgumentException("run not found: " + runId);
        if (!"RUNNING".equals(r.getResult())) {
            throw new IllegalStateException("run already finished: " + runId);
        }
        String prev = r.getLog() == null ? "" : r.getLog();
        r.setLog(prev + "[" + new Date() + "] " + line + "\n");
        mapper.updateById(r);
    }

    @Transactional
    public void finish(Long runId, SandboxResult result) {
        SandboxRun r = mapper.selectById(runId);
        if (r == null) throw new IllegalArgumentException("run not found: " + runId);
        r.setResult(result.name());
        r.setEndTime(new Date());
        mapper.updateById(r);
    }

    public SandboxRun findById(Long runId) {
        return mapper.selectById(runId);
    }
}
