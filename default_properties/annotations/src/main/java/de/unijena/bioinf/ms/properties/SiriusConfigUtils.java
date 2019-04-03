package de.unijena.bioinf.ms.properties;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

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
}
