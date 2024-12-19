package com.dictionary.model;

import com.dictionary.util.FileIOUtil;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class WordBook {
    private Set<Word> favorites;

    public WordBook() {
        favorites = new HashSet<>();
    }

    public void loadFromFile(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length == 2) {
                    String word = parts[0].replaceAll("^\"|\"$", "");
                    String translation = parts[1].replaceAll("^\"|\"$", "");
                    favorites.add(new Word(word, translation));
                }
            }
        } catch (IOException e) {
            System.err.println("加载收藏夹失败: " + e.getMessage());
        }
    }

    public void saveToFile(String filename) {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            for (Word word : favorites) {
                writer.write("\"" + word.getWord() + "\",\"" + word.getTranslation() + "\"");
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("保存收藏夹失败: " + e.getMessage());
        }
    }

    public void addFavorite(Word word) {
        favorites.add(word);
        saveToFile(FileIOUtil.FAVORITES_PATH);
    }

    public void removeFavorite(Word word) {
        favorites.remove(word);
        saveToFile(FileIOUtil.FAVORITES_PATH);
    }

    public boolean isFavorite(Word word) {
        return favorites.contains(word);
    }

    public List<Word> getFavorites() {
        return new ArrayList<>(favorites);
    }
} 