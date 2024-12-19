package com.dictionary.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.scene.input.KeyCode;
import javafx.geometry.Pos;
import com.dictionary.model.Dictionary;
import com.dictionary.model.Word;
import com.dictionary.util.FileIOUtil;

import java.util.List;

public class ModifyDeleteDialogFX extends Dialog<Void> {
    private final TextField searchField = new TextField();
    private final TextField wordField = new TextField();
    private final TextField translationField = new TextField();
    private final ListView<Word> wordList = new ListView<>();
    private final Dictionary dictionary;

    public ModifyDeleteDialogFX(Stage owner, Dictionary dictionary) {
        this.dictionary = dictionary;
        
        // 设置对话框标题和所有者
        setTitle("修改/删除词条");
        initOwner(owner);

        // 创建内容面板
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(400);

        // 创建搜索区域
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = new Label("搜索:");
        searchField.setPromptText("输入要搜索的单词");
        searchField.setPrefWidth(300);
        Button searchButton = new Button("搜索");
        searchBox.getChildren().addAll(searchLabel, searchField, searchButton);

        // 创建列表视图
        wordList.setPrefHeight(200);
        wordList.setCellFactory(lv -> new ListCell<Word>() {
            @Override
            protected void updateItem(Word word, boolean empty) {
                super.updateItem(word, empty);
                if (empty || word == null) {
                    setText(null);
                } else {
                    setText(word.getWord() + " - " + word.getTranslation());
                }
            }
        });

        // 创建编辑区域
        GridPane editArea = new GridPane();
        editArea.setHgap(10);
        editArea.setVgap(10);
        editArea.add(new Label("单词:"), 0, 0);
        editArea.add(wordField, 1, 0);
        editArea.add(new Label("翻译:"), 0, 1);
        editArea.add(translationField, 1, 1);
        wordField.setPrefWidth(300);
        translationField.setPrefWidth(300);

        // 创建按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        Button modifyButton = new Button("修改");
        Button deleteButton = new Button("删除");
        buttonBox.getChildren().addAll(modifyButton, deleteButton);

        // 添加所有组件到内容面板
        content.getChildren().addAll(searchBox, wordList, editArea, buttonBox);

        // 设置对话框内容
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // 添加事件处理
        searchButton.setOnAction(e -> performSearch());
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                performSearch();
            }
        });

        wordList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                wordField.setText(newVal.getWord());
                translationField.setText(newVal.getTranslation());
            }
        });

        modifyButton.setOnAction(e -> modifyWord());
        deleteButton.setOnAction(e -> deleteWord());

        // 应用CSS样式
        getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/modern-theme.css").toExternalForm()
        );
        getDialogPane().getStyleClass().add("dialog-pane");
        
        // 初始加载所有词条
        performSearch();
    }

    private void performSearch() {
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            List<Word> results = dictionary.findSimilarWords(searchText, true);
            wordList.setItems(FXCollections.observableArrayList(results));
        } else {
            // 如果搜索框为空，显示所有词条
            List<Word> allWords = dictionary.getAllWords();
            wordList.setItems(FXCollections.observableArrayList(allWords));
        }
    }

    private void modifyWord() {
        Word selectedWord = wordList.getSelectionModel().getSelectedItem();
        if (selectedWord == null) {
            showAlert("请先选择要修改的词条", Alert.AlertType.WARNING);
            return;
        }

        String newWord = wordField.getText().trim();
        String newTranslation = translationField.getText().trim();

        if (newWord.isEmpty() || newTranslation.isEmpty()) {
            showAlert("单词和翻译都不能为空", Alert.AlertType.WARNING);
            return;
        }

        Word modifiedWord = new Word(newWord, newTranslation);
        dictionary.modifyWord(selectedWord, modifiedWord);
        FileIOUtil.updateDictionaryFile(FileIOUtil.CSV_PATH, dictionary.getAllWords());
        
        showAlert("修改成功", Alert.AlertType.INFORMATION);
        performSearch();
    }

    private void deleteWord() {
        Word selectedWord = wordList.getSelectionModel().getSelectedItem();
        if (selectedWord == null) {
            showAlert("请先选择要删除的词条", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认删除");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("确定要删除这个词条吗？");
        confirmDialog.initOwner(getDialogPane().getScene().getWindow());

        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                dictionary.removeWord(selectedWord);
                FileIOUtil.updateDictionaryFile(FileIOUtil.CSV_PATH, dictionary.getAllWords());
                showAlert("删除成功", Alert.AlertType.INFORMATION);
                performSearch();
            }
        });
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(getDialogPane().getScene().getWindow());
        alert.showAndWait();
    }
} 