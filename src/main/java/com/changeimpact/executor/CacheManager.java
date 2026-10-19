package com.changeimpact.executor;

import com.changeimpact.model.ImpactReport;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Manages caching of change-impact analysis and test selections.
 * Automatically invalidates entries when files, graph structure, or configuration hashes change.
 */
public class CacheManager {

    private final File cacheDir;
    private final ObjectMapper mapper;

    public CacheManager(File cacheDir) {
        this.cacheDir = cacheDir != null ? cacheDir : new File(".changeimpact_cache");
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
        }
        this.mapper = new ObjectMapper();
    }

    public CacheManager() {
        this(new File(".changeimpact_cache"));
    }

    /**
     * Compute a deterministic cache key from changed components and graph fingerprint.
     */
    public String computeCacheKey(Set<String> changedComponents, String graphFingerprint, String configFingerprint) {
        List<String> sortedComponents = new ArrayList<>(changedComponents);
        Collections.sort(sortedComponents);

        String combined = String.join(";", sortedComponents) + "|" +
                (graphFingerprint != null ? graphFingerprint : "") + "|" +
                (configFingerprint != null ? configFingerprint : "");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(combined.hashCode());
        }
    }

    /**
     * Retrieve cached impact report if valid, or null if miss/invalid.
     */
    public ImpactReport getCachedImpactReport(String cacheKey) {
        File cacheFile = new File(cacheDir, "impact_" + cacheKey + ".json");
        if (!cacheFile.exists()) {
            return null;
        }
        try {
            return mapper.readValue(cacheFile, ImpactReport.class);
        } catch (IOException e) {
            // Cache corrupted or invalid format; purge it
            cacheFile.delete();
            return null;
        }
    }

    /**
     * Store impact report in cache.
     */
    public void putCachedImpactReport(String cacheKey, ImpactReport report) {
        File cacheFile = new File(cacheDir, "impact_" + cacheKey + ".json");
        try {
            mapper.writeValue(cacheFile, report);
        } catch (IOException e) {
            System.err.println("Warning: Failed to write to cache: " + e.getMessage());
        }
    }

    /**
     * Invalidate and clear all cached entries.
     */
    public void clearCache() {
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".json")) {
                    f.delete();
                }
            }
        }
    }

    public File getCacheDir() {
        return cacheDir;
    }
}
