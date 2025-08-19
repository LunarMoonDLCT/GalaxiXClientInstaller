// LunarMoon was here
import lib.LunarMoon.theme.ThemeMan;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

public class MainUI extends JFrame {
    private JComboBox<String> versionCombo;
    private JComboBox<String> buildCombo;
    private JTextField pathField;
    private JCheckBox profileCheckbox;
    private JButton installButton;
    private JButton browseButton;
    private Map<String, String> versionMap;
    private Map<String, List<String>> buildMap;
    private JProgressBar progressBar;

    public MainUI() {
        try (InputStream is = getClass().getResourceAsStream("/GC/assets/icon/galaxy2.png")) {
            if (is != null) {
                Image icon = ImageIO.read(is);
                setIconImage(icon);
            }
        } catch (Exception e) {}

        // Debug spam windows        SwingUtilities.invokeLater(() -> new MainUI().setVisible(true));
        System.out.println("MainUI: Hiện cửa sổ");
        setTitle(Langdetect.get("title"));
        setSize(450, 230);
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

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        panel.add(new JLabel(Langdetect.get("label.version")), gbc);

        versionCombo = new JComboBox<>(new String[]{Langdetect.get("loading")});
        versionCombo.setEnabled(false);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        panel.add(versionCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel(Langdetect.get("label.build")), gbc);

        buildCombo = new JComboBox<>(new String[]{Langdetect.get("loading")});
        buildCombo.setEnabled(false);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.8;
        panel.add(buildCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel(Langdetect.get("label.path")), gbc);

        pathField = new JTextField(getDefaultMinecraftPath());
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(pathField, gbc);

        browseButton = new JButton(Langdetect.get("button.browse"));
        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(browseButton, gbc);

        browseButton.setPreferredSize(new Dimension(23, 23));
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            String currentPath = pathField.getText();
            File currentDir   = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setCurrentDirectory(currentDir);
            }

            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                pathField.setText(selected.getAbsolutePath());
            }
        });

        profileCheckbox = new JCheckBox(Langdetect.get("checkbox.profile"));
        profileCheckbox.setSelected(true);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2;
        panel.add(profileCheckbox, gbc);

        installButton = new JButton(Langdetect.get("button.install"));
        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 2;  
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(installButton, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(progressBar, gbc);

        installButton.addActionListener(e -> onInstall());

        versionCombo.addActionListener(e -> {
            String base = (String) versionCombo.getSelectedItem();
            if (base != null && buildMap.containsKey(base)) {
                buildCombo.removeAllItems();
                for (String b : buildMap.get(base)) {
                    buildCombo.addItem(b);
                }
                buildCombo.setEnabled(true);
            }
        });

        add(panel);
    }

    private void fetchReleases() {
        new Thread(() -> {
            try {
                Map<String, String> rawMap = GitHubdownloadfile.getAllZipReleases();

                List<Entry<String, String>> releaseList = new ArrayList<>(rawMap.entrySet());

                versionMap = new LinkedHashMap<>();
                buildMap = new LinkedHashMap<>();

                for (Entry<String, String> e : releaseList) {
                    String fullVersion = e.getKey();
                    String baseVersion = fullVersion;
                    String build = "default";

                    String[] parts = fullVersion.split("-");
                    if (parts.length >= 3) {
                        baseVersion = parts[0] + "-" + parts[1];
                        build = String.join("-", Arrays.copyOfRange(parts, 2, parts.length));
                    }

                    versionMap.put(fullVersion, e.getValue());
                    buildMap.computeIfAbsent(baseVersion, k -> new ArrayList<>()).add(build);
                }

                List<String> sortedBaseVersions = new ArrayList<>(buildMap.keySet());
                sortedBaseVersions.sort((a, b) -> compareVersion(b, a)); 

                SwingUtilities.invokeLater(() -> {
                    versionCombo.removeAllItems();
                    for (String v : sortedBaseVersions) {
                        versionCombo.addItem(v);
                    }
                    versionCombo.setEnabled(true);
                    if (versionCombo.getItemCount() > 0) {
                        versionCombo.setSelectedIndex(0);
                        String firstBase = (String) versionCombo.getSelectedItem();
                        buildCombo.removeAllItems();
                        for (String b : buildMap.get(firstBase)) {
                            buildCombo.addItem(b);
                        }
                        buildCombo.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            Langdetect.get("msg.cannotFetch") + "\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void onInstall() {
        String baseVersion = (String) versionCombo.getSelectedItem();
        String build = (String) buildCombo.getSelectedItem();
        if (baseVersion == null || build == null) {
            JOptionPane.showMessageDialog(this,
                    Langdetect.get("msg.invalidVersion"),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String versionName = baseVersion + "-" + build;
        if (!versionMap.containsKey(versionName)) {
            JOptionPane.showMessageDialog(this,
                    Langdetect.get("msg.invalidVersion"),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String installPath = pathField.getText();
        if (installPath == null || installPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    Langdetect.get("msg.invalidPath"),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File mcFolder = new File(installPath);
        File modsOld = new File(mcFolder, "mods.old");

        installButton.setEnabled(false);
        installButton.setText(Langdetect.get("button.installing"));
        progressBar.setVisible(true);

        new Thread(() -> {
            try {
                // Nếu mods.old đã có → nén lại
                List<File> toZip = new ArrayList<>();
                if (modsOld.exists()) toZip.add(modsOld);

                if (!toZip.isEmpty()) {
                    String time = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                    File zipBackup = new File(mcFolder, "mods-backup-" + time + ".zip");
                    zipFolders(toZip, zipBackup);
                    for (File f : toZip) deleteRecursively(f);
                }

                File mods = new File(mcFolder, "mods");
                if (mods.exists()) mods.renameTo(modsOld);

                File zip = GitHubdownloadfile.downloadZip(versionMap.get(versionName), versionName, mcFolder);
                ZipExtractor.extract(zip, mcFolder);
                zip.delete();

                SwingUtilities.invokeLater(() -> {
                    installButton.setEnabled(true);
                    progressBar.setVisible(false);
                    installButton.setText(Langdetect.get("button.install"));
                    JOptionPane.showMessageDialog(this,
                            Langdetect.get("msg.done"),
                            "OK", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    installButton.setEnabled(true);
                    progressBar.setVisible(false);
                    installButton.setText(Langdetect.get("button.install"));
                    JOptionPane.showMessageDialog(this,
                            Langdetect.get("msg.failed").replace("{0}", ex.getMessage()),
                            "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void zipFolders(List<File> folders, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File folder : folders) {
                zipFile(folder, folder.getName(), zos);
            }
        }
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) return;

        if (fileToZip.isDirectory()) {
            if (!fileName.endsWith("/")) fileName += "/";
            zos.putNextEntry(new ZipEntry(fileName));
            zos.closeEntry();
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + childFile.getName(), zos);
                }
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[4096];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }

    private String getDefaultMinecraftPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String user = System.getProperty("user.name");
        if (os.contains("win")) return "C:\\Users\\" + user + "\\AppData\\Roaming\\.minecraft";
        else if (os.contains("mac")) return System.getProperty("user.home") + "/Library/Application Support/minecraft";
        else return System.getProperty("user.home") + "/.minecraft";
    }

    private int compareVersion(String v1, String v2) {
        try {
            String s1 = v1.substring(v1.lastIndexOf("-") + 1);
            String s2 = v2.substring(v2.lastIndexOf("-") + 1);

            String[] p1 = s1.split("\\.");
            String[] p2 = s2.split("\\.");

            int len = Math.max(p1.length, p2.length);
            for (int i = 0; i < len; i++) {
                int n1 = i < p1.length ? parseSafe(p1[i]) : 0;
                int n2 = i < p2.length ? parseSafe(p2[i]) : 0;
                if (n1 != n2) return Integer.compare(n1, n2);
            }
            return 0;
        } catch (Exception e) {
            return v1.compareTo(v2);
        }
    }

    private int parseSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }
}
