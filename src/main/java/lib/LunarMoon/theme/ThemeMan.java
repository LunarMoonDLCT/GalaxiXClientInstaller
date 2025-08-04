package lib.LunarMoon.theme;

import javax.swing.*;

public class ThemeMan {
    public static void Apply() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("[ThemeMan] Không thể áp dụng LookAndFeel: " + e.getMessage());
        }
    }
}
