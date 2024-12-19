package com.dictionary.util;

import com.dictionary.model.Word;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileIOUtil {
    private static final String WORD_FILE = "EnWords.csv";
    private static final String FAVORITES_FILE = "favorites.csv";
    private static final String APP_DIR = ".dictionary";
    
    public static final String CSV_PATH = System.getProperty("user.dir") + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + WORD_FILE;
    public static final String FAVORITES_PATH = System.getProperty("user.home") + File.separator + APP_DIR + File.separator + FAVORITES_FILE;

    static {
        // 确保应用程序目录存在
        try {
            Files.createDirectories(Paths.get(System.getProperty("user.home"), APP_DIR));
        } catch (IOException e) {
            System.err.println("创建应用程序目录失败: " + e.getMessage());
        }
    }

    public static List<Word> readDictionaryFile(String filename) {
        List<Word> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length == 2) {
                    String word = parts[0].replaceAll("^\"|\"$", "");
                    String translation = parts[1].replaceAll("^\"|\"$", "");
                    words.add(new Word(word, translation));
                }
            }
        } catch (IOException e) {
            System.err.println("读取词典文件失败: " + e.getMessage());
        }
        return words;
    }

    public static void appendWordToFile(String filename, Word word) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true), StandardCharsets.UTF_8))) {
            bw.write("\"" + word.getWord() + "\",\"" + word.getTranslation() + "\"");
            bw.newLine();
        } catch (IOException e) {
            System.err.println("添加单词失败: " + e.getMessage());
        }
    }

    public static void updateDictionaryFile(String filename, List<Word> words) {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {
            bw.write("\"word\",\"translation\"");
            bw.newLine();
            for (Word word : words) {
                bw.write("\"" + word.getWord() + "\",\"" + word.getTranslation() + "\"");
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("更新词典文件失败: " + e.getMessage());
        }
    }
}