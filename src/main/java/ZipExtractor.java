import java.io.*;
import java.util.zip.*;

public class ZipExtractor {
    public static void extract(File zipFile, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;

            while ((entry = zipIn.getNextEntry()) != null) {
                String entryName = entry.getName();
                File filePath = new File(destDir, entryName);

                System.out.println("👉 Đang xử lý: " + entryName);

                try {
                    if (entry.isDirectory()) {
                        filePath.mkdirs();
                    } else {
                        // Tạo thư mục CHA nếu chưa có thư mục đấy
                        File parent = filePath.getParentFile();
                        if (!parent.exists()) {
                            parent.mkdirs();
                        }

                        // Ghi file ra đĩa
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zipIn.read(buffer)) > 0) {
                                bos.write(buffer, 0, len);
                            }
                        }

                        System.out.println("✅ Đã ghi: " + filePath.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    System.err.println("❌ Lỗi tại: " + filePath.getAbsolutePath());
                    ex.printStackTrace();
                    throw new IOException("Không thể giải nén file: " + filePath.getAbsolutePath() + "\nLý do: " + ex.getMessage());
                }

                zipIn.closeEntry();
            }
        }
    }
}
