package org.jeecg.modules.bpm.expression;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.Feature;
import com.googlecode.aviator.Options;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class BpmExpressionEvaluator {

    private final long timeoutMillis;
    private final AviatorEvaluatorInstance aviator;
    private final Map<BpmExpressionCacheKey, Expression> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "bpm-aviator");
        t.setDaemon(true);
        return t;
    });

    public BpmExpressionEvaluator() { this(200L); }

    public BpmExpressionEvaluator(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
        this.aviator = AviatorEvaluator.newInstance();
        this.aviator.setOption(Options.FEATURE_SET, EnumSet.copyOf(AviatorSandboxOptions.ALLOWED_FEATURES));
        this.aviator.disableFeature(Feature.Module);
        this.aviator.disableFeature(Feature.NewInstance);
        this.aviator.disableFeature(Feature.StaticMethods);
        this.aviator.disableFeature(Feature.StaticFields);
        this.aviator.disableFeature(Feature.Lambda);
        // Block all class access — prevents System.exit, Runtime.getRuntime, reflection, etc.
        this.aviator.setOption(Options.ALLOWED_CLASS_SET, Collections.emptySet());
        for (String fn : AviatorSandboxOptions.BLOCKED_FUNCTIONS) {
            try { this.aviator.removeFunction(fn); } catch (Exception ignored) {}
        }
    }

    public Object evaluate(BpmExpressionCacheKey key, Map<String, Object> env) {
        Expression expr;
        try {
            expr = cache.computeIfAbsent(key, k -> aviator.compile(k.getExpression(), true));
        } catch (Exception e) {
            throw new BpmExpressionException("compile failed: " + e.getMessage(), e);
        }
        Future<Object> future = executor.submit(() -> expr.execute(env));
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            throw new BpmExpressionException("expression timeout (>" + timeoutMillis + "ms)");
        } catch (ExecutionException ee) {
            throw new BpmExpressionException("evaluate failed: " + ee.getCause().getMessage(), ee.getCause());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new BpmExpressionException("evaluate interrupted", ie);
        }
    }

    public int cacheSize() { return cache.size(); }
}
