package com.dictionary;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
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
import com.dictionary.gui.DictionaryRepositoryPage;
import com.dictionary.util.TTSUtil;
import com.dictionary.util.SVGUtil;
import com.dictionary.repository.DatabaseHelper;
import com.dictionary.util.ResourceManager;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.beans.binding.Bindings;
import java.lang.ref.WeakReference;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.WeakEventHandler;

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

    // Add lazy loading fields
    private boolean dictionaryLoaded = false;
    private boolean uiInitialized = false;

    // Add pagination fields
    private static final int PAGE_SIZE = 50;
    private final IntegerProperty currentPageProperty = new SimpleIntegerProperty(0);
    private final ObjectProperty<ObservableList<Word>> currentWordListProperty = 
        new SimpleObjectProperty<>(FXCollections.observableArrayList());

    // Add UI optimization fields
    private static final long DEBOUNCE_DELAY_MS = 300;
    private ScheduledExecutorService uiUpdateExecutor;
    private ScheduledFuture<?> searchTask;

    private final ResourceManager resourceManager = ResourceManager.getInstance();

    public DictionaryApp() {
        dictionary = new Dictionary();
        wordBook = new WordBook();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        // Initialize UI update executor
        uiUpdateExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        resourceManager.registerExecutor(uiUpdateExecutor);
        
        // Initialize root container first
        root = new StackPane();
        root.getStyleClass().add("root-pane");
        
        // Create scene
        scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/modern-theme.css").toExternalForm());
        
        // Initialize UI components
        initializeUI();
        createAndShowGUI();
        
        // Set up window
        primaryStage.setTitle("智能英汉词典");
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();
        
        // Initialize TTS in background
        initializeTTS();
        
        // Load dictionary data in background
        loadDictionaryIfNeeded();
        
        // Register main components
        resourceManager.registerNode(root);
        resourceManager.registerNode(contentPane);
        resourceManager.registerNode(wordList);
    }

    private void loadDictionaryIfNeeded() {
        if (!dictionaryLoaded) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> showLoading("正在加载词典..."));
                    
                    // Load dictionary in background
                    dictionary.loadFromFile(FileIOUtil.CSV_PATH);
                    wordBook.loadFromFile(FileIOUtil.FAVORITES_PATH);
                    
                    dictionaryLoaded = true;
                    
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
            
            if (lightStream == null || darkStream == null) {
                System.err.println("Failed to load background images - resource stream is null");
                return;
            }
            
            Image lightImage = new Image(lightStream);
            Image darkImage = new Image(darkStream);
            
            if (lightImage.isError() || darkImage.isError()) {
                System.err.println("Error loading background images");
                return;
            }
            
            // Register images for cleanup
            resourceManager.registerImage(lightImage);
            resourceManager.registerImage(darkImage);
            
            // Create background images
            BackgroundImage lightBg = new BackgroundImage(
                lightImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, false, false)
            );
            
            BackgroundImage darkBg = new BackgroundImage(
                darkImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, false, false)
            );
            
            // Store backgrounds
            resourceManager.registerResource("lightBackground", lightBg);
            resourceManager.registerResource("darkBackground", darkBg);
            
            System.out.println("Background images initialized successfully");
        } catch (Exception e) {
            showAlert("背景图片加载失败: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void createAndShowGUI() {
        // 创建搜索区域
        VBox searchArea = createSearchArea();
        
        // 创建结果区域
        VBox resultArea = createResultArea();
        
        // 创建底部工具栏
        HBox bottomBar = createBottomBar();
        bottomBar.getStyleClass().add("bottom-bar");
        
        // 创建词典仓库页面
        DictionaryRepositoryPage repositoryPage = new DictionaryRepositoryPage(primaryStage);
        repositoryPage.setVisible(false);
        repositoryPage.setOnBackAction(() -> toggleRepositoryPage());
        repositoryPage.setDictionary(dictionary);
        
        // 创建主要内容区域
        VBox mainContent = new VBox(10);
        mainContent.setPadding(new Insets(10));
        mainContent.getChildren().addAll(searchArea, resultArea);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        
        // 创建主布局容器
        BorderPane mainLayout = new BorderPane();
        mainLayout.setCenter(mainContent);
        mainLayout.setBottom(bottomBar);
        mainLayout.getStyleClass().add("main-layout");
        
        // 使用已存在的root
        root.getChildren().addAll(mainLayout, repositoryPage);
        
        // 设置背景
        setupBackground();
        
        // 设置主题监听
        setupThemeListener();
    }

    private void setupBackground() {
        root.setStyle(
            "-fx-background-image: url('/bg.png'); " +
            "-fx-background-size: cover; " +
            "-fx-background-position: center; " +
            "-fx-background-repeat: no-repeat; " +
            "-fx-background-color: transparent;"
        );
    }

    private VBox createSearchArea() {
        VBox searchArea = new VBox();
        searchArea.setSpacing(10);
        searchArea.getStyleClass().add("search-area");
        searchArea.setPadding(new Insets(10));
        searchArea.setMinHeight(200);  // 设置最小高度
        searchArea.setPrefHeight(200);  // 设置首选高度
        searchArea.setMaxHeight(200);  // 设置最大高度
        VBox.setVgrow(searchArea, Priority.NEVER);  // 防止被拉伸
        
        // 创建搜索框和按钮区域
        HBox searchBox = new HBox();
        searchBox.setSpacing(10);  // 增加间距
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPadding(new Insets(5));  // 添加内边距
        HBox.setHgrow(searchBox, Priority.ALWAYS);
        
        Label searchLabel = new Label("请输入词汇:");
        searchLabel.setFont(Font.font("Microsoft YaHei", 14));
        searchLabel.setMinWidth(Region.USE_PREF_SIZE);
        
        searchField = new TextField();
        searchField.setPromptText("输入要查询的单词");
        searchField.setPrefWidth(300);  // 设置首选宽度
        HBox.setHgrow(searchField, Priority.ALWAYS);
        
        // 创建单选按钮组
        ToggleGroup directionGroup = new ToggleGroup();
        englishToChineseBtn = new RadioButton("英译中");
        chineseToEnglishBtn = new RadioButton("中译英");
        englishToChineseBtn.setToggleGroup(directionGroup);
        chineseToEnglishBtn.setToggleGroup(directionGroup);
        englishToChineseBtn.setSelected(true);
        
        HBox directionBox = new HBox();
        directionBox.setSpacing(20);  // 增加间距
        directionBox.setAlignment(Pos.CENTER_LEFT);
        directionBox.setPadding(new Insets(5));  // 添加内边距
        directionBox.getChildren().addAll(englishToChineseBtn, chineseToEnglishBtn);
        
        // 创建按钮区域
        HBox buttonBox = new HBox();
        buttonBox.setSpacing(10);  // 增加间距
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(5));  // 添加内边距
        
        // 创建按钮组
        HBox searchGroup = new HBox();
        searchGroup.setSpacing(10);  // 增加间距
        Button searchBtn = new Button("查询");
        Button clearBtn = new Button("清除");
        searchGroup.getChildren().addAll(searchBtn, clearBtn);
        
        Separator separator1 = new Separator(Orientation.VERTICAL);
        separator1.setPadding(new Insets(0, 10, 0, 10));  // 添加水平内边距
        
        HBox editGroup = new HBox();
        editGroup.setSpacing(10);  // 增加间距
        Button addBtn = new Button("添加");
        Button modifyBtn = new Button("修改与删除");
        editGroup.getChildren().addAll(addBtn, modifyBtn);
        
        Separator separator2 = new Separator(Orientation.VERTICAL);
        separator2.setPadding(new Insets(0, 10, 0, 10));  // 添加水平内边距
        
        HBox utilGroup = new HBox();
        utilGroup.setSpacing(10);  // 增加间距
        Button importBtn = new Button("导入词典");
        showFavoritesBtn = new Button("收藏夹");
        utilGroup.getChildren().addAll(importBtn, showFavoritesBtn);
        
        buttonBox.getChildren().addAll(
            searchGroup, separator1,
            editGroup, separator2,
            utilGroup
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
                    default -> {}
                }
            }
        });
        
        // Add debounced search
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (searchTask != null) {
                searchTask.cancel(false);
            }
            
            if (!newText.trim().isEmpty()) {
                searchTask = uiUpdateExecutor.schedule(
                    () -> Platform.runLater(() -> searchWord()),
                    DEBOUNCE_DELAY_MS,
                    TimeUnit.MILLISECONDS
                );
            }
        });
        
        // 组装搜索区域
        searchBox.getChildren().addAll(searchLabel, searchField);
        searchArea.getChildren().addAll(searchBox, directionBox, buttonBox);
        
        return searchArea;
    }

    private VBox createResultArea() {
        VBox resultArea = new VBox();
        resultArea.setSpacing(10);
        resultArea.getStyleClass().add("result-area");
        resultArea.setFillWidth(true);
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        
        // Create modern list view with virtualization
        wordList = new ListView<>();
        wordList.setStyle("-fx-fixed-cell-size: 50;"); // Enable virtualization
        
        // Use custom cell factory with recycling
        wordList.setCellFactory(listView -> {
            WordListCell cell = new WordListCell();
            cell.setPrefHeight(Region.USE_COMPUTED_SIZE);
            return cell;
        });
        
        // Enable smooth scrolling
        ScrollPane scrollPane = new ScrollPane(wordList);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        wordList.setPlaceholder(new Label("暂无数据"));
        wordList.setFocusTraversable(false);
        
        // Add pagination controls with optimized updates
        HBox paginationBox = createPaginationControls();
        
        resultArea.getChildren().addAll(scrollPane, paginationBox);
        return resultArea;
    }

    private HBox createPaginationControls() {
        HBox paginationBox = new HBox(10);
        paginationBox.setAlignment(Pos.CENTER);
        
        Button prevButton = new Button("上一页");
        Button nextButton = new Button("下一页");
        Label pageLabel = new Label();
        
        // Add button states
        prevButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> currentPageProperty.get() == 0,
                currentPageProperty
            )
        );
        
        nextButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> {
                    int totalPages = (int) Math.ceil((double) currentWordListProperty.get().size() / PAGE_SIZE);
                    return currentPageProperty.get() >= totalPages - 1;
                },
                currentPageProperty,
                currentWordListProperty
            )
        );
        
        // Use event filters for better performance
        prevButton.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> showPreviousPage());
        nextButton.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> showNextPage());
        
        paginationBox.getChildren().addAll(prevButton, pageLabel, nextButton);
        return paginationBox;
    }

    private class WordListCell extends ListCell<Word> {
        private HBox content;
        private Label wordLabel;
        private Label translationLabel;
        private Button pronounceButton;
        private Button favoriteButton;
        private WeakReference<Word> wordRef;
        
        public WordListCell() {
            super();
            
            content = new HBox();
            content.setSpacing(5);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(5));
            content.getStyleClass().add("word-list-item");
            HBox.setHgrow(content, Priority.ALWAYS);
            
            wordLabel = new Label();
            wordLabel.getStyleClass().add("word-label");
            wordLabel.setMinWidth(100);
            wordLabel.setPrefWidth(150);
            wordLabel.setMaxWidth(200);
            HBox.setHgrow(wordLabel, Priority.NEVER);
            
            translationLabel = new Label();
            translationLabel.getStyleClass().add("translation-label");
            translationLabel.setWrapText(true);
            HBox.setHgrow(translationLabel, Priority.ALWAYS);
            
            // 创建按钮容器
            HBox buttonContainer = new HBox();
            buttonContainer.setSpacing(5);
            buttonContainer.setAlignment(Pos.CENTER_RIGHT);
            buttonContainer.setMinWidth(70);
            buttonContainer.setPrefWidth(70);
            buttonContainer.setMaxWidth(70);
            HBox.setHgrow(buttonContainer, Priority.NEVER);
            
            // 创建发音按钮
            pronounceButton = new Button();
            pronounceButton.getStyleClass().addAll("icon-button", "pronounce-button");
            pronounceButton.setGraphic(createSVGImageView("/icons/volume.svg", 16, 16));
            
            // 创建收藏按钮
            favoriteButton = new Button();
            favoriteButton.getStyleClass().addAll("icon-button", "favorite-button");
            updateFavoriteButton(null);
            
            buttonContainer.getChildren().addAll(pronounceButton, favoriteButton);
            
            content.getChildren().addAll(wordLabel, translationLabel, buttonContainer);
            
            // 添加悬停效果
            setOnMouseEntered(e -> content.getStyleClass().add("word-list-item-hover"));
            setOnMouseExited(e -> content.getStyleClass().remove("word-list-item-hover"));
            
            // Register cell components
            resourceManager.registerNode(content);
            resourceManager.registerNode(wordLabel);
            resourceManager.registerNode(translationLabel);
            resourceManager.registerNode(pronounceButton);
            resourceManager.registerNode(favoriteButton);
        }
        
        @Override
        protected void updateItem(Word word, boolean empty) {
            Word oldWord = wordRef != null ? wordRef.get() : null;
            if (oldWord != null) {
                // Clean up old references
                cleanupOldReferences(oldWord);
            }
            
            super.updateItem(word, empty);
            
            if (empty || word == null) {
                setGraphic(null);
                wordRef = null;
            } else {
                wordRef = new WeakReference<>(word);
                wordLabel.setText(word.getWord());
                translationLabel.setText(word.getTranslation());
                
                // Use weak event handlers
                pronounceButton.setOnAction(new WeakEventHandler<>(e -> pronounceWord(word, pronounceButton)));
                favoriteButton.setOnAction(new WeakEventHandler<>(e -> {
                    toggleFavorite(word);
                    updateFavoriteButton(word);
                }));
                
                setGraphic(content);
            }
        }
        
        private void cleanupOldReferences(Word oldWord) {
            // Remove event handlers
            pronounceButton.setOnAction(null);
            favoriteButton.setOnAction(null);
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
        HBox bottomBar = new HBox();
        bottomBar.setSpacing(10);  // 增加按钮间距
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10));
        bottomBar.setStyle("-fx-background-color: transparent;");
        
        Button repositoryButton = new Button("词典仓库");
        repositoryButton.setMinWidth(100);
        repositoryButton.setPrefWidth(100);
        repositoryButton.setMaxWidth(100);
        repositoryButton.setMinHeight(30);
        repositoryButton.setPrefHeight(30);
        repositoryButton.setMaxHeight(30);
        repositoryButton.setOnAction(e -> toggleRepositoryPage());
        
        Button themeToggle = new Button("切换主题");
        themeToggle.setMinWidth(100);
        themeToggle.setPrefWidth(100);
        themeToggle.setMaxWidth(100);
        themeToggle.setMinHeight(30);
        themeToggle.setPrefHeight(30);
        themeToggle.setMaxHeight(30);
        themeToggle.getStyleClass().add("theme-toggle-button");
        themeToggle.setOnAction(e -> toggleTheme());
        
        bottomBar.getChildren().addAll(repositoryButton, themeToggle);
        return bottomBar;
    }

    private void setupThemeListener() {
        darkMode.addListener((obs, oldVal, newVal) -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), contentPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            
            fadeOut.setOnFinished(e -> {
                if (newVal) {
                    root.setStyle(
                        "-fx-background-image: url('/bg-dark.png'); " +
                        "-fx-background-size: cover; " +
                        "-fx-background-position: center; " +
                        "-fx-background-repeat: no-repeat; " +
                        "-fx-background-color: transparent;"
                    );
                    scene.getRoot().getStyleClass().add("dark");
                } else {
                    root.setStyle(
                        "-fx-background-image: url('/bg.png'); " +
                        "-fx-background-size: cover; " +
                        "-fx-background-position: center; " +
                        "-fx-background-repeat: no-repeat; " +
                        "-fx-background-color: transparent;"
                    );
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

        loadDictionaryIfNeeded();

        if (!dictionaryLoaded) {
            showAlert("词典正在加载中，请稍候...", Alert.AlertType.INFORMATION);
            return;
        }

        currentWordListProperty.get().clear();
        currentWordListProperty.get().addAll(dictionary.findSimilarWords(text, englishToChineseBtn.isSelected()));
        if (!currentWordListProperty.get().isEmpty()) {
            currentPageProperty.set(0);
            showPage(0);
        } else {
            currentWordListProperty.get().clear();
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
            // Use ResourceManager to clean up all resources
            resourceManager.cleanup();
            
            // Additional cleanup
            TTSUtil.cleanup();
            if (wordBook != null) {
                wordBook.saveToFile(FileIOUtil.FAVORITES_PATH);
            }
            DatabaseHelper.getInstance().closePool();
        } catch (Exception e) {
            System.err.println("清理资源失败: " + e.getMessage());
        }
    }

    private void clearEventHandlers() {
        if (searchField != null) searchField.setOnKeyPressed(null);
        if (wordList != null) {
            wordList.setOnMouseClicked(null);
            wordList.setItems(null);
        }
        // Clear other event handlers...
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void updateTableView() {
        if (dictionary != null) {
            // Use batch loading for large datasets
            new Thread(() -> {
                List<Word> allWords = dictionary.getAllWords();
                int totalSize = allWords.size();
                int batchSize = 100;
                
                for (int i = 0; i < totalSize; i += batchSize) {
                    final int currentIndex = i;
                    int start = currentIndex;
                    int end = Math.min(start + batchSize, totalSize);
                    List<Word> batch = allWords.subList(start, end);
                    
                    Platform.runLater(() -> {
                        if (currentIndex == 0) {
                            wordList.setItems(FXCollections.observableArrayList(batch));
                        } else {
                            wordList.getItems().addAll(batch);
                        }
                    });
                    
                    try {
                        Thread.sleep(10); // Small delay to prevent UI freezing
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }).start();
        }
    }

    private ImageView createSVGImageView(String path, int width, int height) {
        ImageIcon icon = SVGUtil.loadSVGIcon(path.substring(1), width, height, null);
        if (icon != null) {
            BufferedImage bufferedImage = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
            ImageView imageView = new ImageView(SwingFXUtils.toFXImage(bufferedImage, null));
            resourceManager.registerNode(imageView);
            return imageView;
        }
        return null;
    }

    private void toggleRepositoryPage() {
        // 获取词典仓库页面和主布局
        DictionaryRepositoryPage repositoryPage = null;
        Node mainLayout = null;
        
        // 安全地获取组件
        for (Node node : root.getChildren()) {
            if (node instanceof DictionaryRepositoryPage) {
                repositoryPage = (DictionaryRepositoryPage) node;
            } else if (node instanceof BorderPane) {
                mainLayout = node;
            }
        }
        
        if (repositoryPage == null || mainLayout == null) {
            return;
        }
        
        // 禁用加载动画
        loadingOverlay.setVisible(false);
        
        final Node finalMainLayout = mainLayout;
        final DictionaryRepositoryPage finalRepositoryPage = repositoryPage;
        
        if (repositoryPage.isVisible()) {
            // 切换回主页面
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), finalRepositoryPage);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                finalRepositoryPage.setVisible(false);
                finalMainLayout.setVisible(true);
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), finalMainLayout);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } else {
            // 切换到词典仓库页面
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), finalMainLayout);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                finalMainLayout.setVisible(false);
                finalRepositoryPage.setVisible(true);
                
                // 更新词典仓库页面数据
                finalRepositoryPage.updateDictionaryData();
                
                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), finalRepositoryPage);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    private void showPage(int page) {
        if (currentWordListProperty.get().isEmpty()) {
            return;
        }
        
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, currentWordListProperty.get().size());
        
        if (start >= 0 && start < currentWordListProperty.get().size()) {
            currentPageProperty.set(page);
            List<Word> pageItems = currentWordListProperty.get().subList(start, end);
            wordList.setItems(FXCollections.observableArrayList(pageItems));
            updatePageLabel();
        }
    }

    private void showNextPage() {
        showPage(currentPageProperty.get() + 1);
    }

    private void showPreviousPage() {
        if (currentPageProperty.get() > 0) {
            showPage(currentPageProperty.get() - 1);
        }
    }

    private void updatePageLabel() {
        int totalPages = (int) Math.ceil((double) currentWordListProperty.get().size() / PAGE_SIZE);
        HBox paginationBox = (HBox) ((VBox) wordList.getParent()).getChildren().get(1);
        Label pageLabel = (Label) paginationBox.getChildren().get(1);
        pageLabel.setText(String.format("第 %d/%d 页", currentPageProperty.get() + 1, totalPages));
    }
} 