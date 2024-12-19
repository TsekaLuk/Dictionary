package com.dictionary.util;

import java.sql.*;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DatabaseMigrationTool {
    private static final String MIGRATION_TABLE = "schema_migrations";
    private static final String MIGRATIONS_DIR = "src/main/resources/db/migrations";
    private final Connection connection;
    
    public DatabaseMigrationTool(Connection connection) {
        this.connection = connection;
    }
    
    // 初始化迁移表
    public void initializeMigrationTable() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    version TEXT PRIMARY KEY,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }
    
    // 执行迁移
    public void migrate() throws SQLException, IOException {
        initializeMigrationTable();
        
        // 获取已应用的迁移版本
        Set<String> appliedMigrations = getAppliedMigrations();
        
        // 获取所有迁移文件
        List<Path> migrationFiles = getMigrationFiles();
        
        // 按版本号排序
        migrationFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));
        
        // 执行未应用的迁移
        for (Path migrationFile : migrationFiles) {
            String version = getMigrationVersion(migrationFile);
            
            if (!appliedMigrations.contains(version)) {
                executeMigration(migrationFile, version);
            }
        }
    }
    
    // 获取已应用的迁移版本
    private Set<String> getAppliedMigrations() throws SQLException {
        Set<String> versions = new HashSet<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM " + MIGRATION_TABLE)) {
            while (rs.next()) {
                versions.add(rs.getString("version"));
            }
        }
        return versions;
    }
    
    // 获取迁移文件列表
    private List<Path> getMigrationFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(MIGRATIONS_DIR))) {
            return paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".sql"))
                .collect(Collectors.toList());
        }
    }
    
    // 从文件名获取迁移版本
    private String getMigrationVersion(Path migrationFile) {
        String filename = migrationFile.getFileName().toString();
        return filename.substring(0, filename.indexOf("_"));
    }
    
    // 执行迁移文件
    private void executeMigration(Path migrationFile, String version) throws SQLException, IOException {
        String sql = new String(Files.readAllBytes(migrationFile));
        
        connection.setAutoCommit(false);
        try (Statement stmt = connection.createStatement()) {
            // 执行迁移SQL
            stmt.execute(sql);
            
            // 记录迁移版本
            try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO " + MIGRATION_TABLE + " (version) VALUES (?)")) {
                pstmt.setString(1, version);
                pstmt.executeUpdate();
            }
            
            connection.commit();
            System.out.println("Successfully applied migration: " + migrationFile.getFileName());
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Migration failed: " + migrationFile.getFileName(), e);
        } finally {
            connection.setAutoCommit(true);
        }
    }
    
    // 创建新的迁移文件
    public static void createMigrationFile(String description) throws IOException {
        // 确保迁移目录存在
        Files.createDirectories(Paths.get(MIGRATIONS_DIR));
        
        // 生成迁移文件名
        String version = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = String.format("%s_%s.sql", version, description.toLowerCase().replace(' ', '_'));
        
        // 创建迁移文件
        Path migrationFile = Paths.get(MIGRATIONS_DIR, filename);
        Files.createFile(migrationFile);
        
        // 写入模板内容
        String template = """
            -- Migration: %s
            -- Created at: %s
            
            -- Write your migration SQL here
            
            -- Example:
            -- CREATE TABLE example (
            --     id INTEGER PRIMARY KEY,
            --     name TEXT NOT NULL
            -- );
            """.formatted(description, LocalDateTime.now());
        
        Files.write(migrationFile, template.getBytes());
        System.out.println("Created migration file: " + migrationFile);
    }
    
    // 回滚最后一次迁移
    public void rollback() throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT version FROM " + MIGRATION_TABLE +
                 " ORDER BY applied_at DESC LIMIT 1")) {
            
            if (rs.next()) {
                String version = rs.getString("version");
                connection.setAutoCommit(false);
                
                try {
                    // 执行回滚SQL（如果存在）
                    Path rollbackFile = Paths.get(MIGRATIONS_DIR, version + "_rollback.sql");
                    if (Files.exists(rollbackFile)) {
                        String sql = new String(Files.readAllBytes(rollbackFile));
                        stmt.execute(sql);
                    }
                    
                    // 删除迁移记录
                    stmt.execute("DELETE FROM " + MIGRATION_TABLE + " WHERE version = '" + version + "'");
                    
                    connection.commit();
                    System.out.println("Successfully rolled back migration: " + version);
                } catch (Exception e) {
                    connection.rollback();
                    throw new SQLException("Rollback failed for version: " + version, e);
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }
} 