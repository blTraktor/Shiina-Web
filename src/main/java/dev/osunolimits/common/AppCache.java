package dev.osunolimits.common;

import com.github.benmanes.caffeine.cache.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppCache {
    private static final Logger log = LoggerFactory.getLogger(Cache.class);
    private static final String FILE_PATH = "data/cache.bin";
    private static final long SAVE_INTERVAL_MS = 60_000; // 1 minute

    private final Cache<String, CacheEntry> cache;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public AppCache() {
        new File("data").mkdirs();

        cache = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, CacheEntry>() {
                    @Override
                    public long expireAfterCreate(String key, CacheEntry entry, long currentTime) {
                        return entry.expireAt == 0 ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.toNanos(entry.expireAt - System.currentTimeMillis());
                    }
                    @Override public long expireAfterUpdate(String key, CacheEntry entry, long currentTime, long currentDuration) { return expireAfterCreate(key, entry, currentTime); }
                    @Override public long expireAfterRead(String key, CacheEntry entry, long currentTime, long currentDuration) { return currentDuration; }
                })
                .build();

        loadFromDisk();
        
        // Start periodic save task
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AppCache-Saver");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::saveIfDirty, SAVE_INTERVAL_MS, SAVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void markDirty() {
        dirty.set(true);
    }
    
    private void saveIfDirty() {
        if (dirty.compareAndSet(true, false)) {
            saveToDisk();
        }
    }

    /** Set key with optional TTL (in seconds). */
    public void set(String key, Object value, long ttlSeconds) {
        long expireAt = ttlSeconds > 0 ? System.currentTimeMillis() + ttlSeconds * 1000 : 0;
        cache.put(key, new CacheEntry(value, expireAt));
        markDirty();
    }

    public void set(String key, Object value) {
        set(key, value, 0);
    }

    /** Get value if not expired. */
    public String get(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            cache.invalidate(key);
            return null;
        }
        return (String) entry.value;
    }

    /** Delete a key. */
    public void del(String key) {
        cache.invalidate(key);
        markDirty();
    }

    /** Push element to the head of a list. */
    @SuppressWarnings("unchecked")
    public void lpush(String key, Object value) {
        CacheEntry entry = cache.getIfPresent(key);
        List<Object> list;
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            list = new ArrayList<>();
        } else {
            Object existingValue = entry.value;
            if (existingValue instanceof List) {
                list = new ArrayList<>((List<Object>) existingValue);
            } else {
                list = new ArrayList<>();
            }
        }
        
        list.add(0, value);
        cache.put(key, new CacheEntry(list, entry != null ? entry.expireAt : 0));
        markDirty();
    }

    /** Set list element at index. */
    @SuppressWarnings("unchecked")
    public void lset(String key, int index, Object value) {
        CacheEntry entry = cache.getIfPresent(key);
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            throw new IllegalArgumentException("Key does not exist or has expired");
        }
        
        Object existingValue = entry.value;
        if (!(existingValue instanceof List)) {
            throw new IllegalArgumentException("Key does not contain a list");
        }
        
        List<Object> list = new ArrayList<>((List<Object>) existingValue);
        if (index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("Index out of range: " + index);
        }
        
        list.set(index, value);
        cache.put(key, new CacheEntry(list, entry.expireAt));
        markDirty();
    }

    /** Get a range of elements from a list. Returns empty list if key doesn't exist. */
    @SuppressWarnings("unchecked")
    public List<Object> lrange(String key, int start, int stop) {
        CacheEntry entry = cache.getIfPresent(key);
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            return new ArrayList<>();
        }
        
        Object existingValue = entry.value;
        if (!(existingValue instanceof List)) {
            return new ArrayList<>();
        }
        
        List<Object> list = (List<Object>) existingValue;
        int size = list.size();
        
        // Handle negative indices (Redis-style: -1 means last element)
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;
        
        // Clamp to valid range
        start = Math.max(0, Math.min(start, size - 1));
        stop = Math.max(0, Math.min(stop, size - 1));
        
        if (start > stop || size == 0) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(list.subList(start, stop + 1));
    }

    /** Add member to sorted set with score. */
    @SuppressWarnings("unchecked")
    public void zadd(String key, double score, Object member) {
        CacheEntry entry = cache.getIfPresent(key);
        TreeMap<Double, List<Object>> sortedSet;
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            sortedSet = new TreeMap<>();
        } else {
            Object existingValue = entry.value;
            if (existingValue instanceof TreeMap) {
                sortedSet = new TreeMap<>((TreeMap<Double, List<Object>>) existingValue);
            } else {
                sortedSet = new TreeMap<>();
            }
        }
        
        // Remove member from all score lists if it exists
        sortedSet.values().forEach(list -> list.remove(member));
        
        // Add member to the new score list
        sortedSet.computeIfAbsent(score, k -> new ArrayList<>()).add(member);
        
        cache.put(key, new CacheEntry(sortedSet, entry != null ? entry.expireAt : 0));
        markDirty();
    }

    /** Remove members from sorted set by score range. */
    @SuppressWarnings("unchecked")
    public void zremrangeByScore(String key, String min, String max) {
        CacheEntry entry = cache.getIfPresent(key);
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            return;
        }
        
        Object existingValue = entry.value;
        if (!(existingValue instanceof TreeMap)) {
            return;
        }
        
        TreeMap<Double, List<Object>> sortedSet = new TreeMap<>((TreeMap<Double, List<Object>>) existingValue);
        
        double minScore = min.equals("-inf") ? Double.NEGATIVE_INFINITY : Double.parseDouble(min);
        double maxScore = max.equals("+inf") ? Double.POSITIVE_INFINITY : Double.parseDouble(max);
        
        // Remove all entries within the score range
        sortedSet.entrySet().removeIf(e -> e.getKey() >= minScore && e.getKey() <= maxScore);
        
        cache.put(key, new CacheEntry(sortedSet, entry.expireAt));
        markDirty();
    }

    /** Get cardinality (number of members) in sorted set. */
    @SuppressWarnings("unchecked")
    public long zcard(String key) {
        CacheEntry entry = cache.getIfPresent(key);
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            return 0;
        }
        
        Object existingValue = entry.value;
        if (!(existingValue instanceof TreeMap)) {
            return 0;
        }
        
        TreeMap<Double, List<Object>> sortedSet = (TreeMap<Double, List<Object>>) existingValue;
        return sortedSet.values().stream().mapToInt(List::size).sum();
    }

    /** Remove members from sorted set by rank range. */
    @SuppressWarnings("unchecked")
    public void zremrangeByRank(String key, long start, long stop) {
        CacheEntry entry = cache.getIfPresent(key);
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            return;
        }
        
        Object existingValue = entry.value;
        if (!(existingValue instanceof TreeMap)) {
            return;
        }
        
        TreeMap<Double, List<Object>> sortedSet = new TreeMap<>((TreeMap<Double, List<Object>>) existingValue);
        
        // Flatten to list with indices
        List<Map.Entry<Double, Object>> flatList = new ArrayList<>();
        for (Map.Entry<Double, List<Object>> e : sortedSet.entrySet()) {
            for (Object member : e.getValue()) {
                flatList.add(new AbstractMap.SimpleEntry<>(e.getKey(), member));
            }
        }
        
        int size = flatList.size();
        if (size == 0) return;
        
        // Handle negative indices
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;
        
        start = Math.max(0, start);
        stop = Math.min(size - 1, stop);
        
        if (start > stop) return;
        
        // Collect members to remove
        Set<Object> toRemove = new HashSet<>();
        for (long i = start; i <= stop && i < flatList.size(); i++) {
            toRemove.add(flatList.get((int) i).getValue());
        }
        
        // Remove members from sorted set
        sortedSet.values().forEach(list -> list.removeAll(toRemove));
        sortedSet.entrySet().removeIf(e -> e.getValue().isEmpty());
        
        cache.put(key, new CacheEntry(sortedSet, entry.expireAt));
        markDirty();
    }

    /** Get range of members from sorted set by rank. */
    @SuppressWarnings("unchecked")
    public List<String> zrange(String key, long start, long stop) {
        CacheEntry entry = cache.getIfPresent(key);
        
        if (entry == null || (entry.expireAt > 0 && entry.expireAt < System.currentTimeMillis())) {
            return new ArrayList<>();
        }
        
        Object existingValue = entry.value;
        if (!(existingValue instanceof TreeMap)) {
            return new ArrayList<>();
        }
        
        TreeMap<Double, List<Object>> sortedSet = (TreeMap<Double, List<Object>>) existingValue;
        
        // Flatten to list (sorted by score)
        List<Object> flatList = new ArrayList<>();
        for (List<Object> members : sortedSet.values()) {
            flatList.addAll(members);
        }
        
        int size = flatList.size();
        if (size == 0) return new ArrayList<>();
        
        // Handle negative indices
        if (start < 0) start = size + start;
        if (stop < 0) stop = size + stop;
        
        start = Math.max(0, start);
        stop = Math.min(size - 1, stop);
        
        if (start > stop) return new ArrayList<>();
        
        return flatList.subList((int) start, (int) stop + 1).stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toList());
    }

    /** Save cache content to disk using binary serialization. */
    private synchronized void saveToDisk() {
        Map<String, CacheEntry> map = cache.asMap();
        File tempFile = new File(FILE_PATH + ".tmp");
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            oos.writeObject(new HashMap<>(map));
            oos.flush();
            
            // Atomic rename
            Files.move(tempFile.toPath(), Paths.get(FILE_PATH), 
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            log.debug("Saved {} keys to cache", map.size());
        } catch (IOException e) {
            log.error("Failed to save cache: {}", e.getMessage());
            tempFile.delete();
        }
    }

    /** Load cache from binary file. */
    @SuppressWarnings("unchecked")
    private void loadFromDisk() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            log.info("PersistentCache: No cache file found, starting empty.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            Map<String, CacheEntry> map = (Map<String, CacheEntry>) ois.readObject();
            if (map != null) {
                for (Map.Entry<String, CacheEntry> e : map.entrySet()) {
                    if (e.getValue().expireAt == 0 || e.getValue().expireAt > System.currentTimeMillis()) {
                        cache.put(e.getKey(), e.getValue());
                    }
                }
                log.info("PersistentCache: Loaded {} keys from {}", map.size(), FILE_PATH);
            }
        } catch (Exception e) {
            log.error("Failed to load cache: {}", e.getMessage());
        }
    }

    /** Close and persist before shutdown. */
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Force final save
        saveToDisk();
        log.info("PersistentCache: Saved {} keys to {}", cache.asMap().size(), FILE_PATH);
    }

    /** Inner entry wrapper with expiration info. */
    private static class CacheEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        Object value;
        long expireAt; // 0 = never expires
        CacheEntry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
