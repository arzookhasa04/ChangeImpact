package com.changeimpact.executor;

import com.changeimpact.model.ComponentImpact;
import com.changeimpact.model.ImpactReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CacheManagerTest {

    @Test
    @DisplayName("Should cache impact reports and retrieve on identical fingerprint")
    public void testCacheHitAndInvalidation(@TempDir File tempDir) {
        CacheManager cache = new CacheManager(tempDir);

        Set<String> changed = Set.of("ServiceA", "ServiceB");
        String key1 = cache.computeCacheKey(changed, "graphHash1", "configHash1");

        ImpactReport report = new ImpactReport(
                changed,
                Map.of("ServiceA", new ComponentImpact("ServiceA", true, 0, List.of("ServiceA"))),
                Set.of("TestA"),
                false,
                Collections.emptyList(),
                Collections.emptyList(),
                15L
        );

        cache.putCachedImpactReport(key1, report);

        // Cache hit
        ImpactReport cached = cache.getCachedImpactReport(key1);
        assertNotNull(cached);
        assertEquals(changed, cached.getChangedComponentIds());

        // Cache miss on different graph hash
        String key2 = cache.computeCacheKey(changed, "graphHash2_MODIFIED", "configHash1");
        assertNull(cache.getCachedImpactReport(key2));

        // Clear cache
        cache.clearCache();
        assertNull(cache.getCachedImpactReport(key1));
    }
}
