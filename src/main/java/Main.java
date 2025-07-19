public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            System.out.println("Đang khởi chạy Main");
            System.out.println("Đang load Galaxy Client Installer 0.2.2");
            new MainUI().setVisible(true);
            
        });
    }
}
