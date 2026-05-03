package org.jeecg.modules.bpm.expression;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class BpmExpressionCacheKey {
    private final String defKey;
    private final int version;
    private final String expression;
    private final String exprHash;

    private BpmExpressionCacheKey(String defKey, int version, String expression) {
        this.defKey = Objects.requireNonNull(defKey);
        this.version = version;
        this.expression = Objects.requireNonNull(expression);
        this.exprHash = sha256(expression);
    }

    public static BpmExpressionCacheKey of(String defKey, int version, String expression) {
        return new BpmExpressionCacheKey(defKey, version, expression);
    }

    public String getDefKey()     { return defKey; }
    public int    getVersion()    { return version; }
    public String getExpression() { return expression; }
    public String getExprHash()   { return exprHash; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BpmExpressionCacheKey)) return false;
        BpmExpressionCacheKey that = (BpmExpressionCacheKey) o;
        return version == that.version && defKey.equals(that.defKey) && exprHash.equals(that.exprHash);
    }
    @Override public int hashCode() { return Objects.hash(defKey, version, exprHash); }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
