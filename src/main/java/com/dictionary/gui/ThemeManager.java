package com.dictionary.gui;

import javax.swing.*;
import java.awt.*;

public class ThemeManager {
    private static boolean isDarkMode = false;
    private static float themeTransitionProgress = 0.0f; // 0.0 = 浅色, 1.0 = 深色
    private static Timer transitionTimer;
    
    // 浅色主题颜色
    public static final Color LIGHT_BACKGROUND = new Color(255, 255, 255, 100);
    public static final Color LIGHT_BUTTON = new Color(229, 255, 204, 168);          // 浅色按钮（淡绿色，66%透明度）
    public static final Color LIGHT_TEXT = new Color(0, 0, 0);                  // 纯黑色文字
    public static final Color LIGHT_GRID = new Color(230, 230, 230, 100);
    public static final Color LIGHT_SELECTION = new Color(135, 206, 250, 100);
    public static final Color LIGHT_HEADER = new Color(240, 240, 240, 150);
    public static final Color LIGHT_SCROLLBAR = new Color(200, 200, 200, 150);
    
    // 深色主题颜色 - 优化后的配色
    public static final Color DARK_BACKGROUND = new Color(33, 33, 37, 200);     // 深色背景
    public static final Color DARK_BUTTON = new Color(45, 45, 50, 168);              // 深灰色按钮，66%透明度
    public static final Color DARK_TEXT = new Color(255, 255, 255);             // 纯白色文字
    public static final Color DARK_GRID = new Color(80, 80, 90, 100);          // 更明显的网格线
    public static final Color DARK_SELECTION = new Color(100, 140, 200, 150);   // 更明显的选中色
    public static final Color DARK_HEADER = new Color(45, 45, 50, 200);         // 更深的表头
    public static final Color DARK_SCROLLBAR = new Color(78, 89, 140, 150);     // 匹配按钮颜色

    public static boolean isDarkMode() {
        return isDarkMode;
    }

    public static void toggleTheme(Runnable onComplete) {
        if (transitionTimer != null && transitionTimer.isRunning()) {
            transitionTimer.stop();
        }

        isDarkMode = !isDarkMode;
        final float targetProgress = isDarkMode ? 1.0f : 0.0f;
        final float startProgress = isDarkMode ? 0.0f : 1.0f;
        themeTransitionProgress = startProgress;
        
        final int STEPS = 20; // 动画步数
        final int INTERVAL = 16; // 每步时间间隔(ms)
        final float stepSize = (targetProgress - startProgress) / STEPS;
        final int[] currentStep = {0};

        transitionTimer = new Timer(INTERVAL, null);
        transitionTimer.addActionListener(e -> {
            currentStep[0]++;
            themeTransitionProgress = startProgress + stepSize * currentStep[0];
            
            if (currentStep[0] >= STEPS) {
                themeTransitionProgress = targetProgress;
                transitionTimer.stop();
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
        transitionTimer.start();
    }

    public static Color getButtonColor() {
        Color baseColor = isDarkMode ? DARK_BUTTON : LIGHT_BUTTON;
        // 确保返回的颜色保持66%的透明度
        return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 168);
    }

    public static Color interpolateColor(Color c1, Color c2) {
        float t = themeTransitionProgress;
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) (c1.getRed() * (1 - t) + c2.getRed() * t),
            (int) (c1.getGreen() * (1 - t) + c2.getGreen() * t),
            (int) (c1.getBlue() * (1 - t) + c2.getBlue() * t),
            (int) (c1.getAlpha() * (1 - t) + c2.getAlpha() * t)
        );
    }

    public static Color getBackgroundColor() {
        return interpolateColor(LIGHT_BACKGROUND, DARK_BACKGROUND);
    }

    public static Color getTextColor() {
        return interpolateColor(LIGHT_TEXT, DARK_TEXT);
    }

    public static Color getGridColor() {
        return interpolateColor(LIGHT_GRID, DARK_GRID);
    }

    public static Color getSelectionColor() {
        return interpolateColor(LIGHT_SELECTION, DARK_SELECTION);
    }

    public static Color getHeaderColor() {
        return interpolateColor(LIGHT_HEADER, DARK_HEADER);
    }

    public static Color getScrollbarColor() {
        return interpolateColor(LIGHT_SCROLLBAR, DARK_SCROLLBAR);
    }

    public static float getThemeProgress() {
        return themeTransitionProgress;
    }
} 