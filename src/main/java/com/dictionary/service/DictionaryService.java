package com.dictionary.service;

import com.dictionary.model.DictionaryMetadata;
import com.dictionary.model.Word;
import com.dictionary.repository.DictionaryMetadataRepository;
import com.dictionary.repository.DictionaryWordRepository;
import java.util.List;
import java.util.Optional;

public class DictionaryService {
    private final DictionaryMetadataRepository metadataRepository;
    private final DictionaryWordRepository wordRepository;

    public DictionaryService() {
        this.metadataRepository = new DictionaryMetadataRepository();
        this.wordRepository = new DictionaryWordRepository();
    }

    public DictionaryMetadata createDictionary(DictionaryMetadata metadata, List<Word> words) {
        // Save metadata first
        DictionaryMetadata savedMetadata = metadataRepository.save(metadata);
        
        // Save words
        if (words != null && !words.isEmpty()) {
            wordRepository.saveAll(words, savedMetadata.getId());
            
            // Update word count
            savedMetadata.setWordCount(words.size());
            metadataRepository.update(savedMetadata);
        }
        
        return savedMetadata;
    }

    public void updateDictionary(DictionaryMetadata metadata, List<Word> words) {
        // Update metadata
        metadataRepository.update(metadata);
        
        if (words != null) {
            // Delete existing words
            wordRepository.deleteByDictionaryId(metadata.getId());
            
            // Save new words
            if (!words.isEmpty()) {
                wordRepository.saveAll(words, metadata.getId());
                
                // Update word count
                metadata.setWordCount(words.size());
                metadataRepository.update(metadata);
            }
        }
    }

    public void deleteDictionary(Long id) {
        // Delete words first
        wordRepository.deleteByDictionaryId(id);
        
        // Then delete metadata (soft delete)
        metadataRepository.delete(id);
    }

    public List<DictionaryMetadata> getAllDictionaries() {
        return metadataRepository.findAll();
    }

    public Optional<DictionaryMetadata> getDictionaryById(Long id) {
        return metadataRepository.findById(id);
    }

    public List<Word> getWordsByDictionaryId(Long dictionaryId) {
        return wordRepository.findByDictionaryId(dictionaryId);
    }

    public List<Word> searchWords(Long dictionaryId, String pattern) {
        return wordRepository.findByDictionaryIdAndWord(dictionaryId, pattern);
    }

    public void addWordToDictionary(Long dictionaryId, Word word) {
        wordRepository.save(word, dictionaryId);
        
        // Update word count
        Optional<DictionaryMetadata> metadata = metadataRepository.findById(dictionaryId);
        metadata.ifPresent(m -> {
            m.setWordCount(m.getWordCount() + 1);
            metadataRepository.update(m);
        });
    }

    public void addWordsToDictionary(Long dictionaryId, List<Word> words) {
        if (words != null && !words.isEmpty()) {
            wordRepository.saveAll(words, dictionaryId);
            
            // Update word count
            Optional<DictionaryMetadata> metadata = metadataRepository.findById(dictionaryId);
            metadata.ifPresent(m -> {
                m.setWordCount(m.getWordCount() + words.size());
                metadataRepository.update(m);
            });
        }
    }

    public int getWordCount(Long dictionaryId) {
        return wordRepository.getWordCount(dictionaryId);
    }
} 