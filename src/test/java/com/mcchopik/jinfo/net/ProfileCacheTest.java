package com.mcchopik.jinfo.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProfileCacheTest {

    private static ProfileData profile(String uuid) {
        return new ProfileData(uuid, "data", 123L, true);
    }

    @Test
    void returnsCachedValue() {
        ProfileCache cache = new ProfileCache();
        cache.put(profile("a"));
        assertTrue(cache.get("a").isPresent());
        assertEquals("data", cache.get("a").orElseThrow().data());
    }

    @Test
    void emptyForUnknown() {
        ProfileCache cache = new ProfileCache();
        assertTrue(cache.get("nope").isEmpty());
    }

    @Test
    void evictsBeyondLimit() {
        ProfileCache cache = new ProfileCache();
        for (int i = 0; i < 250; i++) {
            cache.put(profile("u" + i));
        }
        assertEquals(200, cache.size());
        assertTrue(cache.get("u0").isEmpty());
        assertTrue(cache.get("u249").isPresent());
    }
}