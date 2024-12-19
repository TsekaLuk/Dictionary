package com.dictionary.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

public class SQLOptimizer {
    private static final Map<String, String> queryCache = new ConcurrentHashMap<>();
    
    // 优化查询语句
    public static String optimizeQuery(String sql) {
        // 检查缓存
        if (queryCache.containsKey(sql)) {
            return queryCache.get(sql);
        }
        
        // 进行SQL优化
        String optimizedSql = sql;
        
        // 1. 添加索引提示
        if (sql.toLowerCase().contains("where")) {
            optimizedSql = addIndexHints(optimizedSql);
        }
        
        // 2. 限制结果集大小
        if (!optimizedSql.toLowerCase().contains("limit")) {
            optimizedSql = addLimit(optimizedSql);
        }
        
        // 缓存优化后的查询
        queryCache.put(sql, optimizedSql);
        return optimizedSql;
    }
    
    // 生成批量插入语句
    public static String createBatchInsertQuery(String tableName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");
        return sql.toString();
    }
    
    // 添加索引提示
    private static String addIndexHints(String sql) {
        // 为常用查询添加索引提示
        if (sql.contains("dictionary_words") && sql.contains("word LIKE")) {
            sql = sql.replace("FROM dictionary_words",
                "FROM dictionary_words INDEXED BY idx_dictionary_words_word");
        }
        return sql;
    }
    
    // 添加结果集限制
    private static String addLimit(String sql) {
        return sql + " LIMIT 1000";
    }
    
    // 清除查询缓存
    public static void clearCache() {
        queryCache.clear();
    }
    
    // 获取缓存大小
    public static int getCacheSize() {
        return queryCache.size();
    }
} 