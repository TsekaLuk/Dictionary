package com.dictionary.gui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import com.dictionary.model.Dictionary;
import com.dictionary.model.Word;
import com.dictionary.util.FileIOUtil;

public class AddWordDialogFX extends Dialog<Word> {
    private final TextField wordField = new TextField();
    private final TextField translationField = new TextField();
    private final Dictionary dictionary;

    public AddWordDialogFX(Stage owner, Dictionary dictionary) {
        this.dictionary = dictionary;
        
        // 设置对话框标题和所有者
        setTitle("添加生词");
        initOwner(owner);

        // 创建内容面板
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        // 添加输入字段
        grid.add(new Label("单词:"), 0, 0);
        grid.add(wordField, 1, 0);
        grid.add(new Label("翻译:"), 0, 1);
        grid.add(translationField, 1, 1);

        // 设置字段样式
        wordField.setPromptText("请输入单词");
        translationField.setPromptText("请输入翻译");
        wordField.setPrefWidth(300);
        translationField.setPrefWidth(300);

        // 设置对话框内容
        getDialogPane().setContent(grid);

        // 添加按钮
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(addButtonType, cancelButtonType);

        // 设置结果转换器
        setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String word = wordField.getText().trim();
                String translation = translationField.getText().trim();
                
                if (word.isEmpty() || translation.isEmpty()) {
                    showAlert("单词和翻译都不能为空", Alert.AlertType.WARNING);
                    return null;
                }

                Word newWord = new Word(word, translation);
                dictionary.addWord(newWord);
                FileIOUtil.appendWordToFile(FileIOUtil.CSV_PATH, newWord);
                showAlert("添加成功", Alert.AlertType.INFORMATION);
                return newWord;
            }
            return null;
        });

        // 应用CSS样式
        getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/modern-theme.css").toExternalForm()
        );
        getDialogPane().getStyleClass().add("dialog-pane");
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
} 