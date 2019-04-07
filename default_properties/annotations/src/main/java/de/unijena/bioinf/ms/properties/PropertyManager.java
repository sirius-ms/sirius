package de.unijena.bioinf.ms.properties;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 31.08.17.
 */


import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.ImmutableConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static final String DEFAULT_PROPERTY_SOURCE = "sirius.build.properties";
    public static final String DEFAULT_CONFIG_SOURCE = "default.config";
    public static final String DEFAULT_CONFIG_CLASSES_SOURCE = "default_config_class.map";

    public static final String MS_CONFIGS_BASE = PropertyManager.MS_PROPERTY_BASE + ".configs";
    public static final String MS_CONFIG_CLASSES_BASE = PropertyManager.MS_PROPERTY_BASE + ".configClasses";

    public static final String CONFIGS_LOCATIONS_KEY = PropertyManager.MS_PROPERTY_BASE + ".configLocations";
    public static final String CONFIG_CLASSES_LOCATIONS_KEY = PropertyManager.MS_PROPERTY_BASE + ".configClassesLocations";

    protected static final CombinedConfiguration PERSISTENT_PROPERTIES;
    protected static final PropertiesConfiguration CHANGED_PROPERTIES;
    protected static final CombinedConfiguration PROPERTIES;
    public static final ParameterConfig DEFAULTS;


    static {
        try {
            PERSISTENT_PROPERTIES = SiriusConfigUtils.newCombinedConfiguration();

            PROPERTIES = SiriusConfigUtils.newCombinedConfiguration();
            PROPERTIES.addConfiguration(PERSISTENT_PROPERTIES, "PERSISTENT_PROPERTIES");
            PERSISTENT_PROPERTIES.addEventListener(CombinedConfiguration.COMBINED_INVALIDATE, event -> PROPERTIES.invalidate());
            CHANGED_PROPERTIES = loadDefaultProperties();

            DEFAULTS = new ParameterConfig(
                    loadDefaultConfigs(),//config class for configs
                    loadDefaultConfigClasses(),//configs an properties need to have disjoint keys
                    DEFAULT_CONFIG_SOURCE,
                    null,
                    MS_CONFIGS_BASE,
                    MS_CONFIG_CLASSES_BASE
            ).newIndependentInstance("CHANGED_DEFAULT_CONFIGS");


        } catch (Throwable e) {
            System.err.println("Property Manager STATIC Block Error!");
            e.printStackTrace(System.err);
            throw e;
        }
    }


    public static ImmutableConfiguration getConfigClassProperties() {
        return DEFAULTS.getClassConfigs();
    }

    public static String getConfigClassStringProperty(String key) {
        if (key.startsWith(MS_CONFIG_CLASSES_BASE))
            return getProperty(key);
        else
            return getConfigClassProperties().getString(key);
    }

    public static ImmutableConfiguration getDefaultConfigProperties() {
        return DEFAULTS.getConfigs();
    }

    public static String getDefaultConfigStringProperty(String key) {
        if (key.startsWith(MS_CONFIGS_BASE))
            return getProperty(key);
        else
            return getDefaultConfigProperties().getString(key);
    }

    private static PropertiesConfiguration loadDefaultProperties() {
        CombinedConfiguration configToAdd = SiriusConfigUtils.newCombinedConfiguration();
        PropertiesConfiguration changeable = SiriusConfigUtils.newConfiguration();
        configToAdd.addConfiguration(changeable, "CHANGED_DEFAULT_PROPERTIES");
        SiriusConfigUtils.makeConfigFromResources(configToAdd, SiriusConfigUtils.parseResourcesLocation(System.getProperties().getProperty(PROPERTY_LOCATIONS_KEY), DEFAULT_PROPERTY_SOURCE));
        addConfiguration(configToAdd, null, "PROPERTIES");
        return changeable;
    }

    private static CombinedConfiguration loadDefaultConfigClasses() {
        return addPropertiesFromResources(PROPERTIES.getString(CONFIG_CLASSES_LOCATIONS_KEY), DEFAULT_CONFIG_CLASSES_SOURCE, MS_CONFIG_CLASSES_BASE, "CONFIG_CLASSES");
    }

    private static CombinedConfiguration loadDefaultConfigs() {
        return addPropertiesFromResources(PROPERTIES.getString(CONFIGS_LOCATIONS_KEY), DEFAULT_CONFIG_SOURCE, MS_CONFIGS_BASE, "GLOBAL_CONFIG");
    }

    public static PropertiesConfiguration loadConfigurationFromStream(@NotNull InputStream input) throws ConfigurationException {
        PropertiesConfiguration config = SiriusConfigUtils.newConfiguration();
        new FileHandler(config).load(input);
        return config;
    }

    public static PropertiesConfiguration loadPersistentPropertiesFile(@NotNull File file) throws ConfigurationException {
        @NotNull PropertiesConfiguration fileConfig = SiriusConfigUtils.newConfiguration(file);
        PERSISTENT_PROPERTIES.addConfiguration(fileConfig, file.getAbsolutePath());
        return fileConfig;
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream input, @Nullable String name, @Nullable String prefixToAdd) throws ConfigurationException {
        final PropertiesConfiguration config = loadConfigurationFromStream(input);
        PROPERTIES.addConfiguration(config, name, prefixToAdd);
        return config;
    }


    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream stream, @NotNull PropertiesConfiguration config, @Nullable String name) throws IOException, ConfigurationException {
        new FileHandler(config).load(stream);
        PROPERTIES.addConfiguration(config, name);
        return config;
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream stream, @Nullable String name) throws IOException, ConfigurationException {
        return addPropertiesFromStream(stream, SiriusConfigUtils.newConfiguration(), name);
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream stream) throws IOException, ConfigurationException {
        return addPropertiesFromStream(stream, null);
    }


    //this reads and merges read only properties from within jar resources
    public static CombinedConfiguration addPropertiesFromResources(@Nullable final String locations, @Nullable final String defaultLocation, @Nullable final String prefixToAdd, @Nullable String name) {
        return addPropertiesFromResources(SiriusConfigUtils.parseResourcesLocation(locations, defaultLocation), prefixToAdd, name);
    }


    public static CombinedConfiguration addPropertiesFromResources(@NotNull final LinkedHashSet<String> resources, @Nullable String prefixToAdd, @Nullable String name) {
        if (resources.isEmpty())
            throw new IllegalArgumentException("resources to add are empty!");

        name = (name == null || name.isEmpty()) ? String.join("_", resources) : name;


        return addConfiguration(
                SiriusConfigUtils.makeConfigFromResources(resources),
                prefixToAdd, name
        );
    }

    public static <C extends Configuration> C addConfiguration(@NotNull final C configToAdd, @Nullable String prefixToAdd, @NotNull String name) {
        PROPERTIES.addConfiguration(configToAdd, name, prefixToAdd);
        if (configToAdd instanceof CombinedConfiguration)
            listenCombinedConfiguration((CombinedConfiguration) configToAdd, prefixToAdd, name);
        return configToAdd;
    }

    private static void listenCombinedConfiguration(@NotNull final CombinedConfiguration configToAdd, @Nullable String prefixToAdd, @NotNull String name) {
        configToAdd.addEventListener(CombinedConfiguration.COMBINED_INVALIDATE, event -> PROPERTIES.invalidate());
    }

    public static PropertiesConfiguration addPropertiesFromResource(@NotNull final String resource, @Nullable String prefixToAdd, @Nullable String name) {
        PropertiesConfiguration configToAdd = SiriusConfigUtils.makeConfigFromStream(resource);
        PROPERTIES.addConfiguration(configToAdd, name, prefixToAdd);
        return configToAdd;
    }

    public static void setProperty(String key, Object value) {
        //todo do we want to change persintent props automatically?
        CHANGED_PROPERTIES.setProperty(key, value);
    }

    public static void setProperties(Properties properties) {
        properties.forEach((k, v) -> setProperty(String.valueOf(k), v));
    }

    public static String getProperty(@NotNull String key, @Nullable String backupKey, @Nullable String defaultValue) {
        if (backupKey != null)
            return PROPERTIES.getString(key, PROPERTIES.getString(backupKey, defaultValue));
        return PROPERTIES.getString(key, defaultValue);
    }

    public static String getProperty(String key) {
        return PROPERTIES.getString(key);
    }

    public static Path getPath(String key) {
        String v = PROPERTIES.getString(key);
        return (v == null) ? null : Paths.get(v);
    }

    public static File getFile(String key) {
        String v = PROPERTIES.getString(key);
        return (v == null) ? null : new File(v);
    }

    public static int getNumberOfCores() {
        return PROPERTIES.getInt("de.unijena.bioinf.sirius.cpu.cores", 1);
    }

    public static int getNumberOfThreads() {
        return PROPERTIES.getInt("de.unijena.bioinf.sirius.cpu.threads", 2);
    }

    public static Iterator<String> getPropertyKeys() {
        return PROPERTIES.getKeys();
    }

    public static Properties asProperties() {
        final Properties p = new Properties();
        getPropertyKeys().forEachRemaining(k -> p.put(k, PROPERTIES.getString(k)));
        return p;
    }

    /*public static void main(String[] args) throws IOException {
        PropertyManager.PROPERTIES.get("foo");
        PropertyManager.addPropertiesFromStream(DefaultPropertyLoader.class.getResourceAsStream("/default.annotation.properties"),PROPERTY_BASE + ".ms");
        System.out.println(PropertyManager.PROPERTIES);
    }*/

}
