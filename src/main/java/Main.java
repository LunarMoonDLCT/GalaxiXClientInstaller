import lib.LunarMoon.theme.ThemeMan;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Đang khởi chạy Main");
            ThemeMan.Apply();
        } catch (Exception e) {
            System.err.println("[Theme] Không thể load theme: " + e.getMessage());
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            System.out.println("Đang load Galaxy Client Installer 0.2.6");
            new MainUI().setVisible(true);
        });
    }
}
