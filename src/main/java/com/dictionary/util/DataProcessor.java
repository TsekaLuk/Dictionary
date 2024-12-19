package com.dictionary.util;

import com.dictionary.model.Word;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.function.Consumer;

public class DataProcessor {
    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService executorService = 
        Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    
    // Process large word lists in batches
    public static List<Word> processBatch(List<Word> words, int batchSize) {
        List<Word> result = new ArrayList<>();
        int totalSize = words.size();
        
        for (int i = 0; i < totalSize; i += batchSize) {
            int endIndex = Math.min(i + batchSize, totalSize);
            List<Word> batch = words.subList(i, endIndex);
            result.addAll(processSingleBatch(batch));
        }
        
        return result;
    }
    
    // Process a single batch of words
    private static List<Word> processSingleBatch(List<Word> batch) {
        return batch.stream()
            .filter(Objects::nonNull)
            .filter(word -> word.getWord() != null && !word.getWord().isEmpty())
            .collect(Collectors.toList());
    }
    
    // Parallel search implementation
    public static List<Word> parallelSearch(List<Word> words, String query, boolean isEnglishToChineseMode) {
        if (words.isEmpty() || query == null || query.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Split work among available processors
        int partitionSize = words.size() / THREAD_POOL_SIZE;
        List<Future<List<Word>>> futures = new ArrayList<>();
        
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            int startIndex = i * partitionSize;
            int endIndex = (i == THREAD_POOL_SIZE - 1) ? words.size() : (i + 1) * partitionSize;
            
            List<Word> partition = words.subList(startIndex, endIndex);
            futures.add(executorService.submit(() -> 
                searchPartition(partition, query, isEnglishToChineseMode)));
        }
        
        // Collect results
        List<Word> results = new ArrayList<>();
        for (Future<List<Word>> future : futures) {
            try {
                results.addAll(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return results;
    }
    
    // Search within a partition
    private static List<Word> searchPartition(List<Word> partition, String query, boolean isEnglishToChineseMode) {
        return partition.stream()
            .filter(word -> matchesQuery(word, query, isEnglishToChineseMode))
            .collect(Collectors.toList());
    }
    
    // Match word against query
    private static boolean matchesQuery(Word word, String query, boolean isEnglishToChineseMode) {
        if (isEnglishToChineseMode) {
            return word.getWord().toLowerCase().contains(query.toLowerCase());
        } else {
            return word.getTranslation().contains(query);
        }
    }
    
    // Import words in batches
    public static void importWords(List<Word> words, Consumer<List<Word>> batchConsumer) {
        int totalSize = words.size();
        
        for (int i = 0; i < totalSize; i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, totalSize);
            List<Word> batch = words.subList(i, endIndex);
            batchConsumer.accept(processSingleBatch(batch));
        }
    }
    
    // Shutdown the executor service
    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 