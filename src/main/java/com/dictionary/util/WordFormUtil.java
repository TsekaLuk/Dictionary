package com.dictionary.util;

import java.util.*;

public class WordFormUtil {
    // 不规则动词变化表
    private static final Map<String, String[]> IRREGULAR_VERBS = new HashMap<>();
    // 不规则名词复数表
    private static final Map<String, String> IRREGULAR_PLURALS = new HashMap<>();
    // 不规则形容词比较级和最高级表
    private static final Map<String, String[]> IRREGULAR_ADJECTIVES = new HashMap<>();
    
    static {
        // 初始化不规则动词 [过去式, 过去分词]
        IRREGULAR_VERBS.put("be", new String[]{"was/were", "been"});
        IRREGULAR_VERBS.put("do", new String[]{"did", "done"});
        IRREGULAR_VERBS.put("have", new String[]{"had", "had"});
        IRREGULAR_VERBS.put("go", new String[]{"went", "gone"});
        IRREGULAR_VERBS.put("see", new String[]{"saw", "seen"});
        IRREGULAR_VERBS.put("take", new String[]{"took", "taken"});
        IRREGULAR_VERBS.put("get", new String[]{"got", "got/gotten"});
        IRREGULAR_VERBS.put("come", new String[]{"came", "come"});
        IRREGULAR_VERBS.put("know", new String[]{"knew", "known"});
        IRREGULAR_VERBS.put("give", new String[]{"gave", "given"});
        // 添加更多不规则动词...

        // 初始化不规则名词复数
        IRREGULAR_PLURALS.put("child", "children");
        IRREGULAR_PLURALS.put("person", "people");
        IRREGULAR_PLURALS.put("man", "men");
        IRREGULAR_PLURALS.put("woman", "women");
        IRREGULAR_PLURALS.put("foot", "feet");
        IRREGULAR_PLURALS.put("tooth", "teeth");
        IRREGULAR_PLURALS.put("goose", "geese");
        IRREGULAR_PLURALS.put("mouse", "mice");
        // 添加更多不规则复数...

        // 初始化不规则形容词 [比较级, 最高级]
        IRREGULAR_ADJECTIVES.put("good", new String[]{"better", "best"});
        IRREGULAR_ADJECTIVES.put("bad", new String[]{"worse", "worst"});
        IRREGULAR_ADJECTIVES.put("far", new String[]{"farther/further", "farthest/furthest"});
        IRREGULAR_ADJECTIVES.put("little", new String[]{"less", "least"});
        IRREGULAR_ADJECTIVES.put("many", new String[]{"more", "most"});
        IRREGULAR_ADJECTIVES.put("much", new String[]{"more", "most"});
        // 添加更多不规则形容词...
    }

    // 获取所有可能的词形变化
    public static Set<String> getAllWordForms(String word) {
        Set<String> forms = new HashSet<>();
        forms.add(word); // 添加原形

        // 添加动词变化形式
        forms.addAll(getVerbForms(word));
        
        // 添加名词变化形式
        forms.addAll(getNounForms(word));
        
        // 添加形容词变化形式
        forms.addAll(getAdjectiveForms(word));

        return forms;
    }

    // 获取动词的各种形式
    public static Set<String> getVerbForms(String verb) {
        Set<String> forms = new HashSet<>();
        forms.add(verb); // 添加原形

        // 检查不规则动词
        if (IRREGULAR_VERBS.containsKey(verb)) {
            String[] irregularForms = IRREGULAR_VERBS.get(verb);
            forms.add(irregularForms[0]); // 过去式
            forms.addAll(Arrays.asList(irregularForms[1].split("/"))); // 过去分词
        } else {
            // 规则动词变化
            // 第三人称单数
            if (verb.matches(".*[sxzh]$")) {
                forms.add(verb + "es");
            } else if (verb.matches(".*[^aeiou]y$")) {
                forms.add(verb.substring(0, verb.length()-1) + "ies");
            } else {
                forms.add(verb + "s");
            }

            // 过去式和过去分词
            if (verb.matches(".*e$")) {
                forms.add(verb + "d");
            } else if (verb.matches(".*[^aeiou]y$")) {
                forms.add(verb.substring(0, verb.length()-1) + "ied");
            } else if (isDoubleConsonantEnding(verb)) {
                forms.add(verb + verb.charAt(verb.length()-1) + "ed");
            } else {
                forms.add(verb + "ed");
            }

            // 现在分词
            if (verb.matches(".*e$")) {
                forms.add(verb.substring(0, verb.length()-1) + "ing");
            } else if (isDoubleConsonantEnding(verb)) {
                forms.add(verb + verb.charAt(verb.length()-1) + "ing");
            } else {
                forms.add(verb + "ing");
            }
        }

        return forms;
    }

    // 获取名词的各种形式
    public static Set<String> getNounForms(String noun) {
        Set<String> forms = new HashSet<>();
        forms.add(noun); // 添加原形

        // 检查不规则复数
        if (IRREGULAR_PLURALS.containsKey(noun)) {
            forms.add(IRREGULAR_PLURALS.get(noun));
        } else {
            // 规则复数变化
            if (noun.matches(".*[sxzh]$")) {
                forms.add(noun + "es");
            } else if (noun.matches(".*[^aeiou]y$")) {
                forms.add(noun.substring(0, noun.length()-1) + "ies");
            } else if (noun.matches(".*[aeiou]y$")) {
                forms.add(noun + "s");
            } else if (noun.matches(".*[^aeiou]o$")) {
                forms.add(noun + "es");
            } else {
                forms.add(noun + "s");
            }
        }

        // 所有格形式
        if (noun.endsWith("s")) {
            forms.add(noun + "'");
        } else {
            forms.add(noun + "'s");
        }

        return forms;
    }

    // 获取形容词的各种形式
    public static Set<String> getAdjectiveForms(String adjective) {
        Set<String> forms = new HashSet<>();
        forms.add(adjective); // 添加原形

        // 检查不规则形容词
        if (IRREGULAR_ADJECTIVES.containsKey(adjective)) {
            String[] irregularForms = IRREGULAR_ADJECTIVES.get(adjective);
            forms.addAll(Arrays.asList(irregularForms[0].split("/")));  // 比较级
            forms.addAll(Arrays.asList(irregularForms[1].split("/")));  // 最高级
        } else {
            // 规则形容词变化
            if (adjective.matches(".*e$")) {
                forms.add(adjective + "r");     // 比较级
                forms.add(adjective + "st");    // 最高级
            } else if (adjective.matches(".*y$")) {
                forms.add(adjective.substring(0, adjective.length()-1) + "ier");  // 比较级
                forms.add(adjective.substring(0, adjective.length()-1) + "iest"); // 最高级
            } else if (isDoubleConsonantEnding(adjective)) {
                forms.add(adjective + adjective.charAt(adjective.length()-1) + "er");  // 比较级
                forms.add(adjective + adjective.charAt(adjective.length()-1) + "est"); // 最高级
            } else {
                forms.add(adjective + "er");    // 比较级
                forms.add(adjective + "est");   // 最高级
            }
        }

        return forms;
    }

    // 判断是否需要双写末尾辅音字母
    private static boolean isDoubleConsonantEnding(String word) {
        if (word.length() < 2) return false;
        
        char lastChar = word.charAt(word.length() - 1);
        char secondLastChar = word.charAt(word.length() - 2);
        
        // 如果末尾是辅音字母，倒数第二个是元音字母，并且单词长度小于等于2或倒数第三个不是元音
        return !isVowel(lastChar) && isVowel(secondLastChar) && 
               (word.length() <= 2 || !isVowel(word.charAt(word.length() - 3)));
    }

    // 判断是否为元音
    private static boolean isVowel(char c) {
        return "aeiouAEIOU".indexOf(c) != -1;
    }
} 