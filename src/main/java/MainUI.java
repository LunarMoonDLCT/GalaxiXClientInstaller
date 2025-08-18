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
    private JTextField pathField;
    private JCheckBox profileCheckbox;
    private JButton installButton;
    private JButton browseButton;
    private Map<String, String> versionMap;
    private JProgressBar progressBar;

    public MainUI() {
        try (InputStream is = getClass().getResourceAsStream("/GC/assets/icon/galaxy2.png")) {
            if (is != null) {
                Image icon = ImageIO.read(is);
                setIconImage(icon);
            }
        } catch (Exception e) {}

//Debug spam windows        SwingUtilities.invokeLater(() -> new MainUI().setVisible(true));
        System.out.println("MainUI: Hiện cửa sổ");
        setTitle(Langdetect.get("title"));
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

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        panel.add(new JLabel(Langdetect.get("label.version")), gbc);

        versionCombo = new JComboBox<>(new String[]{Langdetect.get("loading")});
        versionCombo.setEnabled(false);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        panel.add(versionCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel(Langdetect.get("label.path")), gbc);

        pathField = new JTextField(getDefaultMinecraftPath());
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(pathField, gbc);

        browseButton = new JButton(Langdetect.get("button.browse"));
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0;
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
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(profileCheckbox, gbc);

        installButton = new JButton(Langdetect.get("button.install"));
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2;  
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(installButton, gbc);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);

        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 2;
        panel.add(progressBar, gbc);

        installButton.addActionListener(e -> onInstall());

        add(panel);
    }

    private void fetchReleases() {
        new Thread(() -> {
            try {
                Map<String, String> rawMap = GitHubdownloadfile.getAllZipReleases();
                List<Entry<String, String>> sortedList = new ArrayList<>(rawMap.entrySet());
                sortedList.sort((a, b) -> compareVersions(b.getKey(), a.getKey()));

                versionMap = new LinkedHashMap<>();
                for (Entry<String, String> e : sortedList) {
                    versionMap.put(e.getKey(), e.getValue());
                }

                SwingUtilities.invokeLater(() -> {
                    versionCombo.removeAllItems();
                    for (String v : versionMap.keySet()) versionCombo.addItem(v);
                    versionCombo.setEnabled(true);
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
        String versionName = (String) versionCombo.getSelectedItem();
        if (versionName == null || !versionMap.containsKey(versionName)) {
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
//        File configOld = new File(mcFolder, "config.old");

        installButton.setEnabled(false);
        installButton.setText(Langdetect.get("button.installing"));
        progressBar.setVisible(true);

        new Thread(() -> {
            try {
                // Nếu mods.old đã có → nén lại
                List<File> toZip = new ArrayList<>();
                if (modsOld.exists()) toZip.add(modsOld);
//                if (configOld.exists()) toZip.add(configOld);

                if (!toZip.isEmpty()) {
                    String time = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
                    File zipBackup = new File(mcFolder, "mods-backup-" + time + ".zip");
                    zipFolders(toZip, zipBackup);
                    for (File f : toZip) deleteRecursively(f);
                }

                File mods = new File(mcFolder, "mods");
//                File config = new File(mcFolder, "config");
                if (mods.exists()) mods.renameTo(modsOld);
//                if (config.exists()) config.renameTo(configOld);

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

    private int compareVersions(String v1, String v2) {
        String[] a = v1.replaceAll("[^\\d.]", "").split("\\.");
        String[] b = v2.replaceAll("[^\\d.]", "").split("\\.");
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            int ai = i < a.length ? Integer.parseInt(a[i]) : 0;
            int bi = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (ai != bi) return ai - bi;
        }
        return 0;
    }
}
