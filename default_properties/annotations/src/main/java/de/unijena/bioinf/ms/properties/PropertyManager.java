package de.unijena.bioinf.ms.properties;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 31.08.17.
 */

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PropertyManager {
    public static final String PROPERTY_BASE = "de.unijena.bioinf";
    public static final Properties PROPERTIES;
    public static final DefaultPropertyLoader DEFAULTS;

    static {
        if (System.getProperty("de.unijena.bioinf.ms.sirius.props") == null)
            System.getProperties().put("de.unijena.bioinf.ms.sirius.props", "sirius.build.properties");
        PROPERTIES = new Properties();
        loadDefaultProperties();
        loadProfileProperties();
        DEFAULTS = new DefaultPropertyLoader(PROPERTIES, PROPERTY_BASE + ".ms");
    }

    public static void addPropertiesFromStream(@NotNull InputStream stream, @Nullable String prefixToAdd) throws IOException {
        Properties props = new Properties();
        props.load(stream);

        if (prefixToAdd != null && !prefixToAdd.isEmpty())
            props.forEach((key, value) -> PropertyManager.PROPERTIES.put(prefixToAdd + "." + key, value));
        else
            PropertyManager.PROPERTIES.putAll(props);
    }

    public static void addPropertiesFromStream(@NotNull InputStream stream) throws IOException {
        addPropertiesFromStream(stream, null);
    }

    public static void addPropertiesFromFile(@NotNull Path files) {
        addPropertiesFromFile(files, null);
    }

    public static void addPropertiesFromFile(@NotNull Path files, @Nullable String prefixToAdd) {
        try {
            if (Files.exists(files)) {
                addPropertiesFromStream(Files.newInputStream(files, StandardOpenOption.READ), prefixToAdd);
            }
        } catch (IOException e) {
            System.err.println("WARNING: could not load Properties from: " + files.toString());
            e.printStackTrace();
        }
    }

    public static void addProfilePropertiesFromFile(@NotNull Path profilePath) {
        addPropertiesFromFile(profilePath, PROPERTY_BASE + ".ms");
    }

    public static void addProfilePropertiesFromStream(@NotNull InputStream stream) throws IOException {
        addPropertiesFromStream(stream, PROPERTY_BASE + ".ms");
    }

    private static void loadProfileProperties() {
        loadProperties(PROPERTIES.getProperty("de.unijena.bioinf.ms.sirius.profiles"), PROPERTY_BASE + ".ms");
    }

    private static void loadDefaultProperties() {
        loadProperties(System.getProperties().getProperty("de.unijena.bioinf.ms.sirius.props"), null);
    }

    private static void loadProperties(@Nullable final String locations, @Nullable final String prefixToAdd) {
        LinkedHashSet<String> resources = new LinkedHashSet<>();

        if (locations != null && !locations.isEmpty())
            resources.addAll(Arrays.asList(locations.split(",")));

        loadProperties(resources, prefixToAdd);
    }

    private static void loadProperties(@NotNull final LinkedHashSet<String> resources, @Nullable String prefixToAdd) {
        for (String resource : resources) {
            try (InputStream input = PropertyManager.class.getResourceAsStream("/" + resource)) {
                addPropertiesFromStream(input, prefixToAdd);
            } catch (IOException e) {
                System.err.println("Could not load properties from " + resource);
                e.printStackTrace();
            }
        }
    }

    public static Object setProperty(String key, String value) {
        return PROPERTIES.setProperty(key, value);
    }

    public static Object put(String key, String value) {
        return PROPERTIES.put(key, value);
    }

    public static int getNumberOfCores() {
        return Integer.valueOf(PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.cores", "1"));
    }

    public static int getNumberOfThreads() {
        return Integer.valueOf(PROPERTIES.getProperty("de.unijena.bioinf.sirius.cpu.threads", "2"));
    }

    public static String getProperty(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static String getProperty(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String getStringProperty(String key, String backupKey, String defaultValue) {
        return getProperty(key, getProperty(backupKey, defaultValue));
    }


    public static String getStringProperty(String key, String backupKey) {
        return getStringProperty(key, backupKey, null);
    }

    public static int getIntProperty(String key, String backupKey) {
        return Integer.valueOf(getStringProperty(key, backupKey));
    }

    public static double getDoubleProperty(String key, String backupKey) {
        return Double.valueOf(getStringProperty(key, backupKey));
    }

    public static boolean getBooleanProperty(String key, String backupKey) {
        return Boolean.valueOf(getStringProperty(key, backupKey));
    }

    public static int getIntProperty(String key, int defaultValue) {
        String v = getProperty(key);
        return v == null ? defaultValue : Integer.valueOf(v);
    }

    public static double getDoubleProperty(String key, double defaultValue) {
        String v = getProperty(key);
        return v == null ? defaultValue : Double.valueOf(v);
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String v = getProperty(key);
        return v == null ? defaultValue : Boolean.valueOf(v);
    }

    public static boolean getBooleanProperty(String key) {
        return getBooleanProperty(key, false);
    }

    public static Path getPath(String key) {
        String v = getProperty(key);
        return (v == null) ? null : Paths.get(v);
    }

    public static File getFile(String key) {
        String v = getProperty(key);
        return (v == null) ? null : new File(v);
    }


    /*public static void main(String[] args) throws IOException {
        PropertyManager.PROPERTIES.get("foo");
        PropertyManager.addPropertiesFromStream(DefaultPropertyLoader.class.getResourceAsStream("/default.annotation.properties"),PROPERTY_BASE + ".ms");
        System.out.println(PropertyManager.PROPERTIES);
    }*/

}
