import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.Map.Entry;

public class MainUI extends JFrame {
    private JComboBox<String> versionCombo;
    private JTextField pathField;
    private JCheckBox profileCheckbox;
    private JButton installButton;
    private JButton browseButton;

    private Map<String, String> versionMap;

    public MainUI() {
        setTitle("Galaxy Client Installer");
        setSize(450, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI();
        fetchReleases();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Phiên bản client
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

        // Nút chọn chỗ chứa minecraft
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

        // Checkbox tạo hồ sơ để trưng
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

    private void fetchReleases() {
        new Thread(() -> {
            try {
                Map<String, String> rawMap = GitHubdownloadfile.getAllZipReleases();

                // Sắp xếp các phiên bản
                java.util.List<Entry<String, String>> sortedList = new java.util.ArrayList<>(rawMap.entrySet());

                sortedList.sort((e1, e2) -> compareVersions(e2.getKey(), e1.getKey()));

                versionMap = new LinkedHashMap<>();
                for (Entry<String, String> entry : sortedList) {
                    versionMap.put(entry.getKey(), entry.getValue());
                }

                SwingUtilities.invokeLater(() -> {
                    versionCombo.removeAllItems();
                    for (String version : versionMap.keySet()) {
                        versionCombo.addItem(version);
                    }
                    versionCombo.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Không thể lấy danh sách phiên bản từ GitHub:\n" + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void onInstall() {
        String versionName = (String) versionCombo.getSelectedItem();
        if (versionName == null || versionMap == null || !versionMap.containsKey(versionName)) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phiên bản hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String downloadUrl = versionMap.get(versionName);
        String installPath = pathField.getText();
        if (installPath == null || installPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn thư mục cài đặt.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        installButton.setEnabled(false);
        installButton.setText("Đang cài...");

        new Thread(() -> {
            try {
                File zip = GitHubdownloadfile.downloadZip(downloadUrl, versionName);
                ZipExtractor.extract(zip, new File(installPath));

                if (profileCheckbox.isSelected()) {
                    // Ghi vào launcher_profiles.json
                }

                SwingUtilities.invokeLater(() -> {
                    installButton.setEnabled(true);
                    installButton.setText("Cài đặt");
                    JOptionPane.showMessageDialog(this, "Cài đặt thành công!", "Đã cài đặt thành công", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    installButton.setEnabled(true);
                    installButton.setText("Cài đặt");
                    JOptionPane.showMessageDialog(this, "Cài đặt thất bại:\n" + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
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

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.replace("GalaxyClient-", "").split("\\.");
        String[] parts2 = v2.replace("GalaxyClient-", "").split("\\.");

        int len = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < len; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) return Integer.compare(num1, num2);
        }
        return 0;
    }
}
