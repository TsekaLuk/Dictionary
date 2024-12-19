# Dictionary Application

A modern, feature-rich dictionary application built with JavaFX, designed to help users efficiently manage and learn vocabulary.

## Features

- 📚 Comprehensive word management
- 🔍 Advanced search with fuzzy matching
- ⭐ Favorites system
- 🎯 Word form variations
- 🎨 Modern UI with theme support
- 🔊 Text-to-Speech capability

## Technology Stack

- Java 23
- JavaFX
- Maven
- SQLite (for data storage)
- Text-to-Speech API

## Getting Started

### Prerequisites

- Java 23 (with JavaFX included)
  - Download from [Oracle JDK 23](https://www.oracle.com/java/technologies/downloads/#java23)
  - Or use [OpenJDK 23](https://jdk.java.net/23/)
- Maven 3.6.3 or higher
- Git

### Installation

1. Clone the repository

```bash
git clone https://github.com/TsekaLuk/Dictionary.git
```

2. Navigate to the project directory

```bash
cd Dictionary
```

3. Build the project

```bash
mvn clean package
```

4. Run the application

For Windows users, simply use the provided batch file:

```bash
run.bat
```

For other operating systems or manual execution, use:

```bash
mvn javafx:run
```

Note: Direct `java -jar` execution is not supported due to JavaFX module requirements. Please use one of the methods above to run the application.

## Usage

- Add new words with definitions
- Search for words using fuzzy matching
- Mark words as favorites
- View word variations and forms
- Switch between light and dark themes
- Use text-to-speech for pronunciation

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Thanks to all contributors
- Inspired by modern dictionary applications
- Built with ❤️ using JavaFX
