package com.dictionary.model;

import java.util.Objects;

public class Word {
    private String word;
    private String translation;

    public Word(String word, String translation) {
        this.word = word;
        this.translation = translation;
    }

    public String getWord() {
        return word;
    }

    public String getTranslation() {
        return translation;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Word other = (Word) obj;
        return word.equalsIgnoreCase(other.word) && 
               simplifyTranslation(translation).equalsIgnoreCase(simplifyTranslation(other.translation));
    }

    @Override
    public int hashCode() {
        return Objects.hash(word.toLowerCase(), simplifyTranslation(translation).toLowerCase());
    }

    private String simplifyTranslation(String text) {
        // 去除括号和方括号中的内容，以及词性部分
        return text.replaceAll("\\([^)]*\\)", "")  // 移除括号内容
                  .replaceAll("\\[[^]]*\\]", "")  // 移除方括号内容
                  .replaceAll("\\b\\w+\\.(?=\\s)", "")  // 移除词性部分
                  .trim();
    }

    @Override
    public String toString() {
        return "Word{" +
                "word='" + word + '\'' +
                ", translation='" + translation + '\'' +
                '}';
    }
}
