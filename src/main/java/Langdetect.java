import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Langdetect {
    private static Map<String, String> langMap = new HashMap<>();

    public static void load(String langCode) {
        String fileName = "lang/" + langCode + ".lang";
        try (InputStream is = Langdetect.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) throw new IOException("Lang file not found: " + fileName);

            Properties props = new Properties();
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));

            langMap.clear();
            for (String key : props.stringPropertyNames()) {
                langMap.put(key, props.getProperty(key));
            }
        } catch (Exception e) {
            System.err.println("⚠️ Không thể load file ngôn ngữ, đưa về mặc định tiếng anh");
            if (!langCode.equals("en_us")) load("en_us");
        }
    }

    public static String tr(String key) {
        return langMap.getOrDefault(key, key);
    }

    public static String get(String key) {
    return tr(key);
    }

    public static String detectLangCode() {
        String sysLang = System.getProperty("user.language");
        switch (sysLang) {
            case "vi": return "vi";
            case "ru": return "ru";
            default: return "en_us";
        }
    }
}
