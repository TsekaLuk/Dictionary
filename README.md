# Smart English-Chinese Dictionary

A modern, feature-rich English-Chinese dictionary application built with Java Swing, featuring an intelligent search engine, word morphology support, and a clean, responsive UI with dark/light theme support.

![Dictionary Screenshot](screenshots/main.png)

## Features

### Core Functionality

- **Bidirectional Translation**: Support for both English-to-Chinese and Chinese-to-English lookups
- **Intelligent Search**:
  - Fuzzy matching with advanced similarity algorithms
  - Word morphology recognition (verb conjugations, noun plurals, adjective forms)
  - Context-aware search prioritization
  - Common words highlighting

### Word Morphology Support

- **Verb Forms**:

  - Regular and irregular conjugations
  - Third person singular (play → plays)
  - Past tense (play → played)
  - Past participle (play → played)
  - Present participle (play → playing)
  - Support for irregular verbs (go → went → gone)
- **Noun Forms**:

  - Regular and irregular plurals (cat → cats, child → children)
  - Possessive forms (cat → cat's)
  - Special cases handling (man → men, mouse → mice)
- **Adjective Forms**:

  - Comparative forms (tall → taller)
  - Superlative forms (tall → tallest)
  - Irregular forms (good → better → best)

### Advanced Features

- **Text-to-Speech (TTS)**:

  - High-quality English pronunciation
  - Powered by FreeTTS engine
  - Instant playback with clear articulation
- **Favorites System**:

  - Save and manage favorite words
  - Quick access to frequently used entries
  - Persistent storage across sessions
- **Modern UI/UX**:

  - Clean, intuitive interface
  - Dark/Light theme support with smooth transitions
  - Responsive design with modern aesthetics
  - Custom SVG icons
  - Elegant animations and visual feedback

### Performance Optimization

- **Multi-level Caching**:

  - Query result caching
  - Similarity calculation caching
  - LRU (Least Recently Used) cache management
  - Thread-safe implementation
- **Efficient Search Algorithm**:

  - Weighted similarity calculation
  - Multiple similarity metrics (Cosine, Jaccard, Edit Distance)
  - Optimized for both exact and fuzzy matching

## Technical Details

### Architecture

- **Design Pattern**: MVC (Model-View-Controller)
- **UI Framework**: Java Swing with custom components
- **Build System**: Maven

### Key Components

- **Word Processing**:

  - Jieba Chinese word segmentation
  - Custom morphological analysis
  - Advanced string similarity algorithms
- **Data Management**:

  - CSV-based dictionary storage
  - Efficient data structures for quick lookup
  - Thread-safe cache implementation
- **UI Components**:

  - Custom button and text field renderers
  - Smooth animation system
  - Theme management system

### Dependencies

- Java 23 (or higher)
- Maven 3.x
- Key Libraries:
  - Jieba-analysis: Chinese word segmentation
  - FreeTTS: Text-to-speech functionality
  - Batik: SVG rendering
  - HanLP: Chinese language processing

## Installation

1. Ensure you have Java 23 or higher installed:

   ```bash
   java -version
   ```
2. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/dictionary.git
   cd dictionary
   ```
3. Build with Maven:

   ```bash
   mvn clean package
   ```
4. Run the application:

   ```bash
   java -jar target/Dictionary-1.0-SNAPSHOT.jar
   ```

## Usage

### Basic Operations

1. **Word Lookup**:

   - Enter a word in the search field
   - Select English-to-Chinese or Chinese-to-English mode
   - Press Enter or click "Search"
2. **Word Forms**:

   - Enter any form of a word (e.g., "went")
   - The system will automatically find the base form ("go")
3. **Favorites**:

   - Click the heart icon to add/remove words from favorites
   - Access favorites through the dedicated button
4. **Pronunciation**:

   - Click the play button next to any English word
   - Clear, natural pronunciation will be played

### Theme Switching

- Click the theme toggle button in the bottom-right corner
- Smooth transition between light and dark themes
- Theme preference is preserved across sessions

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/dictionary.git

# Navigate to project directory
cd dictionary

# Build the project
mvn clean package

# Run the application
java -jar target/Dictionary-1.0-SNAPSHOT.jar
```

### Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── dictionary/
│   │           ├── gui/         # UI components
│   │           ├── model/       # Data models
│   │           └── util/        # Utility classes
│   └── resources/
│       ├── icons/              # SVG icons
│       └── CommonWords.csv     # Common words data
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

### Guidelines

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Jieba](https://github.com/huaban/jieba-analysis) for Chinese word segmentation
- [FreeTTS](https://freetts.sourceforge.io/) for text-to-speech functionality
- [Batik](https://xmlgraphics.apache.org/batik/) for SVG rendering
- [HanLP](https://github.com/hankcs/HanLP) for Chinese language processing

## Contact

Mail: zihoi.luk@foxmail.com
