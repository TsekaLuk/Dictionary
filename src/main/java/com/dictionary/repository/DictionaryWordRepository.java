package com.dictionary.repository;

import com.dictionary.model.Word;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DictionaryWordRepository {
    private final DatabaseHelper dbHelper;

    public DictionaryWordRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public Word save(Word word, Long dictionaryId) {
        String sql = """
            INSERT INTO dictionary_words 
            (dictionary_id, word, translation, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            LocalDateTime now = LocalDateTime.now();
            stmt.setLong(1, dictionaryId);
            stmt.setString(2, word.getWord());
            stmt.setString(3, word.getTranslation());
            stmt.setTimestamp(4, Timestamp.valueOf(now));
            stmt.setTimestamp(5, Timestamp.valueOf(now));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating word failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // You might want to extend the Word class to include an ID field
                    return word;
                } else {
                    throw new SQLException("Creating word failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save dictionary word", e);
        }
    }

    public void saveAll(List<Word> words, Long dictionaryId) {
        String sql = """
            INSERT INTO dictionary_words 
            (dictionary_id, word, translation, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            LocalDateTime now = LocalDateTime.now();
            
            for (Word word : words) {
                stmt.setLong(1, dictionaryId);
                stmt.setString(2, word.getWord());
                stmt.setString(3, word.getTranslation());
                stmt.setTimestamp(4, Timestamp.valueOf(now));
                stmt.setTimestamp(5, Timestamp.valueOf(now));
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save dictionary words batch", e);
        }
    }

    public List<Word> findByDictionaryId(Long dictionaryId) {
        String sql = "SELECT * FROM dictionary_words WHERE dictionary_id = ? ORDER BY word";
        List<Word> words = new ArrayList<>();
        
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, dictionaryId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                words.add(mapResultSetToWord(rs));
            }
            
            return words;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch dictionary words", e);
        }
    }

    public List<Word> findByDictionaryIdAndWord(Long dictionaryId, String wordPattern) {
        String sql = "SELECT * FROM dictionary_words WHERE dictionary_id = ? AND word LIKE ? ORDER BY word";
        List<Word> words = new ArrayList<>();
        
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, dictionaryId);
            stmt.setString(2, "%" + wordPattern + "%");
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                words.add(mapResultSetToWord(rs));
            }
            
            return words;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch dictionary words by pattern", e);
        }
    }

    public void deleteByDictionaryId(Long dictionaryId) {
        String sql = "DELETE FROM dictionary_words WHERE dictionary_id = ?";
        
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, dictionaryId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete dictionary words", e);
        }
    }

    public int getWordCount(Long dictionaryId) {
        String sql = "SELECT COUNT(*) FROM dictionary_words WHERE dictionary_id = ?";
        
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, dictionaryId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
            return 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get word count", e);
        }
    }

    private Word mapResultSetToWord(ResultSet rs) throws SQLException {
        return new Word(
            rs.getString("word"),
            rs.getString("translation")
        );
    }
} 