package com.dictionary.util;

import java.util.*;

public class FuzzyMatchUtil {
    // 常见的拼写错误模式
    private static final Map<String, String> COMMON_MISSPELLINGS = new HashMap<>();
    private static final Set<String> VOWELS = new HashSet<>(Arrays.asList("a", "e", "i", "o", "u"));
    private static final int MAX_VARIANTS = 200; // 限制变体数量
    
    // 键盘邻居映射
    private static final Map<Character, Set<Character>> KEYBOARD_NEIGHBORS = new HashMap<>();
    
    static {
        // 初始化常见拼写错误映射
        COMMON_MISSPELLINGS.put("ie", "ei");
        COMMON_MISSPELLINGS.put("ei", "ie");
        COMMON_MISSPELLINGS.put("a", "e");
        COMMON_MISSPELLINGS.put("e", "a");
        COMMON_MISSPELLINGS.put("ant", "ent");
        COMMON_MISSPELLINGS.put("ent", "ant");
        COMMON_MISSPELLINGS.put("able", "ible");
        COMMON_MISSPELLINGS.put("ible", "able");
        // 添加更多常见拼写错误模式
        COMMON_MISSPELLINGS.put("ance", "ence");
        COMMON_MISSPELLINGS.put("ence", "ance");
        COMMON_MISSPELLINGS.put("ize", "ise");
        COMMON_MISSPELLINGS.put("ise", "ize");
        COMMON_MISSPELLINGS.put("yze", "yse");
        COMMON_MISSPELLINGS.put("yse", "yze");
        COMMON_MISSPELLINGS.put("ll", "l");
        COMMON_MISSPELLINGS.put("l", "ll");
        COMMON_MISSPELLINGS.put("mm", "m");
        COMMON_MISSPELLINGS.put("m", "mm");
        COMMON_MISSPELLINGS.put("nn", "n");
        COMMON_MISSPELLINGS.put("n", "nn");
        COMMON_MISSPELLINGS.put("rr", "r");
        COMMON_MISSPELLINGS.put("r", "rr");
        COMMON_MISSPELLINGS.put("ss", "s");
        COMMON_MISSPELLINGS.put("s", "ss");
        COMMON_MISSPELLINGS.put("cc", "c");
        COMMON_MISSPELLINGS.put("c", "cc");
        COMMON_MISSPELLINGS.put("pp", "p");
        COMMON_MISSPELLINGS.put("p", "pp");
        COMMON_MISSPELLINGS.put("tt", "t");
        COMMON_MISSPELLINGS.put("t", "tt");
        COMMON_MISSPELLINGS.put("ff", "f");
        COMMON_MISSPELLINGS.put("f", "ff");
        COMMON_MISSPELLINGS.put("gg", "g");
        COMMON_MISSPELLINGS.put("g", "gg");
        COMMON_MISSPELLINGS.put("tion", "sion");
        COMMON_MISSPELLINGS.put("sion", "tion");
        COMMON_MISSPELLINGS.put("eable", "able");
        COMMON_MISSPELLINGS.put("able", "eable");

        // 初始化键盘邻居映射
        KEYBOARD_NEIGHBORS.put('q', new HashSet<>(Arrays.asList('w', 'a')));
        KEYBOARD_NEIGHBORS.put('w', new HashSet<>(Arrays.asList('q', 'e', 's', 'a')));
        KEYBOARD_NEIGHBORS.put('e', new HashSet<>(Arrays.asList('w', 'r', 'd', 's')));
        KEYBOARD_NEIGHBORS.put('r', new HashSet<>(Arrays.asList('e', 't', 'f', 'd')));
        KEYBOARD_NEIGHBORS.put('t', new HashSet<>(Arrays.asList('r', 'y', 'g', 'f')));
        KEYBOARD_NEIGHBORS.put('y', new HashSet<>(Arrays.asList('t', 'u', 'h', 'g')));
        KEYBOARD_NEIGHBORS.put('u', new HashSet<>(Arrays.asList('y', 'i', 'j', 'h')));
        KEYBOARD_NEIGHBORS.put('i', new HashSet<>(Arrays.asList('u', 'o', 'k', 'j')));
        KEYBOARD_NEIGHBORS.put('o', new HashSet<>(Arrays.asList('i', 'p', 'l', 'k')));
        KEYBOARD_NEIGHBORS.put('p', new HashSet<>(Arrays.asList('o', 'l')));
        KEYBOARD_NEIGHBORS.put('a', new HashSet<>(Arrays.asList('q', 'w', 's', 'z')));
        KEYBOARD_NEIGHBORS.put('s', new HashSet<>(Arrays.asList('w', 'e', 'd', 'x', 'z', 'a')));
        KEYBOARD_NEIGHBORS.put('d', new HashSet<>(Arrays.asList('e', 'r', 'f', 'c', 'x', 's')));
        KEYBOARD_NEIGHBORS.put('f', new HashSet<>(Arrays.asList('r', 't', 'g', 'v', 'c', 'd')));
        KEYBOARD_NEIGHBORS.put('g', new HashSet<>(Arrays.asList('t', 'y', 'h', 'b', 'v', 'f')));
        KEYBOARD_NEIGHBORS.put('h', new HashSet<>(Arrays.asList('y', 'u', 'j', 'n', 'b', 'g')));
        KEYBOARD_NEIGHBORS.put('j', new HashSet<>(Arrays.asList('u', 'i', 'k', 'm', 'n', 'h')));
        KEYBOARD_NEIGHBORS.put('k', new HashSet<>(Arrays.asList('i', 'o', 'l', 'm', 'j')));
        KEYBOARD_NEIGHBORS.put('l', new HashSet<>(Arrays.asList('o', 'p', 'k')));
        KEYBOARD_NEIGHBORS.put('z', new HashSet<>(Arrays.asList('a', 's', 'x')));
        KEYBOARD_NEIGHBORS.put('x', new HashSet<>(Arrays.asList('s', 'd', 'c', 'z')));
        KEYBOARD_NEIGHBORS.put('c', new HashSet<>(Arrays.asList('d', 'f', 'v', 'x')));
        KEYBOARD_NEIGHBORS.put('v', new HashSet<>(Arrays.asList('f', 'g', 'b', 'c')));
        KEYBOARD_NEIGHBORS.put('b', new HashSet<>(Arrays.asList('g', 'h', 'n', 'v')));
        KEYBOARD_NEIGHBORS.put('n', new HashSet<>(Arrays.asList('h', 'j', 'm', 'b')));
        KEYBOARD_NEIGHBORS.put('m', new HashSet<>(Arrays.asList('j', 'k', 'n')));
    }

    /**
     * 计算两个字符串的综合相似度
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        // 完全匹配检查
        if (s1.equals(s2)) {
            return 1.0;
        }

        // 计算各种相似度指标
        double phoneticSim = phoneticSimilarity(s1, s2);
        double editDistSim = normalizedEditDistance(s1, s2);
        double commonPatternSim = commonPatternSimilarity(s1, s2);
        double qgramSim = qgramSimilarity(s1, s2, 2);

        // 调整权重
        return 0.35 * phoneticSim +  // 增加音素相似度的权重
               0.35 * editDistSim +   // 增加编辑距离的权重
               0.15 * commonPatternSim + // 降低模式相似度的权重
               0.15 * qgramSim;         // 降低q-gram相似度的权重
    }

    /**
     * 计算音素相似度
     */
    private static double phoneticSimilarity(String s1, String s2) {
        String p1 = getPhoneticCode(s1);
        String p2 = getPhoneticCode(s2);
        
        if (p1.equals(p2)) {
            return 1.0;
        }
        
        // 添加部分匹配的支持
        int matchLength = 0;
        int minLength = Math.min(p1.length(), p2.length());
        
        for (int i = 0; i < minLength; i++) {
            if (p1.charAt(i) == p2.charAt(i)) {
                matchLength++;
            }
        }
        
        return matchLength > 0 ? (double) matchLength / Math.max(p1.length(), p2.length()) : 0.0;
    }

    /**
     * 改进的音素编码实现
     */
    private static String getPhoneticCode(String s) {
        if (s.isEmpty()) return "";
        
        StringBuilder code = new StringBuilder();
        char prev = '\0';
        
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            
            // 跳过重复字母
            if (c == prev) continue;
            
            // 元音编码
            if (VOWELS.contains(String.valueOf(c))) {
                if (code.length() == 0) code.append('A');
                continue;
            }
            
            // 改进的辅音编码规则
            switch (c) {
                case 'b', 'p', 'f', 'v' -> code.append('1');
                case 'c', 'k', 'g', 'j', 'q' -> code.append('2');
                case 'd', 't' -> code.append('3');
                case 'l' -> code.append('4');
                case 'm', 'n' -> code.append('5');
                case 'r' -> code.append('6');
                case 's', 'z', 'x' -> code.append('7');
                case 'h', 'w', 'y' -> {} // 忽略这些字母
                default -> code.append(c);
            }
            
            prev = c;
        }
        
        return code.toString();
    }

    /**
     * 优化的编辑距离相似度
     */
    private static double normalizedEditDistance(String s1, String s2) {
        if (s1.length() > 30 || s2.length() > 30) {
            // 对于长字符串，使用简化的计算
            return simplifiedEditDistance(s1, s2);
        }
        
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        
        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    int substitution = dp[i - 1][j - 1] + 1;
                    int deletion = dp[i - 1][j] + 1;
                    int insertion = dp[i][j - 1] + 1;
                    
                    // 考虑转置错误（相邻字母交换）
                    int transposition = Integer.MAX_VALUE;
                    if (i > 1 && j > 1 && 
                        s1.charAt(i - 1) == s2.charAt(j - 2) && 
                        s1.charAt(i - 2) == s2.charAt(j - 1)) {
                        transposition = dp[i - 2][j - 2] + 1;
                    }
                    
                    // 考虑键盘布局的相邻字母
                    if (areKeyboardNeighbors(s1.charAt(i - 1), s2.charAt(j - 1))) {
                        substitution -= 0.5; // 降低相邻键位字母替换的成本
                    }
                    
                    dp[i][j] = Math.min(Math.min(substitution, deletion), 
                                      Math.min(insertion, transposition));
                }
            }
        }
        
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - (double) dp[s1.length()][s2.length()] / maxLen;
    }

    /**
     * 简化的编辑距离计算（用于长字符串）
     */
    private static double simplifiedEditDistance(String s1, String s2) {
        int matches = 0;
        int maxLen = Math.max(s1.length(), s2.length());
        int minLen = Math.min(s1.length(), s2.length());
        
        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                matches++;
            }
        }
        
        return (double) matches / maxLen;
    }

    /**
     * 检查两个字母在键盘上是否相邻
     */
    private static boolean areKeyboardNeighbors(char c1, char c2) {
        Set<Character> neighbors = KEYBOARD_NEIGHBORS.get(c1);
        return neighbors != null && neighbors.contains(c2);
    }

    /**
     * 优化的常见模式相似度
     */
    private static double commonPatternSimilarity(String s1, String s2) {
        double similarity = 0.0;
        
        // 检查常见拼写错误模式
        for (Map.Entry<String, String> pattern : COMMON_MISSPELLINGS.entrySet()) {
            String p1 = pattern.getKey();
            String p2 = pattern.getValue();
            
            boolean s1HasP1 = s1.contains(p1);
            boolean s1HasP2 = s1.contains(p2);
            boolean s2HasP1 = s2.contains(p1);
            boolean s2HasP2 = s2.contains(p2);
            
            if ((s1HasP1 && s2HasP2) || (s1HasP2 && s2HasP1)) {
                similarity += 0.5;
            }
        }
        
        // 检查双字母规则
        for (int i = 1; i < s1.length(); i++) {
            if (s1.charAt(i) == s1.charAt(i - 1)) {
                for (int j = 1; j < s2.length(); j++) {
                    if (s2.charAt(j) == s2.charAt(j - 1) && 
                        s1.charAt(i) == s2.charAt(j)) {
                        similarity += 0.3;
                    }
                }
            }
        }
        
        // 检查首字母
        if (s1.length() > 0 && s2.length() > 0 && 
            s1.charAt(0) == s2.charAt(0)) {
            similarity += 0.2;
        }
        
        // 检查末尾字母
        if (s1.length() > 0 && s2.length() > 0 && 
            s1.charAt(s1.length() - 1) == s2.charAt(s2.length() - 1)) {
            similarity += 0.2;
        }
        
        return Math.min(1.0, similarity);
    }

    /**
     * 优化的Q-gram相似度
     */
    private static double qgramSimilarity(String s1, String s2, int q) {
        if (s1.length() < q || s2.length() < q) {
            return normalizedEditDistance(s1, s2);
        }
        
        // 使用LinkedHashSet保持顺序
        Set<String> qgrams1 = new LinkedHashSet<>();
        Set<String> qgrams2 = new LinkedHashSet<>();
        
        // 添加起始和结束标记
        s1 = "#" + s1 + "#";
        s2 = "#" + s2 + "#";
        
        // 生成q-grams
        for (int i = 0; i <= s1.length() - q; i++) {
            qgrams1.add(s1.substring(i, i + q));
        }
        for (int i = 0; i <= s2.length() - q; i++) {
            qgrams2.add(s2.substring(i, i + q));
        }
        
        // 计算改进的Jaccard相似度
        Set<String> union = new HashSet<>(qgrams1);
        union.addAll(qgrams2);
        
        Set<String> intersection = new HashSet<>(qgrams1);
        intersection.retainAll(qgrams2);
        
        // 考虑q-gram的位置信息
        double positionBonus = 0.0;
        List<String> list1 = new ArrayList<>(qgrams1);
        List<String> list2 = new ArrayList<>(qgrams2);
        int minSize = Math.min(list1.size(), list2.size());
        
        for (int i = 0; i < minSize; i++) {
            if (list1.get(i).equals(list2.get(i))) {
                positionBonus += 0.1;
            }
        }
        
        return Math.min(1.0, (double) intersection.size() / union.size() + positionBonus);
    }

    /**
     * 优化的拼写变体生成
     */
    public static Set<String> generateSpellingVariants(String word) {
        Set<String> variants = new LinkedHashSet<>(); // 使用LinkedHashSet保持顺序
        variants.add(word);
        
        // 如果单词太长，限制变体生成
        if (word.length() > 15) {
            return generateLimitedVariants(word);
        }
        
        // 1. 删除一个字母
        for (int i = 0; i < word.length(); i++) {
            variants.add(word.substring(0, i) + word.substring(i + 1));
        }
        
        // 2. 替换一个字母（只考虑键盘相邻字母）
        for (int i = 0; i < word.length(); i++) {
            char original = word.charAt(i);
            Set<Character> neighbors = KEYBOARD_NEIGHBORS.get(original);
            if (neighbors != null) {
                for (char c : neighbors) {
                    variants.add(word.substring(0, i) + c + word.substring(i + 1));
                }
            }
        }
        
        // 3. 插入一个字母（只在合理的位置）
        for (int i = 0; i <= word.length(); i++) {
            // 在元音前后插入元音
            if (i < word.length() && VOWELS.contains(String.valueOf(word.charAt(i)))) {
                for (String vowel : VOWELS) {
                    variants.add(word.substring(0, i) + vowel + word.substring(i));
                }
            }
            // 在辅音前后插入辅音
            if (i < word.length() && !VOWELS.contains(String.valueOf(word.charAt(i)))) {
                char c = word.charAt(i);
                Set<Character> neighbors = KEYBOARD_NEIGHBORS.get(c);
                if (neighbors != null) {
                    for (char neighbor : neighbors) {
                        if (!VOWELS.contains(String.valueOf(neighbor))) {
                            variants.add(word.substring(0, i) + neighbor + word.substring(i));
                        }
                    }
                }
            }
        }
        
        // 4. 相邻字母转置
        for (int i = 0; i < word.length() - 1; i++) {
            variants.add(word.substring(0, i) + 
                        word.charAt(i + 1) + 
                        word.charAt(i) + 
                        word.substring(i + 2));
        }
        
        // 5. 处理常见的拼写错误模式
        for (Map.Entry<String, String> pattern : COMMON_MISSPELLINGS.entrySet()) {
            String p1 = pattern.getKey();
            String p2 = pattern.getValue();
            if (word.contains(p1)) {
                variants.add(word.replace(p1, p2));
            }
        }
        
        // 限制变体数量
        if (variants.size() > MAX_VARIANTS) {
            return new LinkedHashSet<>(new ArrayList<>(variants).subList(0, MAX_VARIANTS));
        }
        
        return variants;
    }

    /**
     * 为长单词生成有限的变体
     */
    private static Set<String> generateLimitedVariants(String word) {
        Set<String> variants = new LinkedHashSet<>();
        variants.add(word);
        
        // 只处理最可能的错误
        // 1. 处理常见的拼写错误模式
        for (Map.Entry<String, String> pattern : COMMON_MISSPELLINGS.entrySet()) {
            String p1 = pattern.getKey();
            String p2 = pattern.getValue();
            if (word.contains(p1)) {
                variants.add(word.replace(p1, p2));
            }
        }
        
        // 2. 相邻字母转置（只在单词的前后部分）
        for (int i = 0; i < 3 && i < word.length() - 1; i++) {
            variants.add(word.substring(0, i) + 
                        word.charAt(i + 1) + 
                        word.charAt(i) + 
                        word.substring(i + 2));
        }
        for (int i = word.length() - 3; i < word.length() - 1; i++) {
            variants.add(word.substring(0, i) + 
                        word.charAt(i + 1) + 
                        word.charAt(i) + 
                        word.substring(i + 2));
        }
        
        return variants;
    }
} 