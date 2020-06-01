package de.unijena.bioinf.ms.properties;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
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
import java.util.*;

public class SiriusConfigUtils {

    public static CombinedConfiguration newCombinedConfiguration() {
        return new CombinedConfiguration(new OverrideCombiner());
    }

    private static PropertiesBuilderParameters makeConfigProps(@Nullable File file) {
        final PropertiesBuilderParameters paras = new Parameters().properties()
                .setThrowExceptionOnMissing(false)
                .setListDelimiterHandler(new DisabledListDelimiterHandler())
                .setIncludesAllowed(true);
        if (file != null)
            paras.setFile(file);
        return paras;
    }

    public static PropertiesConfiguration newConfiguration() {
        return newConfiguration((File) null);
    }

    public static @NotNull PropertiesConfiguration newConfiguration(@Nullable File file) {
        try {
            return new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(makeConfigProps(file)).getConfiguration();
        } catch (ConfigurationException e) {
            System.err.println("WARNING: Error during PropertiesConfiguration initialization");
            e.printStackTrace();
            return new PropertiesConfiguration();
        }
    }

    public static @NotNull PropertiesConfiguration newConfiguration(@NotNull PropertyFileWatcher watcher) {
        try {
            ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new ReloadingFileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(makeConfigProps(watcher.getFile().toFile()));
            watcher.setController(builder.getReloadingController());
            return builder.getConfiguration();

        } catch (ConfigurationException e) {
            System.err.println("WARNING: Error during PropertiesConfiguration initialization with auto reloading");
            e.printStackTrace();
            return new PropertiesConfiguration();
        }
    }


    public static LinkedHashSet<String> parseResourcesLocation(@Nullable final String locations) {
        return parseResourcesLocation(locations, null);
    }

    public static LinkedHashSet<String> parseResourcesLocation(@Nullable final String locations, @Nullable final String defaultLocation) {
        LinkedHashSet<String> resources = new LinkedHashSet<>();
        if (defaultLocation != null && !defaultLocation.isEmpty())
            resources.add(defaultLocation);

        if (locations != null && !locations.isEmpty())
            resources.addAll(Arrays.asList(locations.trim().split("\\s*,\\s*")));

        return resources;
    }

    //this reads and merges read only properties from within jar resources
    public static CombinedConfiguration makeConfigFromResources(@NotNull final LinkedHashSet<String> resources) {
        return makeConfigFromResources(SiriusConfigUtils.newCombinedConfiguration(), resources);
    }

    public static CombinedConfiguration makeConfigFromResources(@NotNull CombinedConfiguration configToAddTo, @NotNull final LinkedHashSet<String> resources) {
        List<String> reverse = new ArrayList<>(resources);
        Collections.reverse(reverse);
        for (String resource : reverse) {
            configToAddTo.addConfiguration(makeConfigFromStream(resource), resource);
        }

        return configToAddTo;
    }

    public static PropertiesConfiguration makeConfigFromStream(@NotNull final String resource) {
        final PropertiesConfiguration config = newConfiguration();
        try (InputStream input = PropertyManager.class.getResourceAsStream("/" + resource)) {
            if (input != null)
                new FileHandler(config).load(input);

        } catch (ConfigurationException | IOException e) {
            System.err.println("Could not load properties from " + resource);
            e.printStackTrace();
        }

        return config;
    }

    public static PropertiesConfiguration makeConfigFromStream(@NotNull final InputStream input) throws ConfigurationException {
        final PropertiesConfiguration config = newConfiguration();
        new FileHandler(config).load(input);
        return config;
    }
}