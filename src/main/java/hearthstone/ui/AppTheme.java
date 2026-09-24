package hearthstone.ui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public final class AppTheme {

    public static final Color BACKGROUND = new Color(18, 21, 28);
    public static final Color PANEL = new Color(31, 34, 43);
    public static final Color PANEL_LIGHT = new Color(45, 48, 58);
    public static final Color GOLD = new Color(235, 181, 86);
    public static final Color GOLD_DARK = new Color(151, 92, 39);
    public static final Color TEXT = new Color(248, 241, 222);
    public static final Color MUTED = new Color(190, 178, 151);
    public static final Color BLUE = new Color(55, 151, 211);
    public static final Color RED = new Color(205, 76, 59);
    public static final Color GREEN = new Color(76, 165, 105);

    private AppTheme() {
    }

    public static Border roundedLine(Color color, int thickness) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, thickness, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        );
    }

    public static void stylePrimaryButton(AbstractButton button) {
        button.setBackground(GOLD);
        button.setForeground(new Color(43, 28, 14));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 14f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 20, 11, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleSecondaryButton(AbstractButton button) {
        button.setBackground(PANEL_LIGHT);
        button.setForeground(TEXT);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setFocusPainted(false);
        button.setBorder(roundedLine(new Color(90, 91, 98), 1));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
