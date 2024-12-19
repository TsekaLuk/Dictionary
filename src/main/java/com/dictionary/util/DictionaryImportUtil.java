package com.dictionary.util;

import com.dictionary.model.Word;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.sql.*;

public class DictionaryImportUtil {
    
    public static class ImportResult {
        private final List<Word> words;
        private final List<String> headers;
        private final Map<String, Integer> columnMap;
        private final String format;
        private final String error;

        public ImportResult(List<Word> words, List<String> headers, Map<String, Integer> columnMap, String format) {
            this.words = words;
            this.headers = headers;
            this.columnMap = columnMap;
            this.format = format;
            this.error = null;
        }

        public ImportResult(String error) {
            this.words = null;
            this.headers = null;
            this.columnMap = null;
            this.format = null;
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null;
        }

        public List<Word> getWords() {
            return words;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public Map<String, Integer> getColumnMap() {
            return columnMap;
        }

        public String getFormat() {
            return format;
        }

        public String getError() {
            return error;
        }
    }

    public static ImportResult importDictionary(File file) {
        String fileName = file.getName().toLowerCase();
        
        try {
            if (fileName.endsWith(".csv")) {
                return importCSV(file);
            } else if (fileName.endsWith(".xlsx")) {
                return importExcel(file, true);
            } else if (fileName.endsWith(".xls")) {
                return importExcel(file, false);
            } else if (fileName.endsWith(".apkg") || fileName.endsWith(".colpkg")) {
                return importAnki(file);
            } else {
                return new ImportResult("Unsupported file format");
            }
        } catch (Exception e) {
            return new ImportResult("Import failed: " + e.getMessage());
        }
    }

    private static ImportResult importCSV(File file) throws IOException {
        List<Word> words = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        Map<String, Integer> columnMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            
            // Read headers
            String headerLine = reader.readLine();
            if (headerLine != null) {
                String[] headerArray = headerLine.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < headerArray.length; i++) {
                    String header = headerArray[i].trim().replaceAll("^\"|\"$", "");
                    headers.add(header);
                    columnMap.put(header, i);
                }
            }

            // Try to identify word and translation columns
            int wordIndex = -1;
            int translationIndex = -1;

            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i).toLowerCase();
                if (header.contains("word") || header.contains("term") || header.contains("vocabulary")) {
                    wordIndex = i;
                } else if (header.contains("translation") || header.contains("meaning") || header.contains("definition")) {
                    translationIndex = i;
                }
            }

            // If we couldn't identify columns, use first two columns
            if (wordIndex == -1 || translationIndex == -1) {
                wordIndex = 0;
                translationIndex = 1;
            }

            // Read data
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length >= 2) {
                    String word = parts[wordIndex].replaceAll("^\"|\"$", "").trim();
                    String translation = parts[translationIndex].replaceAll("^\"|\"$", "").trim();
                    if (!word.isEmpty() && !translation.isEmpty()) {
                        words.add(new Word(word, translation));
                    }
                }
            }
        }

        return new ImportResult(words, headers, columnMap, "CSV");
    }

    private static ImportResult importExcel(File file, boolean isXlsx) throws IOException {
        List<Word> words = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        Map<String, Integer> columnMap = new HashMap<>();

        try (InputStream is = new FileInputStream(file);
             Workbook workbook = isXlsx ? new XSSFWorkbook(is) : new HSSFWorkbook(is)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Read headers
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String header = cell != null ? cell.toString().trim() : "";
                headers.add(header);
                columnMap.put(header, i);
            }

            // Try to identify word and translation columns
            int wordIndex = -1;
            int translationIndex = -1;

            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i).toLowerCase();
                if (header.contains("word") || header.contains("term") || header.contains("vocabulary")) {
                    wordIndex = i;
                } else if (header.contains("translation") || header.contains("meaning") || header.contains("definition")) {
                    translationIndex = i;
                }
            }

            // If we couldn't identify columns, use first two columns
            if (wordIndex == -1 || translationIndex == -1) {
                wordIndex = 0;
                translationIndex = 1;
            }

            // Read data
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    Cell wordCell = row.getCell(wordIndex);
                    Cell translationCell = row.getCell(translationIndex);
                    
                    if (wordCell != null && translationCell != null) {
                        String word = wordCell.toString().trim();
                        String translation = translationCell.toString().trim();
                        
                        if (!word.isEmpty() && !translation.isEmpty()) {
                            words.add(new Word(word, translation));
                        }
                    }
                }
            }
        }

        return new ImportResult(words, headers, columnMap, isXlsx ? "XLSX" : "XLS");
    }

    private static ImportResult importAnki(File file) throws IOException {
        List<Word> words = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        Map<String, Integer> columnMap = new HashMap<>();

        try (ZipFile zipFile = new ZipFile(file)) {
            // Find the SQLite database file
            ZipEntry dbEntry = null;
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".anki2")) {
                    dbEntry = entry;
                    break;
                }
            }

            if (dbEntry == null) {
                return new ImportResult("No Anki database found in the package");
            }

            // Extract the database to a temporary file
            File tempDb = File.createTempFile("anki", ".db");
            tempDb.deleteOnExit();

            try (InputStream is = zipFile.getInputStream(dbEntry);
                 FileOutputStream fos = new FileOutputStream(tempDb)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }

            // Connect to the SQLite database and process data
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tempDb.getAbsolutePath())) {
                // Get field names from the first note
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, flds FROM notes ORDER BY id DESC LIMIT 1")) {
                    
                    if (rs.next()) {
                        String[] fields = rs.getString("flds").split("\u001F");
                        for (int i = 0; i < fields.length; i++) {
                            headers.add("Field " + (i + 1));
                            columnMap.put("Field " + (i + 1), i);
                        }
                    }
                } catch (SQLException e) {
                    return new ImportResult("Failed to read Anki note fields: " + e.getMessage());
                }

                // Get all notes
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT flds FROM notes")) {
                    
                    while (rs.next()) {
                        String[] fields = rs.getString("flds").split("\u001F");
                        if (fields.length >= 2) {
                            String word = fields[0].trim();
                            String translation = fields[1].trim();
                            if (!word.isEmpty() && !translation.isEmpty()) {
                                words.add(new Word(word, translation));
                            }
                        }
                    }
                } catch (SQLException e) {
                    return new ImportResult("Failed to read Anki notes: " + e.getMessage());
                }
            } catch (SQLException e) {
                return new ImportResult("Failed to connect to Anki database: " + e.getMessage());
            }
        }

        return new ImportResult(words, headers, columnMap, "ANKI");
    }
} 