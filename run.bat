@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-23
set JAVAFX_PATH=%USERPROFILE%\.m2\repository\org\openjfx
set JAVAFX_VERSION=21.0.1

set MODULE_PATH=%JAVAFX_PATH%\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%-win.jar;^
%JAVAFX_PATH%\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%-win.jar;^
%JAVAFX_PATH%\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%-win.jar;^
%JAVAFX_PATH%\javafx-fxml\%JAVAFX_VERSION%\javafx-fxml-%JAVAFX_VERSION%-win.jar;^
%JAVAFX_PATH%\javafx-media\%JAVAFX_VERSION%\javafx-media-%JAVAFX_VERSION%-win.jar;^
%JAVAFX_PATH%\javafx-swing\%JAVAFX_VERSION%\javafx-swing-%JAVAFX_VERSION%-win.jar;^
%JAVAFX_PATH%\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%.jar;^
%JAVAFX_PATH%\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%.jar;^
%JAVAFX_PATH%\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%.jar;^
%JAVAFX_PATH%\javafx-fxml\%JAVAFX_VERSION%\javafx-fxml-%JAVAFX_VERSION%.jar;^
%JAVAFX_PATH%\javafx-media\%JAVAFX_VERSION%\javafx-media-%JAVAFX_VERSION%.jar;^
%JAVAFX_PATH%\javafx-swing\%JAVAFX_VERSION%\javafx-swing-%JAVAFX_VERSION%.jar

"%JAVA_HOME%\bin\java.exe" ^
--module-path "%MODULE_PATH%" ^
--add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.swing ^
--add-opens javafx.graphics/javafx.scene=ALL-UNNAMED ^
--add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED ^
-jar target\Dictionary-1.0-SNAPSHOT.jar