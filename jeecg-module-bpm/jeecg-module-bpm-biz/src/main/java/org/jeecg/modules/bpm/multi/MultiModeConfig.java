package org.jeecg.modules.bpm.multi;

public class MultiModeConfig {
    private final String mode;

    public MultiModeConfig(String mode) {
        if (!"PARALLEL".equals(mode) && !"ANY".equals(mode) && !"SEQUENCE".equals(mode)) {
            throw new IllegalArgumentException("invalid multi mode: " + mode);
        }
        this.mode = mode;
    }

    public String mode() { return mode; }
}
