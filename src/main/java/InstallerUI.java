import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class InstallerUI extends JFrame {
    private JComboBox<String> versionCombo;
    private JTextField pathField;
    private JCheckBox profileCheckbox;
    private JButton installButton;
    private JButton browseButton;

    private String downloadUrl = null;

    public InstallerUI() {
        setTitle("Galaxy Client Installer");
        setSize(500, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        fetchLatestRelease();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Phiên bản
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        panel.add(new JLabel("Phiên bản:"), gbc);

        versionCombo = new JComboBox<>(new String[]{"Đang tải..."});
        versionCombo.setEnabled(false);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        panel.add(versionCombo, gbc);

        // Đường dẫn Minecraft
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Đường dẫn Minecraft:"), gbc);

        pathField = new JTextField(getDefaultMinecraftPath());
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(pathField, gbc);

        // Nút chọn folder
        browseButton = new JButton("...");
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(browseButton, gbc);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int option = chooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                pathField.setText(selected.getAbsolutePath());
            }
        });

        // Checkbox tạo hồ sơ
        profileCheckbox = new JCheckBox("Tạo hồ sơ trong launcher");
        profileCheckbox.setSelected(true);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(profileCheckbox, gbc);

        // Nút Cài đặt
        installButton = new JButton("Cài đặt");
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(installButton, gbc);

        installButton.addActionListener(e -> onInstall());

        add(panel);
    }

    private void fetchLatestRelease() {
        new Thread(() -> {
            try {
                downloadUrl = GitHubFetcher.getLatestZipUrl();
                String versionName = downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1).replace(".zip", "");
                SwingUtilities.invokeLater(() -> {
                    versionCombo.removeAllItems();
                    versionCombo.addItem(versionName);
                    versionCombo.setEnabled(true);
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể lấy bản mới nhất từ GitHub:\n" + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }).start();
    }

    private void onInstall() {
        if (downloadUrl == null) {
            JOptionPane.showMessageDialog(this, "Chưa có link tải.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String installPath = pathField.getText();
        if (installPath == null || installPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thư mục cài đặt.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        installButton.setEnabled(false);
        installButton.setText("Đang cài...");

        new Thread(() -> {
            try {
                String versionName = (String) versionCombo.getSelectedItem();
                File zip = GitHubFetcher.downloadZip(downloadUrl, versionName);
                ZipExtractor.extract(zip, new File(installPath));

                // Optional: Tạo hồ sơ ở đây nếu cần
                if (profileCheckbox.isSelected()) {
                    // TODO: Ghi vào launcher_profiles.json nếu cần
                }

                SwingUtilities.invokeLater(() -> {
                    installButton.setEnabled(true);
                    installButton.setText("Cài đặt");
                    JOptionPane.showMessageDialog(this, "Cài đặt thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    installButton.setEnabled(true);
                    installButton.setText("Cài đặt");
                    JOptionPane.showMessageDialog(this, "Cài đặt thất bại:\n" + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private String getDefaultMinecraftPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String user = System.getProperty("user.name");

        if (os.contains("win")) {
            return "C:\\Users\\" + user + "\\AppData\\Roaming\\.minecraft";
        } else if (os.contains("mac")) {
            return System.getProperty("user.home") + "/Library/Application Support/minecraft";
        } else {
            return System.getProperty("user.home") + "/.minecraft";
        }
    }
}
