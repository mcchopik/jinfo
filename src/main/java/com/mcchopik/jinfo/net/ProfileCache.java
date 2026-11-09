package com.mcchopik.jinfo.net;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ProfileCache {

    private static final int MAX_ENTRIES = 200;
    private static final long TTL_MILLIS = 10 * 60 * 1000L;

    private record Entry(ProfileData data, long expiresAt) {
    }

    private final Map<String, Entry> entries = new LinkedHashMap<>(16, 0.75f, true);

    public synchronized Optional<ProfileData> get(String uuid) {
        Entry e = entries.get(uuid);
        if (e == null) {
            return Optional.empty();
        }
        if (e.expiresAt() < System.currentTimeMillis()) {
            entries.remove(uuid);
            return Optional.empty();
        }
        return Optional.of(e.data());
    }

    public synchronized void put(ProfileData data) {
        entries.put(data.uuid(), new Entry(data, System.currentTimeMillis() + TTL_MILLIS));
        trim();
    }

    public synchronized void remove(String uuid) {
        entries.remove(uuid);
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }

    private void trim() {
        Iterator<String> it = entries.keySet().iterator();
        while (entries.size() > MAX_ENTRIES && it.hasNext()) {
            it.next();
            it.remove();
        }
    }
}
