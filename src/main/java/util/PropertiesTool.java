package util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class PropertiesTool {
    private static final String fileName = "preferences.properties";
    private static Properties props = null;

    static {
        try {
            props = new Properties();
            props.load(new FileInputStream(fileName));
        } catch (Exception e) {
           e.printStackTrace();
        }
    }

    public static Properties getProps() {
        return props;
    }

    public static void saveProperties(String key, String value) {
        try {
            props.setProperty(key, value);
            FileOutputStream filePath = new FileOutputStream(fileName);
            props.store(filePath, "User Preferences");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readProperties(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

}
