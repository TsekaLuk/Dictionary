package com.dictionary.model;

import com.dictionary.util.WordFormUtil;
import com.dictionary.util.FuzzyMatchUtil;
import com.dictionary.util.FileIOUtil;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

public class Dictionary {
    private List<Word> words;
    private Set<String> commonWords;
    
    // 添加缓存
    private static final int MAX_CACHE_SIZE = 1000; // 最大缓存条目数
    private final Map<String, List<Word>> queryCache; // 查询结果缓存
    private final Map<String, Double> similarityCache; // 相似度计算缓存
    private final LinkedHashMap<String, Long> cacheAccessTime; // 缓存访问时间记录
    
    public Dictionary() {
        words = new ArrayList<>();
        queryCache = new ConcurrentHashMap<>();
        similarityCache = new ConcurrentHashMap<>();
        cacheAccessTime = new LinkedHashMap<String, Long>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        loadCommonWords();
    }

    public void loadFromFile(String filename) {
        try {
            List<Word> loadedWords = FileIOUtil.readDictionaryFile(filename);
            words.clear();
            words.addAll(loadedWords);
            clearCache();
        } catch (Exception e) {
            throw new RuntimeException("加载词典文件失败: " + e.getMessage(), e);
        }
    }

    private void loadCommonWords() {
        commonWords = new HashSet<>();
        String filePath = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "CommonWords.csv";

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                commonWords.add(line.trim().toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addWord(Word word) {
        words.add(word);
        clearCache(); // 清除缓存
    }

    public void addAll(List<Word> newWords) {
        words.addAll(newWords);
        clearCache(); // 清除缓存
    }

    public void removeWord(Word word) {
        words.remove(word);
        clearCache(); // 清除缓存
    }

    public void modifyWord(Word oldWord, Word newWord) {
        int index = words.indexOf(oldWord);
        if (index != -1) {
            words.set(index, newWord);
            clearCache(); // 清除缓存
        }
    }

    private void clearCache() {
        queryCache.clear();
        similarityCache.clear();
        cacheAccessTime.clear();
    }

    private String createCacheKey(String text, boolean isEnglishToChinese) {
        return text + "|" + isEnglishToChinese;
    }

    /**
     * 获取词典中的所有单词
     * @return 单词列表的副本
     */
    public List<Word> getAllWords() {
        return new ArrayList<>(words);
    }

    public Word search(String text, boolean isEnglishToChinese) {
        String cacheKey = createCacheKey(text, isEnglishToChinese);
        List<Word> cachedResults = queryCache.get(cacheKey);
        
        if (cachedResults != null && !cachedResults.isEmpty()) {
            updateCacheAccessTime(cacheKey);
            return cachedResults.get(0);
        }

        Word result = null;
        if (isEnglishToChinese) {
            // 获取所有可能的词形和拼写变体
            Set<String> wordForms = WordFormUtil.getAllWordForms(text);
            Set<String> spellingVariants = FuzzyMatchUtil.generateSpellingVariants(text);
            Set<String> allVariants = new HashSet<>();
            allVariants.addAll(wordForms);
            allVariants.addAll(spellingVariants);
            
            // 尝试匹配每一个变体
            for (String variant : allVariants) {
                result = words.stream()
                        .filter(word -> word.getWord().equalsIgnoreCase(variant))
                        .findFirst()
                        .orElse(null);
                if (result != null) break;
            }
        } else {
            result = words.stream()
                    .filter(word -> simplifyTranslation(word.getTranslation()).equalsIgnoreCase(text))
                    .findFirst()
                    .orElse(null);
        }

        if (result != null) {
            queryCache.put(cacheKey, Collections.singletonList(result));
            updateCacheAccessTime(cacheKey);
        }

        return result;
    }

    public List<Word> findSimilarWords(String text, boolean isEnglishToChinese) {
        String cacheKey = createCacheKey(text, isEnglishToChinese);
        
        // 检查缓存
        List<Word> cachedResults = queryCache.get(cacheKey);
        if (cachedResults != null) {
            updateCacheAccessTime(cacheKey);
            return new ArrayList<>(cachedResults);
        }

        List<Word> results;
        if (isEnglishToChinese) {
            // 1. 首先尝试精确匹配
            Word exactMatch = words.parallelStream()
                    .filter(word -> word.getWord().equalsIgnoreCase(text))
                    .findFirst()
                    .orElse(null);
            
            if (exactMatch != null) {
                results = Collections.singletonList(exactMatch);
            } else {
                // 2. 生成并缓存词形变化和拼写变体
                final Set<String> wordForms = WordFormUtil.getAllWordForms(text);
                final Set<String> spellingVariants = FuzzyMatchUtil.generateSpellingVariants(text);
                final Set<String> allVariants = new HashSet<>();
                allVariants.addAll(wordForms);
                allVariants.addAll(spellingVariants);
                
                // 3. 使用并行流进行相似度计算
                results = words.parallelStream()
                        .map(word -> {
                            double maxSimilarity = 0.0;
                            // 首先检查原始输入
                            String similarityKey = word.getWord() + "|" + text;
                            Double cachedSimilarity = similarityCache.get(similarityKey);
                            
                            if (cachedSimilarity != null) {
                                maxSimilarity = cachedSimilarity;
                            } else {
                                maxSimilarity = FuzzyMatchUtil.calculateSimilarity(word.getWord(), text);
                                similarityCache.put(similarityKey, maxSimilarity);
                            }
                            
                            // 检查词形变化和拼写变体
                            final String wordLower = word.getWord().toLowerCase();
                            if (allVariants.contains(wordLower)) {
                                maxSimilarity = 1.0; // 完全匹配
                            } else {
                                // 只在相似度较高时才进行详细比较
                                if (maxSimilarity > 0.5) {
                                    for (String variant : allVariants) {
                                        similarityKey = word.getWord() + "|" + variant;
                                        cachedSimilarity = similarityCache.get(similarityKey);
                                        
                                        double similarity;
                                        if (cachedSimilarity != null) {
                                            similarity = cachedSimilarity;
                                        } else {
                                            similarity = FuzzyMatchUtil.calculateSimilarity(word.getWord(), variant);
                                            similarityCache.put(similarityKey, similarity);
                                        }
                                        
                                        // 应用权重
                                        if (word.getWord().equalsIgnoreCase(variant)) {
                                            similarity *= 128;
                                        } else if (word.getWord().split("[,;]")[0].equalsIgnoreCase(variant)) {
                                            similarity *= 64;
                                        }
                                        
                                        if (commonWords.contains(wordLower)) {
                                            similarity *= 16;
                                        }
                                        
                                        if (isBasicWord(word.getWord())) {
                                            similarity *= 32;
                                        }
                                        
                                        maxSimilarity = Math.max(maxSimilarity, similarity);
                                    }
                                }
                            }
                            
                            return new AbstractMap.SimpleEntry<>(word, maxSimilarity);
                        })
                        .filter(entry -> entry.getValue() > 0.01)
                        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                        .map(AbstractMap.SimpleEntry::getKey)
                        .distinct()
                        .limit(24)
                        .collect(Collectors.toList());
            }
        } else {
            results = words.stream()
                    .map(word -> {
                        String searchText = simplifyTranslation(word.getTranslation());
                        double similarity = FuzzyMatchUtil.calculateSimilarity(searchText, text);
                        
                        // 检查是否为主要含义（第一个翻译）
                        String[] meanings = searchText.split("[,;]");
                        String primaryMeaning = meanings[0].trim();
                        
                        // 完全匹配时给予超高权重
                        if (primaryMeaning.equalsIgnoreCase(text)) {
                            similarity *= 256;  // 大幅提高主要含义完全匹配的权重
                        }
                        
                        // 检查每个翻译是否完全匹配
                        boolean hasExactMatch = false;
                        for (int i = 0; i < meanings.length; i++) {
                            String meaning = meanings[i].trim();
                            if (meaning.equalsIgnoreCase(text)) {
                                // 根据位置给予不同的权重，越靠前权重越高
                                similarity *= (128.0 / (i + 1));  // 第一个翻译128x，第二个64x，第三个42.7x...
                                hasExactMatch = true;
                                break;
                            }
                        }
                        
                        // 增加常用词的权重
                        if (commonWords.contains(word.getWord().toLowerCase())) {
                            similarity *= 16;  // 提高常用词的权重
                        }
                        
                        // 特殊处理基础词汇
                        if (isBasicWord(text)) {
                            if (primaryMeaning.equalsIgnoreCase(text)) {
                                similarity *= 512;  // 基础词汇完全匹配给予超高权重
                            } else if (hasExactMatch) {
                                similarity *= 128;  // 基础词汇其他位置匹配也给予较高权重
                            } else {
                                // 对于基础词汇的衍生义，适度降低权重
                                similarity *= 0.1;  // 降低但不要太过严厉
                            }
                        }
                        
                        // 如果是单字词，进一步调整权重
                        if (text.length() == 1 && isCJK(text.charAt(0))) {
                            if (primaryMeaning.equalsIgnoreCase(text)) {
                                similarity *= 1024;  // 单字词完全匹配给予最高权重
                            } else if (hasExactMatch) {
                                similarity *= 256;  // 单字词其他位置匹配也给予较高权重
                            } else {
                                // 单字词的衍生义权重适度降低
                                similarity *= 0.1;  // 降低但不要太过严厉
                            }
                        }
                        
                        return new AbstractMap.SimpleEntry<>(word, similarity);
                    })
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .filter(entry -> entry.getValue() > 0.001)
                    .map(AbstractMap.SimpleEntry::getKey)
                    .distinct()
                    .limit(24)
                    .collect(Collectors.toList());
        }

        // 缓存结果
        if (!results.isEmpty()) {
            queryCache.put(cacheKey, new ArrayList<>(results));
            updateCacheAccessTime(cacheKey);
        }

        return results;
    }

    private synchronized void updateCacheAccessTime(String key) {
        cacheAccessTime.put(key, System.currentTimeMillis());
        
        // 如果缓存超过最大大小，移除最旧的条目
        while (queryCache.size() > MAX_CACHE_SIZE) {
            String oldestKey = cacheAccessTime.keySet().iterator().next();
            queryCache.remove(oldestKey);
            similarityCache.remove(oldestKey);
            cacheAccessTime.remove(oldestKey);
        }
    }

    private String simplifyTranslation(String translation) {
        return translation.replaceAll("\\([^)]*\\)", "")
                         .replaceAll("\\[[^]]*\\]", "")
                         .replaceAll("\\b\\w+\\.(?=\\s)", "")
                         .trim();
    }

    // 判断是否为基础词汇
    private boolean isBasicWord(String word) {
        // 基础词汇的特征：
        // 1. 长度较短（1-4个字符）
        // 2. 在常用词列表中
        // 3. 是常见的单字词（中文）
        // 4. 是基础的双字词（中文）
        
        // 检查是否为常见的中文词
        boolean isCJKWord = true;
        for (int i = 0; i < word.length(); i++) {
            if (!isCJK(word.charAt(i))) {
                isCJKWord = false;
                break;
            }
        }
        
        // 中文词的判断
        if (isCJKWord) {
            if (word.length() <= 2) {  // 单字词和双字词都视为基础词汇
                return true;
            }
        }
        // 英文词的判断
        else if (word.length() <= 4 && commonWords.contains(word.toLowerCase())) {
            return true;
        }
        
        return false;
    }

    // 判断是否为CJK字符（中日韩统一表意文字）
    private boolean isCJK(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B;
    }
}


