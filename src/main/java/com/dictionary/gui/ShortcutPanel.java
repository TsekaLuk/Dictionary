package com.dictionary.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ShortcutPanel extends JPanel {
    private final Map<String, String> shortcuts;
    private final JPanel contentPanel;

    public ShortcutPanel() {
        setOpaque(false);
        setBorder(new EmptyBorder(10, 20, 10, 20));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 0));

        // Initialize shortcuts map with key-value pairs
        shortcuts = new LinkedHashMap<>();
        shortcuts.put("Enter", "搜索");
        shortcuts.put("Esc", "清空输入");
        shortcuts.put("Ctrl+C", "复制");
        shortcuts.put("Ctrl+V", "粘贴");
        shortcuts.put("Ctrl+X", "剪切");

        // Create content panel
        contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw semi-transparent background
                g2.setColor(new Color(255, 255, 255, ThemeManager.isDarkMode() ? 10 : 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Draw border
                g2.setColor(new Color(200, 200, 200, ThemeManager.isDarkMode() ? 30 : 50));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                
                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Add title
        JLabel titleLabel = new JLabel("快捷键");
        titleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setForeground(ThemeManager.isDarkMode() ? ThemeManager.DARK_TEXT : Color.BLACK);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(10));

        // Add shortcuts
        for (Map.Entry<String, String> entry : shortcuts.entrySet()) {
            addShortcutItem(entry.getKey(), entry.getValue());
            contentPanel.add(Box.createVerticalStrut(8));
        }

        add(contentPanel, BorderLayout.NORTH);
    }

    private void addShortcutItem(String key, String description) {
        JPanel itemPanel = new JPanel();
        itemPanel.setOpaque(false);
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.X_AXIS));
        itemPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Key label with custom background
        JLabel keyLabel = new JLabel(key) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw background
                g2.setColor(ThemeManager.getButtonColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        keyLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        keyLabel.setForeground(ThemeManager.isDarkMode() ? ThemeManager.DARK_TEXT : Color.BLACK);
        keyLabel.setBorder(new EmptyBorder(2, 6, 2, 6));
        
        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        descLabel.setForeground(ThemeManager.isDarkMode() ? ThemeManager.DARK_TEXT : Color.BLACK);

        itemPanel.add(keyLabel);
        itemPanel.add(Box.createHorizontalStrut(10));
        itemPanel.add(descLabel);
        itemPanel.add(Box.createHorizontalGlue());

        contentPanel.add(itemPanel);
    }

    public void updateTheme() {
        // Update all components' colors
        for (Component comp : contentPanel.getComponents()) {
            if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(ThemeManager.isDarkMode() ? ThemeManager.DARK_TEXT : Color.BLACK);
            } else if (comp instanceof JPanel) {
                for (Component innerComp : ((JPanel) comp).getComponents()) {
                    if (innerComp instanceof JLabel) {
                        ((JLabel) innerComp).setForeground(ThemeManager.isDarkMode() ? ThemeManager.DARK_TEXT : Color.BLACK);
                    }
                }
            }
        }
        repaint();
    }
} 