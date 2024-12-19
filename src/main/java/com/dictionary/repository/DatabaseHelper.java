package com.dictionary.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.*;
import java.nio.file.*;
import java.io.File;

public class DatabaseHelper {
    private static final String DB_NAME = "dictionary.db";
    private static final String DB_PATH = System.getProperty("user.home") + File.separator + ".dictionary" + File.separator + DB_NAME;
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    private static DatabaseHelper instance;
    private HikariDataSource dataSource;

    private DatabaseHelper() {
        initializeDatabase();
    }

    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            // Ensure directory exists
            Files.createDirectories(Paths.get(DB_PATH).getParent());
            
            // Configure HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(JDBC_URL);
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(5);
            config.setIdleTimeout(300000); // 5 minutes
            config.setMaxLifetime(600000); // 10 minutes
            config.setConnectionTimeout(30000); // 30 seconds
            config.setPoolName("DictionaryPool");
            
            // Enable auto-commit
            config.setAutoCommit(true);
            
            // Create the connection pool
            dataSource = new HikariDataSource(config);
            
            // Create tables if they don't exist
            createTables();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create dictionary_metadata table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dictionary_metadata (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    description TEXT,
                    cover_image_path TEXT,
                    format TEXT NOT NULL,
                    word_count INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN DEFAULT 1
                )
            """);

            // Create dictionary_words table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS dictionary_words (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dictionary_id INTEGER NOT NULL,
                    word TEXT NOT NULL,
                    translation TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (dictionary_id) REFERENCES dictionary_metadata(id)
                )
            """);

            // Create indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_dictionary_words_word ON dictionary_words(word)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_dictionary_words_dict_id ON dictionary_words(dictionary_id)");
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    // Helper method to execute updates with auto-closing resources
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        }
    }

    // Helper method to execute queries with auto-closing resources
    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt.executeQuery();
    }

    @Override
    protected void finalize() throws Throwable {
        closePool();
        super.finalize();
    }
} 