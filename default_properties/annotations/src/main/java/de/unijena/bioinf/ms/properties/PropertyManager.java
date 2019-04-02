package de.unijena.bioinf.ms.properties;
/**
 * Created by Markus Fleischauer (markus.fleischauer@gmail.com)
 * as part of the sirius
 * 31.08.17.
 */


import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
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
import java.util.*;

/**
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
public class PropertyManager {
    public static final String PROPERTY_BASE = "de.unijena.bioinf";
    public static final String MS_PROPERTY_BASE = PROPERTY_BASE + ".ms";
    private static final String PROPERTY_LOCATIONS_KEY = MS_PROPERTY_BASE + ".propertyLocations";

    public static final CombinedConfiguration PROPERTIES;

    private static final CombinedConfiguration GLOBAL_CONFIGS;
    private static final CombinedConfiguration CONFIG_CLASSES;

    public static final String DEFAULT_PROPERTY_SOURCE = "sirius.build.properties";
    public static final String DEFAULT_CONFIG_SOURCE = "default.config";
    public static final String DEFAULT_CONFIG_CLASSES_SOURCE = "default_config_class.map";

    public static final String MS_CONFIGS_BASE = PropertyManager.MS_PROPERTY_BASE + ".configs";
    public static final String MS_CONFIG_CLASSES_BASE = PropertyManager.MS_PROPERTY_BASE + ".configClasses";

    public static final String CONFIGS_LOCATIONS_KEY = PropertyManager.MS_PROPERTY_BASE + ".configLocations";
    public static final String CONFIG_CLASSES_LOCATIONS_KEY = PropertyManager.MS_PROPERTY_BASE + ".configClassesLocations";

    public static final ParameterConfig DEFAULTS;


    static {
        try {

            PROPERTIES = newCombinedProperties();
            loadDefaultProperties();

            //config class for configs
            CONFIG_CLASSES = loadDefaultConfigClasses();

            //configs an properties need to have disjoint keys
            GLOBAL_CONFIGS = loadDefaultConfigs();

            DEFAULTS = new ParameterConfig(
                    GLOBAL_CONFIGS,
                    CONFIG_CLASSES,
                    null,
                    MS_CONFIGS_BASE,
                    MS_CONFIG_CLASSES_BASE
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

    private static CombinedConfiguration loadDefaultProperties() {
        return addPropertiesFromResources(System.getProperties().getProperty(PROPERTY_LOCATIONS_KEY), DEFAULT_PROPERTY_SOURCE, null, "PROPERTIES");
    }

    private static CombinedConfiguration loadDefaultConfigClasses() {
        return addPropertiesFromResources(PROPERTIES.getString(CONFIG_CLASSES_LOCATIONS_KEY), DEFAULT_CONFIG_SOURCE, MS_CONFIG_CLASSES_BASE, "CONFIG_CLASSES");
    }

    private static CombinedConfiguration loadDefaultConfigs() {
        GLOBAL_CONFIGS.addConfiguration(initProperties(), "");
        return addPropertiesFromResources(PROPERTIES.getString(CONFIGS_LOCATIONS_KEY), DEFAULT_CONFIG_CLASSES_SOURCE, MS_CONFIGS_BASE, "GLOBAL_CONFIG");
    }

    public static PropertiesConfiguration loadConfigurationFromStream(@NotNull InputStream input) throws ConfigurationException {
        PropertiesConfiguration config = initProperties();
        new FileHandler(config).load(input);

        /*if (prefixToAdd != null && !prefixToAdd.isEmpty()) {
            final PropertiesConfiguration tmp = initProperties();
            ((SubsetConfiguration) tmp.subset(prefixToAdd)).append(config);
            config = tmp;
        }*/
        return config;
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
        return addPropertiesFromStream(stream, initProperties(), name);
    }

    public static PropertiesConfiguration addPropertiesFromStream(@NotNull InputStream stream) throws IOException, ConfigurationException {
        return addPropertiesFromStream(stream, null);
    }


    //this reads and merges read only properties from within jar resources
    public static CombinedConfiguration addPropertiesFromResources(@Nullable final String locations, @Nullable final String defaultLocation, @Nullable final String prefixToAdd, @Nullable String name) {
        LinkedHashSet<String> resources = new LinkedHashSet<>();
        if (defaultLocation != null && !defaultLocation.isEmpty())
            resources.add(defaultLocation);

        if (locations != null && !locations.isEmpty())
            resources.addAll(Arrays.asList(locations.trim().split("\\s*,\\s*")));

        return addPropertiesFromResources(resources, prefixToAdd, name);
    }

    //this reads and merges read only properties from within jar resources
    public static CombinedConfiguration makePropertiesFromResources(@NotNull final LinkedHashSet<String> resources, @Nullable String prefixToAdd, @Nullable String name) {
        name = (name == null || name.isEmpty()) ? String.join("_", resources) : name;
        final CombinedConfiguration combined = newCombinedProperties();
        List<String> reverse = new ArrayList<>(resources);
        Collections.reverse(reverse);
        for (String resource : reverse) {
            combined.addConfiguration(makePropertiesFromResource(resource), resource);
        }

        return combined;
    }

    public static CombinedConfiguration addPropertiesFromResources(@NotNull final LinkedHashSet<String> resources, @Nullable String prefixToAdd, @Nullable String name) {
        if (resources.isEmpty())
            throw new IllegalArgumentException("resources to add are empty!");
        CombinedConfiguration configToAdd = makePropertiesFromResources(resources, prefixToAdd, name);
        PROPERTIES.addConfiguration(configToAdd, name, prefixToAdd);
        return configToAdd;
    }

    public static PropertiesConfiguration addPropertiesFromResource(@NotNull final String resource, @Nullable String prefixToAdd, @Nullable String name) {
        PropertiesConfiguration configToAdd = makePropertiesFromResource(resource);
        PROPERTIES.addConfiguration(configToAdd, name, prefixToAdd);
        return configToAdd;
    }

    private static PropertiesConfiguration makePropertiesFromResource(@NotNull final String resource) {
        final PropertiesConfiguration config = initProperties();
        try (InputStream input = PropertyManager.class.getResourceAsStream("/" + resource)) {
            if (input != null)
                new FileHandler(config).load(input);
                /*if (prefixToAdd != null && !prefixToAdd.isEmpty()) {
                    SubsetConfiguration sub = ((SubsetConfiguration) combined.subset(prefixToAdd));
                    sub.append(tmp);
                    tmp.getLayout().getKeys().stream().forEach(key -> {
                        final String kk = prefixToAdd + "." + key;
                        if (combined.getLayout().getComment(kk) == null)
                            combined.getLayout().setComment(kk, tmp.getLayout().getComment(key));
                    });
                } else {
                    combined.append(tmp);
                }*/
            } catch (ConfigurationException | IOException e) {
                System.err.println("Could not load properties from " + resource);
                e.printStackTrace();
            }

        return config;
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
        String v = .getProperty(key);
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
