package com.dictionary.util;

import com.dictionary.model.Word;
import com.dictionary.model.DictionaryMetadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CacheManager {
    private static final CacheManager instance = new CacheManager();
    
    // 缓存容器
    private final Map<String, CacheEntry<List<Word>>> wordListCache;
    private final Map<Long, CacheEntry<DictionaryMetadata>> metadataCache;
    
    // 缓存配置
    private static final long CACHE_DURATION = 5 * 60 * 1000; // 5分钟
    private static final int MAX_CACHE_SIZE = 100;
    
    // 清理调度器
    private final ScheduledExecutorService cleanupExecutor;
    
    private CacheManager() {
        wordListCache = new ConcurrentHashMap<>();
        metadataCache = new ConcurrentHashMap<>();
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // 启动定期清理任务
        cleanupExecutor.scheduleAtFixedRate(
            this::cleanupCache,
            CACHE_DURATION,
            CACHE_DURATION,
            TimeUnit.MILLISECONDS
        );
    }
    
    public static CacheManager getInstance() {
        return instance;
    }
    
    // 缓存词典列表
    public void cacheWordList(String key, List<Word> words) {
        if (wordListCache.size() >= MAX_CACHE_SIZE) {
            cleanupCache();
        }
        wordListCache.put(key, new CacheEntry<>(words));
    }
    
    // 获取缓存的词典列表
    public List<Word> getWordList(String key) {
        CacheEntry<List<Word>> entry = wordListCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return null;
    }
    
    // 缓存词典元数据
    public void cacheMetadata(Long id, DictionaryMetadata metadata) {
        if (metadataCache.size() >= MAX_CACHE_SIZE) {
            cleanupCache();
        }
        metadataCache.put(id, new CacheEntry<>(metadata));
    }
    
    // 获取缓存的词典元数据
    public DictionaryMetadata getMetadata(Long id) {
        CacheEntry<DictionaryMetadata> entry = metadataCache.get(id);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }
        return null;
    }
    
    // 清除特定缓存
    public void invalidateWordList(String key) {
        wordListCache.remove(key);
    }
    
    public void invalidateMetadata(Long id) {
        metadataCache.remove(id);
    }
    
    // 清除所有缓存
    public void clearAll() {
        wordListCache.clear();
        metadataCache.clear();
    }
    
    // 清理过期缓存
    private void cleanupCache() {
        long now = System.currentTimeMillis();
        
        wordListCache.entrySet().removeIf(entry ->
            entry.getValue().isExpired(now)
        );
        
        metadataCache.entrySet().removeIf(entry ->
            entry.getValue().isExpired(now)
        );
    }
    
    // 关闭缓存管理器
    public void shutdown() {
        cleanupExecutor.shutdown();
        clearAll();
    }
    
    // 缓存条目内部类
    private static class CacheEntry<T> {
        private final T value;
        private final long timestamp;
        
        public CacheEntry(T value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        public T getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }
        
        public boolean isExpired(long now) {
            return now - timestamp > CACHE_DURATION;
        }
    }
} 