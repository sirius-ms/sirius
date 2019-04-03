package de.unijena.bioinf.ms.properties;

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class SiriusConfigUtils {
    public static CombinedConfiguration newCombinedConfiguration() {
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

    public static PropertiesConfiguration newConfiguration() {
        return newConfiguration(null);
    }

    public static @NotNull PropertiesConfiguration newConfiguration(@Nullable Path file) {
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

    //this reads and merges read only properties from within jar resources
    public static CombinedConfiguration makeConfigFromResources(@NotNull final LinkedHashSet<String> resources, @Nullable String prefixToAdd, @Nullable String name) {
        name = (name == null || name.isEmpty()) ? String.join("_", resources) : name;
        final CombinedConfiguration combined = SiriusConfigUtils.newCombinedConfiguration();
        List<String> reverse = new ArrayList<>(resources);
        Collections.reverse(reverse);
        for (String resource : reverse) {
            combined.addConfiguration(makeConfigFromStream(resource), resource);
        }

        return combined;
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