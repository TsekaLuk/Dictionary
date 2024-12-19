-- Rollback Migration: Initial Schema
-- Created at: 2023-12-20 00:00:00

-- Drop indexes
DROP INDEX IF EXISTS idx_dictionary_words_word;
DROP INDEX IF EXISTS idx_dictionary_words_dict_id;

-- Drop tables
DROP TABLE IF EXISTS dictionary_words;
DROP TABLE IF EXISTS dictionary_metadata; 