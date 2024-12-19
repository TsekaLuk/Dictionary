package com.dictionary.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.ImageView;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.application.Platform;
import com.dictionary.util.SVGUtil;
import com.dictionary.util.DictionaryImportUtil;
import com.dictionary.model.Dictionary;
import com.dictionary.model.Word;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

public class DictionaryRepositoryPage extends VBox {
    private Stage primaryStage;
    private Runnable onBackAction;
    private Dictionary dictionary;
    private Label wordCountLabel;

    public DictionaryRepositoryPage(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.dictionary = new Dictionary();
        
        // 设置基本样式
        setSpacing(20);
        setPadding(new Insets(20));
        getStyleClass().add("repository-page");
        
        // 创建标题区域
        createHeader();
        
        // 创建词典卡片区域
        createDictionaryCard();
    }

    public void setDictionary(Dictionary dictionary) {
        this.dictionary = dictionary;
        updateWordCount();
    }

    private void updateWordCount() {
        if (wordCountLabel != null && dictionary != null) {
            int count = dictionary.getAllWords().size();
            Platform.runLater(() -> wordCountLabel.setText("词条数: " + count));
        }
    }

    public void setOnBackAction(Runnable action) {
        this.onBackAction = action;
    }
    
    private void createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 20, 0));
        
        // 创建返回按钮
        Button backButton = new Button();
        backButton.getStyleClass().add("back-button");
        ImageView backIcon = createSVGImageView("/icons/back.svg", 24, 24);
        if (backIcon != null) {
            backButton.setGraphic(backIcon);
        }
        backButton.setOnAction(e -> {
            if (onBackAction != null) {
                onBackAction.run();
            }
        });
        
        // 创建标题
        VBox titleBox = new VBox(5);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("词典仓库");
        title.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 24));
        title.getStyleClass().add("repository-title");
        
        Label subtitle = new Label("管理您的词典");
        subtitle.setFont(Font.font("Microsoft YaHei", 14));
        subtitle.getStyleClass().add("repository-subtitle");
        
        titleBox.getChildren().addAll(title, subtitle);
        
        header.getChildren().addAll(backButton, titleBox);
        getChildren().add(header);
    }
    
    private void createDictionaryCard() {
        // 创建卡片容器
        VBox container = new VBox(20);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(20));
        
        // 创建词典卡片
        VBox card = new VBox(15);
        card.getStyleClass().add("dictionary-card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(400);
        card.setMaxWidth(400);
        
        // 准备要添加到卡片的组件列表
        List<javafx.scene.Node> cardComponents = new ArrayList<>();
        
        // 添加图标
        ImageView icon = createSVGImageView("/icons/dictionary.svg", 48, 48);
        if (icon != null) {
            icon.getStyleClass().add("dictionary-icon");
            cardComponents.add(icon);
        }
        
        // 添加标题
        Label titleLabel = new Label("英汉词典");
        titleLabel.setFont(Font.font("Microsoft YaHei", FontWeight.BOLD, 18));
        titleLabel.getStyleClass().add("dictionary-title");
        cardComponents.add(titleLabel);
        
        // 添加类型标签
        Label typeLabel = new Label("基础词典");
        typeLabel.getStyleClass().add("dictionary-type");
        cardComponents.add(typeLabel);
        
        // 添加描述
        Label descLabel = new Label("包含常用英语单词及其中文释义 (EnWords.csv)");
        descLabel.setWrapText(true);
        descLabel.getStyleClass().add("dictionary-description");
        cardComponents.add(descLabel);
        
        // 添加分隔符
        cardComponents.add(new Separator());
        
        // 添加统计信息
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        
        wordCountLabel = new Label("词条数: " + (dictionary != null ? dictionary.getAllWords().size() : 0));
        wordCountLabel.getStyleClass().add("dictionary-stats");
        
        Label updateLabel = new Label("上次更新: 2023-12-19");
        updateLabel.getStyleClass().add("dictionary-stats");
        
        statsBox.getChildren().addAll(wordCountLabel, updateLabel);
        cardComponents.add(statsBox);
        
        // 添加按钮
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button importBtn = new Button("导入新词条");
        importBtn.getStyleClass().addAll("dictionary-button", "import-button");
        importBtn.setOnAction(e -> handleImport());
        
        Button useBtn = new Button("使用");
        useBtn.getStyleClass().addAll("dictionary-button", "use-button");
        useBtn.setOnAction(e -> {
            if (onBackAction != null) {
                onBackAction.run();
            }
        });
        
        buttonBox.getChildren().addAll(importBtn, useBtn);
        cardComponents.add(buttonBox);
        
        // 将所有非空组件添加到卡片
        card.getChildren().addAll(cardComponents);
        
        // 添加悬停效果
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setRadius(10);
        shadow.setOffsetY(5);
        
        card.setOnMouseEntered(e -> {
            card.getStyleClass().add("dictionary-card-hover");
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1.02);
            scale.setToY(1.02);
            scale.play();
        });
        
        card.setOnMouseExited(e -> {
            card.getStyleClass().remove("dictionary-card-hover");
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), card);
            scale.setToX(1);
            scale.setToY(1);
            scale.play();
        });
        
        container.getChildren().add(card);
        
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("repository-scroll-pane");
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        getChildren().add(scrollPane);
    }
    
    private ImageView createSVGImageView(String path, int width, int height) {
        try {
            ImageIcon icon = SVGUtil.loadSVGIcon(path.substring(1), width, height, null);
            if (icon != null) {
                BufferedImage bufferedImage = new BufferedImage(
                    width, height, BufferedImage.TYPE_INT_ARGB);
                icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
                return new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
            }
        } catch (Exception e) {
            System.err.println("Error loading SVG icon from path: " + path);
            e.printStackTrace();
        }
        return null;
    }

    private void handleImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择词典文件");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
            new FileChooser.ExtensionFilter("Anki Files", "*.apkg", "*.colpkg")
        );
        
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            // 显示加载对话框
            ProgressDialog progressDialog = new ProgressDialog(primaryStage, "正在导入词典...");
            progressDialog.show();
            
            // 在后台线程中处理导入
            new Thread(() -> {
                try {
                    DictionaryImportUtil.ImportResult result = DictionaryImportUtil.importDictionary(file);
                    
                    Platform.runLater(() -> {
                        progressDialog.close();
                        
                        if (result.isSuccess()) {
                            List<Word> words = result.getWords();
                            dictionary.addAll(words);
                            updateWordCount();
                            showAlert("导入成功", "成功导入 " + words.size() + " 个词条", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("导入失败", result.getError(), Alert.AlertType.ERROR);
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        progressDialog.close();
                        showAlert("导入失败", ex.getMessage(), Alert.AlertType.ERROR);
                    });
                }
            }).start();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    public void updateDictionaryData() {
        if (dictionary != null) {
            // 只更新统计信息，不重新创建卡片
            updateStatistics();
        }
    }

    private void updateStatistics() {
        if (dictionary != null && wordCountLabel != null) {
            List<Word> words = dictionary.getAllWords();
            int totalWords = words.size();
            int englishWords = (int) words.stream()
                .filter(w -> w.getWord().matches("^[a-zA-Z\\s-]+$"))
                .count();
            int chineseWords = totalWords - englishWords;
            
            Platform.runLater(() -> {
                wordCountLabel.setText(String.format(
                    "词条数: %d (英文: %d, 中文: %d)",
                    totalWords, englishWords, chineseWords
                ));
            });
        }
    }
}

class ProgressDialog extends Dialog<Void> {
    public ProgressDialog(Stage owner, String message) {
        initOwner(owner);
        setTitle("请稍候");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(50, 50);
        
        Label messageLabel = new Label(message);
        messageLabel.setFont(Font.font("Microsoft YaHei", 14));
        
        content.getChildren().addAll(progress, messageLabel);
        getDialogPane().setContent(content);
        
        getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/modern-theme.css").toExternalForm()
        );
    }
} 