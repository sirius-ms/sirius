package de.unijena.bioinf.ms.properties;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 31.08.17.
 */


import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.SubsetConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PropertyManager {
    public static final String PROPERTY_BASE = "de.unijena.bioinf";
    public static final String MS_PROPERTY_BASE = PROPERTY_BASE + ".ms";
    private static final String PROPERTY_LOCATIONS_KEY = MS_PROPERTY_BASE + ".propertyLocations";

    public static final CombinedConfiguration PROPERTIES;

    public static final String MS_CONFIGS_BASE = PropertyManager.MS_PROPERTY_BASE + ".configs";
    public static final String CONFIGS_LOCATIONS_KEY = PropertyManager.MS_PROPERTY_BASE + ".configLocations";

    public static final DefaultParameterConfig DEFAULTS;




    static {
        try {
            PROPERTIES = newCombinedProperties();
            loadDefaultProperties();

            DEFAULTS = new DefaultParameterConfig(
                    PROPERTIES,
                    loadDefaultConfigs().getLayout(),
                    MS_CONFIGS_BASE
            );
        } catch (Throwable e) {
            System.err.println("Property Manager STATIC Block Error!");
            e.printStackTrace(System.err);
            throw e;
        }
    }

    public static CombinedConfiguration newCombinedProperties() {
        try {
            CombinedConfiguration c = new CombinedConfigurationBuilder()
                    .configure(new Parameters().combined()
                            .setThrowExceptionOnMissing(false)
                            .setListDelimiterHandler(new DisabledListDelimiterHandler()))
                    .getConfiguration();
            c.setNodeCombiner(new OverrideCombiner());
            return c;

        } catch (ConfigurationException e) {
            System.err.println("WARNING: Error during initProperties");
            return new CombinedConfiguration();
        }
    }

    public static PropertiesConfiguration initProperties() {
        return initProperties(null);
    }

    public static @NotNull PropertiesConfiguration initProperties(@Nullable Path file) {
        try {
            PropertiesBuilderParameters props = new Parameters().properties()
                    .setThrowExceptionOnMissing(false)
                    .setListDelimiterHandler(new DisabledListDelimiterHandler())
                    .setIncludesAllowed(true);
            if (file != null)
                props.setFile(file.toFile());

            return new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(props).getConfiguration();
        } catch (ConfigurationException e) {
            System.err.println("WARNING: Error during initProperties");
            return new PropertiesConfiguration();
        }
    }

    private static PropertiesConfiguration loadDefaultProperties() {
        if (System.getProperty(PROPERTY_LOCATIONS_KEY) == null)
            System.getProperties().put(PROPERTY_LOCATIONS_KEY, "sirius.build.properties");
        return addPropertiesFromResources(System.getProperties().getProperty(PROPERTY_LOCATIONS_KEY), null, "resource_properties");
    }

    private static PropertiesConfiguration loadDefaultConfigs() {
        return addPropertiesFromResources(PROPERTIES.getString(CONFIGS_LOCATIONS_KEY), MS_CONFIGS_BASE, "resource_configs");
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream input, @Nullable String name, @Nullable String prefixToAdd) throws IOException, ConfigurationException {
        PropertiesConfiguration config = initProperties();
        new FileHandler(config).load(input);

        if (prefixToAdd != null && !prefixToAdd.isEmpty()) {
            final PropertiesConfiguration tmp = initProperties();
            ((SubsetConfiguration) tmp.subset(prefixToAdd)).append(config);
            config = tmp;
        }

        return addPropertiesFromConfiguration(config, name);
    }

    public static PropertiesConfiguration addPropertiesFromConfiguration(@NotNull PropertiesConfiguration config, @Nullable String name) {
        if (name == null || name.isEmpty())
            PROPERTIES.addConfiguration(config);
        else
            PROPERTIES.addConfiguration(config, name);
        return config;
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream stream, @NotNull PropertiesConfiguration config, @Nullable String name) throws IOException, ConfigurationException {
        new FileHandler(config).load(stream);
        addPropertiesFromConfiguration(config, name);
        return config;
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream stream, @Nullable String name) throws IOException, ConfigurationException {
        return addPropertiesFromStream(stream, initProperties(), name);
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream stream) throws IOException, ConfigurationException {
        return addPropertiesFromStream(stream, null);
    }


    /*public static @Nullable Configuration addPropertiesFromFile(@NotNull Path file) {
        try {
            if (Files.exists(file)) {
                return addPropertiesFromStream(Files.newInputStream(file, StandardOpenOption.READ), initProperties(file), file.getFileName().toString());
            }
        } catch (ConfigurationException | IOException e) {
            System.err.println("WARNING: could not load Properties from: " + file.toString());
            e.printStackTrace();
        }
        return null;
    }*/

    //this reads and merges read only properties from within jar resources
    public static PropertiesConfiguration addPropertiesFromResources(@Nullable final String locations, @Nullable final String prefixToAdd, @Nullable String name) {
        LinkedHashSet<String> resources = new LinkedHashSet<>();

        if (locations != null && !locations.isEmpty())
            resources.addAll(Arrays.asList(locations.split(",")));

        return addPropertiesFromResources(resources, prefixToAdd, name);
    }

    //this reads and merges read only properties from within jar resources
    public static PropertiesConfiguration addPropertiesFromResources(@NotNull final LinkedHashSet<String> resources, @Nullable String prefixToAdd, @Nullable String name) {
        name = (name == null || name.isEmpty()) ? String.join("_", resources) : name;
        final PropertiesConfiguration combined = initProperties();
        for (String resource : resources) {
            try (InputStream input = PropertyManager.class.getResourceAsStream("/" + resource)) {
                final PropertiesConfiguration tmp = initProperties();
                new FileHandler(tmp).load(input);

                if (prefixToAdd != null && !prefixToAdd.isEmpty()) {
                    SubsetConfiguration sub = ((SubsetConfiguration) combined.subset(prefixToAdd));
                    sub.append(tmp);
                    tmp.getLayout().getKeys().stream().forEach(key -> {
                        final String kk = prefixToAdd + "." + key;
                        if (combined.getLayout().getComment(kk) == null)
                            combined.getLayout().setComment(kk, tmp.getLayout().getComment(key));
                    });
                } else {
                    combined.append(tmp);
                }
            } catch (ConfigurationException | IOException e) {
                System.err.println("Could not load properties from " + resource);
                e.printStackTrace();
            }
        }
        addPropertiesFromConfiguration(combined, name);
        return combined;
    }

    public static void setProperty(String key, Object value) {
        PROPERTIES.setProperty(key, value);
    }

    public static void setProperties(Properties properties) {
        properties.forEach((k, v) -> setProperty(String.valueOf(k), v));
    }

    public static int getNumberOfCores() {
        return PROPERTIES.getInt("de.unijena.bioinf.sirius.cpu.cores", 1);
    }

    public static int getNumberOfThreads() {
        return PROPERTIES.getInt("de.unijena.bioinf.sirius.cpu.threads", 2);
    }

    public static String getStringProperty(String key) {
        return PROPERTIES.getString(key);
    }

    public static String getStringProperty(String key, String backupKey, String defaultValue) {
        return PROPERTIES.getString(key, PROPERTIES.getString(backupKey, defaultValue));
    }


    public static String getStringProperty(String key, String backupKey) {
        return getStringProperty(key, backupKey, null);
    }

    /*public static int getIntProperty(String key, String backupKey) {
        return Integer.valueOf(getStringProperty(key, backupKey));
    }

    public static double getDoubleProperty(String key, String backupKey) {
        return Double.valueOf(getStringProperty(key, backupKey));
    }

    public static boolean getBooleanProperty(String key, String backupKey) {
        return Boolean.valueOf(getStringProperty(key, backupKey));
    }*/

    /*public static int getIntProperty(String key, int defaultValue) {
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
    }*/

    public static Path getPath(String key) {
        String v = PROPERTIES.getString(key);
        return (v == null) ? null : Paths.get(v);
    }

    public static File getFile(String key) {
        String v = PROPERTIES.getString(key);
        return (v == null) ? null : new File(v);
    }

    public static Iterator<String> getDefaultPropertyKeys() {
        return PROPERTIES.getKeys();
    }

    public static Properties asProperties() {
        final Properties p = new Properties();
        getDefaultPropertyKeys().forEachRemaining(k -> p.put(k, PROPERTIES.getString(k)));
        return p;
    }


    /*public static void main(String[] args) throws IOException {
        PropertyManager.PROPERTIES.get("foo");
        PropertyManager.addPropertiesFromStream(DefaultPropertyLoader.class.getResourceAsStream("/default.annotation.properties"),PROPERTY_BASE + ".ms");
        System.out.println(PropertyManager.PROPERTIES);
    }*/

}
