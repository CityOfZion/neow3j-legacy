package io.neow3j.compiler;

import java.io.IOException;
import java.util.Properties;

public class TestProperties {

    private static TestProperties instance = null;
    private Properties properties;

    protected TestProperties() throws IOException {
        properties = new Properties();
        properties.load(getClass().getResourceAsStream("/test.properties"));
    }

    public static TestProperties getInstance() {
        if (instance == null) {
            try {
                instance = new TestProperties();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return instance;
    }

    public static String defaultAccountAddress() {
        return getValue("defaultAccountAddress");
    }

    public static String defaultAccountWIF() {
        return getValue("defaultAccountWIF");
    }
    public static String neo3PrivateNetContainerImg() {
        return getValue("neo3PrivateNetContainerImg");
    }

    public static String committeeAccountAddress() {
        return getValue("committeeAccountAddress");
    }

    public static String getValue(String key) {
        return getInstance().properties.getProperty(key);
    }

}
