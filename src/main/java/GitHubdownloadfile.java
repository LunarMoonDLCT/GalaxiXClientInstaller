import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.net.ssl.HttpsURLConnection;

public class GitHubdownloadfile {
    private static final String API_URL = "https://api.github.com/repos/LunarMoonDLCT/Minecraft-Galaxy-Client/releases";

    public static Map<String, String> getAllZipReleases() throws IOException {
        URL url = new URL(API_URL);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "GalaxyClientInstaller");

        if (conn.getResponseCode() != 200) {
            throw new IOException("GitHub API trả về lỗi " + conn.getResponseCode());
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder json = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }
        reader.close();

        // Lọc toàn các file chỉ đúng tên GalaxyClient
        Pattern pattern = Pattern.compile("\"name\"\\s*:\\s*\"(GalaxyClient[^\"]+\\.zip)\".*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json.toString());

        Map<String, String> result = new LinkedHashMap<>();
        while (matcher.find()) {
            result.put(matcher.group(1).replace(".zip", ""), matcher.group(2));
        }

        if (result.isEmpty()) {
            throw new IOException("Không tìm thấy bản phát hành nào!");
        }

        return result;
    }

    public static File downloadZip(String urlStr, String versionTag, File installDir) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "GalaxyClientInstaller");

        if (conn.getResponseCode() != 200) {
            throw new IOException("Không thể tải về file từ GitHub: " + conn.getResponseCode());
        }

        File zipFile = new File(installDir, "GalaxyClient-" + versionTag + ".zip");

        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(zipFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return zipFile;
    }
}
