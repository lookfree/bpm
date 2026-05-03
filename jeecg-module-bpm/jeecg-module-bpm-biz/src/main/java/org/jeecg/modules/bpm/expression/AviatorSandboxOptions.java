package org.jeecg.modules.bpm.expression;

import com.googlecode.aviator.Feature;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class AviatorSandboxOptions {
    public static final Set<Feature> ALLOWED_FEATURES = Collections.unmodifiableSet(
            EnumSet.of(
                    Feature.Assignment,
                    Feature.Return,
                    Feature.If,
                    Feature.ForLoop,
                    Feature.Let,
                    Feature.LexicalScope,
                    Feature.StringInterpolation
            ));

    public static final Set<String> BLOCKED_FUNCTIONS = Collections.unmodifiableSet(new java.util.HashSet<>(
            java.util.Arrays.asList(
                    "Class.forName", "System.exit", "Runtime.getRuntime",
                    "ProcessBuilder", "java.io.File", "FileInputStream",
                    "FileOutputStream", "Socket", "URL"
            )));

    private AviatorSandboxOptions() {}
}
