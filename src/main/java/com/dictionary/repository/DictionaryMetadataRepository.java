package com.dictionary.repository;

import com.dictionary.model.DictionaryMetadata;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DictionaryMetadataRepository {
    private final DatabaseHelper dbHelper;

    public DictionaryMetadataRepository() {
        this.dbHelper = DatabaseHelper.getInstance();
    }

    public DictionaryMetadata save(DictionaryMetadata metadata) {
        String sql = """
            INSERT INTO dictionary_metadata 
            (name, description, cover_image_path, format, word_count, created_at, updated_at, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, metadata.getName());
            stmt.setString(2, metadata.getDescription());
            stmt.setString(3, metadata.getCoverImagePath());
            stmt.setString(4, metadata.getFormat());
            stmt.setInt(5, metadata.getWordCount());
            stmt.setTimestamp(6, Timestamp.valueOf(metadata.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(metadata.getUpdatedAt()));
            stmt.setBoolean(8, metadata.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating dictionary failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    metadata.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating dictionary failed, no ID obtained.");
                }
            }

            return metadata;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save dictionary metadata", e);
        }
    }

    public Optional<DictionaryMetadata> findById(Long id) {
        String sql = "SELECT * FROM dictionary_metadata WHERE id = ?";
        
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToMetadata(rs));
            }
            
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find dictionary metadata by ID", e);
        }
    }

    public List<DictionaryMetadata> findAll() {
        String sql = "SELECT * FROM dictionary_metadata WHERE is_active = 1 ORDER BY updated_at DESC";
        List<DictionaryMetadata> dictionaries = new ArrayList<>();
        
        try (Connection conn = dbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                dictionaries.add(mapResultSetToMetadata(rs));
            }
            
            return dictionaries;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch dictionary metadata list", e);
        }
    }

    public void update(DictionaryMetadata metadata) {
        String sql = """
            UPDATE dictionary_metadata 
            SET name = ?, description = ?, cover_image_path = ?, format = ?, 
                word_count = ?, updated_at = ?, is_active = ?
            WHERE id = ?
        """;

        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, metadata.getName());
            stmt.setString(2, metadata.getDescription());
            stmt.setString(3, metadata.getCoverImagePath());
            stmt.setString(4, metadata.getFormat());
            stmt.setInt(5, metadata.getWordCount());
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setBoolean(7, metadata.isActive());
            stmt.setLong(8, metadata.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating dictionary failed, no rows affected.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update dictionary metadata", e);
        }
    }

    public void delete(Long id) {
        String sql = "UPDATE dictionary_metadata SET is_active = 0 WHERE id = ?";
        
        try (Connection conn = dbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete dictionary metadata", e);
        }
    }

    private DictionaryMetadata mapResultSetToMetadata(ResultSet rs) throws SQLException {
        DictionaryMetadata metadata = new DictionaryMetadata();
        metadata.setId(rs.getLong("id"));
        metadata.setName(rs.getString("name"));
        metadata.setDescription(rs.getString("description"));
        metadata.setCoverImagePath(rs.getString("cover_image_path"));
        metadata.setFormat(rs.getString("format"));
        metadata.setWordCount(rs.getInt("word_count"));
        metadata.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        metadata.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        metadata.setActive(rs.getBoolean("is_active"));
        return metadata;
    }
} 