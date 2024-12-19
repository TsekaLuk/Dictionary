package com.dictionary;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.collections.FXCollections;
import javafx.scene.input.*;
import javafx.scene.text.Font;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;

import com.dictionary.model.Dictionary;
import com.dictionary.model.Word;
import com.dictionary.model.WordBook;
import com.dictionary.util.FileIOUtil;
import com.dictionary.gui.AddWordDialogFX;
import com.dictionary.gui.ModifyDeleteDialogFX;
import com.dictionary.util.TTSUtil;
import com.dictionary.util.SVGUtil;

import java.io.File;
import java.util.List;

public class DictionaryApp extends Application {
    private Stage primaryStage;
    private Scene scene;
    private BooleanProperty darkMode = new SimpleBooleanProperty(false);
    private StackPane root;
    private VBox contentPane;
    
    // 核心组件
    private Dictionary dictionary;
    private WordBook wordBook;
    private TextField searchField;
    private ListView<Word> wordList;
    private RadioButton englishToChineseBtn;
    private RadioButton chineseToEnglishBtn;
    private Button showFavoritesBtn;
    private boolean showingFavorites = false;

    // 加载指示器
    private StackPane loadingOverlay;
    private ProgressIndicator progressIndicator;
    private Label loadingLabel;

    public DictionaryApp() {
        dictionary = new Dictionary();
        wordBook = new WordBook();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Create root container
        root = new StackPane();
        root.setPadding(new Insets(20));
        
        // Create content pane
        contentPane = new VBox(15);
        contentPane.setPrefWidth(1186);
        contentPane.setPrefHeight(667);
        contentPane.setMinWidth(800);
        contentPane.setMinHeight(600);
        contentPane.getStyleClass().add("content-pane");
        
        initializeUI();
        createAndShowGUI();
        
        // Initialize TTS after UI is ready
        initializeTTS();
        
        // Load dictionary and favorites
        loadData();
        primaryStage.show();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> showLoading("正在加载词典..."));
                
                // Load dictionary
                dictionary.loadFromFile(FileIOUtil.CSV_PATH);
                
                // Load favorites
                wordBook.loadFromFile(FileIOUtil.FAVORITES_PATH);
                
                Platform.runLater(() -> {
                    hideLoading();
                    updateTableView();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideLoading();
                    showAlert("数据加载失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void initializeUI() {
        // 初始化加载指示器
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(100, 100);
        
        loadingLabel = new Label();
        loadingLabel.getStyleClass().add("loading-label");
        
        VBox loadingContent = new VBox(20);
        loadingContent.setAlignment(Pos.CENTER);
        loadingContent.getChildren().addAll(progressIndicator, loadingLabel);
        
        loadingOverlay = new StackPane(loadingContent);
        loadingOverlay.getStyleClass().add("loading-overlay");
        loadingOverlay.setVisible(false);
        
        root.getChildren().add(loadingOverlay);
    }

    private void initializeTTS() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> showLoading("正在初始化TTS..."));
                TTSUtil.initialize();
                Platform.runLater(() -> hideLoading());
            } catch (Exception e) {
                Platform.runLater(() -> {
                    hideLoading();
                    showAlert("TTS初始化失败: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void showLoading(String message) {
        loadingLabel.setText(message);
        loadingOverlay.setVisible(true);
    }

    private void hideLoading() {
        loadingOverlay.setVisible(false);
    }

    private void initializeBackgrounds() {
        try {
            System.out.println("Attempting to load background images...");
            var lightStream = getClass().getResourceAsStream("/bg.png");
            var darkStream = getClass().getResourceAsStream("/bg-dark.png");
            
            if (lightStream == null) {
                System.err.println("Failed to load /bg.png - resource stream is null");
                return;
            }
            if (darkStream == null) {
                System.err.println("Failed to load /bg-dark.png - resource stream is null");
                return;
            }
            
            System.out.println("Resource streams obtained successfully");
            Image lightImage = new Image(lightStream);
            Image darkImage = new Image(darkStream);
            
            if (lightImage.isError()) {
                System.err.println("Error loading light image: " + lightImage.getException());
                return;
            }
            if (darkImage.isError()) {
                System.err.println("Error loading dark image: " + darkImage.getException());
                return;
            }
            
            System.out.println("Images loaded successfully");
            new BackgroundImage(
                lightImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, false, false)
            );
            
            new BackgroundImage(
                darkImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, false, false)
            );
            
            System.out.println("Background images initialized successfully");
        } catch (Exception e) {
            showAlert("背景图片加载失败: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void createAndShowGUI() {
        // 创建搜索区域
        VBox searchArea = createSearchArea();
        VBox.setVgrow(searchArea, Priority.NEVER);
        
        // 创建结果区域
        VBox resultArea = createResultArea();
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        
        // 创建底部工具栏
        HBox bottomBar = createBottomBar();
        VBox.setVgrow(bottomBar, Priority.NEVER);
        
        // 组装界面
        contentPane.getChildren().addAll(searchArea, resultArea, bottomBar);
        root.getChildren().add(contentPane);
        
        // 创建场景
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/modern-theme.css").toExternalForm());
        
        // 设置主题监听
        setupThemeListener();
        
        // 设置窗口
        primaryStage.setTitle("智能英汉词典");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setScene(scene);
        
        // 添加窗口显示动画
        contentPane.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), contentPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private VBox createSearchArea() {
        VBox searchArea = new VBox(10);
        searchArea.setPrefWidth(Region.USE_COMPUTED_SIZE);
        searchArea.setMinHeight(100);
        searchArea.getStyleClass().add("search-area");
        
        // 创建搜索框和按钮区域
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
        
        Label searchLabel = new Label("请输入词汇:");
        searchLabel.setFont(Font.font("Microsoft YaHei", 14));
        
        searchField = new TextField();
        searchField.setPromptText("输入要查询的单词");
        searchField.setPrefWidth(300);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        // 创建单选按钮组
        ToggleGroup directionGroup = new ToggleGroup();
        englishToChineseBtn = new RadioButton("英译中");
        chineseToEnglishBtn = new RadioButton("中译英");
        englishToChineseBtn.setToggleGroup(directionGroup);
        chineseToEnglishBtn.setToggleGroup(directionGroup);
        englishToChineseBtn.setSelected(true);
        
        // 创建按钮区域
        HBox buttonBox = new HBox(8);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button searchBtn = new Button("查询");
        Button clearBtn = new Button("清除");
        Button addBtn = new Button("添加");
        Button modifyBtn = new Button("修改与删除");
        Button importBtn = new Button("导入词典");
        showFavoritesBtn = new Button("收藏夹");
        
        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        buttonBox.getChildren().addAll(
            searchBtn, clearBtn, separator,
            addBtn, modifyBtn, importBtn, showFavoritesBtn
        );
        
        // 添加事件处理
        searchBtn.setOnAction(e -> searchWord());
        clearBtn.setOnAction(e -> clearResults());
        addBtn.setOnAction(e -> showAddWordDialog());
        modifyBtn.setOnAction(e -> showModifyDeleteDialog());
        importBtn.setOnAction(e -> importDictionary());
        showFavoritesBtn.setOnAction(e -> toggleFavorites());
        
        // 添加回车键搜索
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                searchWord();
            }
        });
        
        // 添加右键菜单
        ContextMenu contextMenu = new ContextMenu();
        MenuItem copyItem = new MenuItem("复制");
        MenuItem pasteItem = new MenuItem("粘贴");
        MenuItem cutItem = new MenuItem("剪切");
        
        copyItem.setOnAction(e -> searchField.copy());
        pasteItem.setOnAction(e -> searchField.paste());
        cutItem.setOnAction(e -> searchField.cut());
        
        contextMenu.getItems().addAll(copyItem, pasteItem, cutItem);
        searchField.setContextMenu(contextMenu);
        
        // 添加快捷键
        searchField.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown()) {
                switch (e.getCode()) {
                                    case C -> searchField.copy();
                                    case V -> searchField.paste();
                                    case X -> searchField.cut();
                                    default -> throw new IllegalArgumentException("Unexpected value: " + e.getCode());
                }
            }
        });
        
        // 组装搜索区域
        searchBox.getChildren().addAll(searchLabel, searchField);
        HBox directionBox = new HBox(20);
        directionBox.getChildren().addAll(englishToChineseBtn, chineseToEnglishBtn);
        
        searchArea.getChildren().addAll(searchBox, directionBox, buttonBox);
        return searchArea;
    }

    private VBox createResultArea() {
        VBox resultArea = new VBox(10);
        resultArea.getStyleClass().add("result-area");
        
        // 创建现代列表视图
        wordList = new ListView<>();
        wordList.setCellFactory(listView -> new WordListCell());
        wordList.setPlaceholder(new Label("暂无数据"));
        
        // 添加列表右键菜单
        ContextMenu listMenu = new ContextMenu();
        MenuItem copyMenuItem = new MenuItem("复制选中内容");
        copyMenuItem.setOnAction(e -> copySelectedContent());
        listMenu.getItems().add(copyMenuItem);
        
        wordList.setContextMenu(listMenu);
        
        // 添加快捷键
        wordList.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.C) {
                copySelectedContent();
            }
        });
        
        resultArea.getChildren().add(wordList);
        VBox.setVgrow(wordList, Priority.ALWAYS);
        
        return resultArea;
    }

    private class WordListCell extends ListCell<Word> {
        private HBox content;
        private Label wordLabel;
        private Label translationLabel;
        private Button pronounceButton;
        private Button favoriteButton;
        private Region spacer;
        
        public WordListCell() {
            super();
            
            content = new HBox(20);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(10, 15, 10, 15));
            content.getStyleClass().add("word-list-item");
            
            wordLabel = new Label();
            wordLabel.getStyleClass().add("word-label");
            wordLabel.setMinWidth(150);
            wordLabel.setPrefWidth(200);
            
            translationLabel = new Label();
            translationLabel.getStyleClass().add("translation-label");
            translationLabel.setMinWidth(300);
            translationLabel.setPrefWidth(400);
            translationLabel.setWrapText(true);
            
            spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            // 创建发音按钮
            pronounceButton = new Button();
            pronounceButton.getStyleClass().addAll("icon-button", "pronounce-button");
            pronounceButton.setGraphic(createSVGImageView("/icons/volume.svg", 18, 18));
            
            // 创建收藏按钮
            favoriteButton = new Button();
            favoriteButton.getStyleClass().addAll("icon-button", "favorite-button");
            updateFavoriteButton(null);
            
            content.getChildren().addAll(wordLabel, translationLabel, spacer, pronounceButton, favoriteButton);
            
            // 添加悬停效果
            setOnMouseEntered(e -> content.getStyleClass().add("word-list-item-hover"));
            setOnMouseExited(e -> content.getStyleClass().remove("word-list-item-hover"));
        }
        
        @Override
        protected void updateItem(Word word, boolean empty) {
            super.updateItem(word, empty);
            
            if (empty || word == null) {
                setGraphic(null);
            } else {
                wordLabel.setText(word.getWord());
                translationLabel.setText(word.getTranslation());
                
                pronounceButton.setOnAction(e -> pronounceWord(word, pronounceButton));
                
                // 更新收藏按钮状态
                updateFavoriteButton(word);
                favoriteButton.setOnAction(e -> {
                    toggleFavorite(word);
                    updateFavoriteButton(word);
                });
                
                setGraphic(content);
            }
        }

        private void updateFavoriteButton(Word word) {
            if (word == null) {
                favoriteButton.setGraphic(createSVGImageView("/icons/favorite_border.svg", 18, 18));
                return;
            }
            
            boolean isFavorite = wordBook != null && wordBook.isFavorite(word);
            String iconPath = isFavorite ? "/icons/favorite.svg" : "/icons/favorite_border.svg";
            favoriteButton.setGraphic(createSVGImageView(iconPath, 18, 18));
        }
    }

    private HBox createBottomBar() {
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        
        Button themeToggle = new Button("切换主题");
        themeToggle.getStyleClass().add("theme-toggle-button");
        themeToggle.setOnAction(e -> toggleTheme());
        
        bottomBar.getChildren().add(themeToggle);
        return bottomBar;
    }

    private void setupThemeListener() {
        darkMode.addListener((obs, oldVal, newVal) -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), contentPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                if (newVal) {
                    root.setStyle("-fx-background-image: url('/bg-dark.png'); " +
                                "-fx-background-position: center center; " +
                                "-fx-background-repeat: no-repeat; " +
                                "-fx-background-size: cover;");
                    scene.getRoot().getStyleClass().add("dark");
                } else {
                    root.setStyle("-fx-background-image: url('/bg.png'); " +
                                "-fx-background-position: center center; " +
                                "-fx-background-repeat: no-repeat; " +
                                "-fx-background-size: cover;");
                    scene.getRoot().getStyleClass().remove("dark");
                }
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), contentPane);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            
            fadeOut.play();
        });
    }

    private void searchWord() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            showAlert("请输入要查询的单词", Alert.AlertType.WARNING);
            return;
        }

        List<Word> similarWords = dictionary.findSimilarWords(text, englishToChineseBtn.isSelected());
        if (!similarWords.isEmpty()) {
            wordList.setItems(FXCollections.observableArrayList(similarWords));
        } else {
            wordList.setItems(FXCollections.observableArrayList());
            showAlert("未找到相似词条", Alert.AlertType.INFORMATION);
        }

        searchField.clear();
    }

    private void clearResults() {
        wordList.setItems(FXCollections.observableArrayList());
        searchField.clear();
    }

    private void showAddWordDialog() {
        AddWordDialogFX dialog = new AddWordDialogFX(primaryStage, dictionary);
        dialog.showAndWait().ifPresent(word -> {
            if (!showingFavorites) {
                List<Word> currentWords = wordList.getItems();
                currentWords.add(word);
                wordList.setItems(FXCollections.observableArrayList(currentWords));
            }
        });
    }

    private void showModifyDeleteDialog() {
        ModifyDeleteDialogFX dialog = new ModifyDeleteDialogFX(primaryStage, dictionary);
        dialog.showAndWait();
        if (!showingFavorites) {
            searchWord();
        }
    }

    private void importDictionary() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择词典文件");
        File file = fileChooser.showOpenDialog(primaryStage);
        
        if (file != null) {
            try {
                List<Word> importedWords = FileIOUtil.readDictionaryFile(file.getPath());
                dictionary.addAll(importedWords);
                FileIOUtil.updateDictionaryFile(FileIOUtil.CSV_PATH, dictionary.getAllWords());
                showAlert("词典导入成功", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("导入词典失败: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void toggleFavorites() {
        showingFavorites = !showingFavorites;
        showFavoritesBtn.setText(showingFavorites ? "返回词典" : "收藏夹");
        
        if (showingFavorites) {
            List<Word> favorites = wordBook.getFavorites();
            wordList.setItems(FXCollections.observableArrayList(favorites));
        } else {
            wordList.setItems(FXCollections.observableArrayList());
        }
    }

    private void toggleFavorite(Word word) {
        if (wordBook.isFavorite(word)) {
            wordBook.removeFavorite(word);
        } else {
            wordBook.addFavorite(word);
        }
        wordList.refresh();
    }

    private void pronounceWord(Word word, Button button) {
        if (word != null) {
            if (button != null) {
                button.setDisable(true);
            }
            
            new Thread(() -> {
                try {
                    TTSUtil.speak(word.getWord());
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("发音失败: " + e.getMessage(), Alert.AlertType.ERROR));
                } finally {
                    Platform.runLater(() -> {
                        if (button != null) {
                            button.setDisable(false);
                        }
                    });
                }
            }).start();
        }
    }

    private void copySelectedContent() {
        Word selectedWord = wordList.getSelectionModel().getSelectedItem();
        if (selectedWord != null) {
            String content = selectedWord.getWord() + ": " + selectedWord.getTranslation();
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(content);
            clipboard.setContent(clipboardContent);
        }
    }

    private void toggleTheme() {
        darkMode.set(!darkMode.get());
    }

    private void showAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(primaryStage);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            TTSUtil.cleanup();
            if (wordBook != null) {
                wordBook.saveToFile(FileIOUtil.FAVORITES_PATH);
            }
        } catch (Exception e) {
            System.err.println("清理资源失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void updateTableView() {
        if (dictionary != null) {
            List<Word> allWords = dictionary.getAllWords();
            wordList.setItems(FXCollections.observableArrayList(allWords));
        }
    }

    private ImageView createSVGImageView(String path, int width, int height) {
        ImageIcon icon = SVGUtil.loadSVGIcon(path.substring(1), width, height, null);
        if (icon != null) {
            BufferedImage bufferedImage = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
            return new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
        }
        return null;
    }
} 