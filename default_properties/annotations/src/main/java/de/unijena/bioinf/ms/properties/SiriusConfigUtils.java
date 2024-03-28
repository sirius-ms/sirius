/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.properties;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.builder.fluent.PropertiesBuilderParameters;
import org.apache.commons.configuration2.convert.DisabledListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class SiriusConfigUtils {


    private static ReadWriteSynchronizer getSynchronizer() {
        return new ReadWriteSynchronizer();
    }


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
        PropertiesConfiguration c;
        try {
            c = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class).configure(makeConfigProps(file)).getConfiguration();
        } catch (ConfigurationException e) {
            System.err.println("WARNING: Error during PropertiesConfiguration initialization");
            e.printStackTrace();
            c = new PropertiesConfiguration();
        }
        c.setSynchronizer(getSynchronizer());
        return c;
    }

    public static @NotNull PropertiesConfiguration newConfiguration(@NotNull PropertyFileWatcher watcher) {
        PropertiesConfiguration c;
        try {
            ReloadingFileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new ReloadingFileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                            .configure(makeConfigProps(watcher.getFile().toFile()));
            watcher.setController(builder.getReloadingController());
            c = builder.getConfiguration();

        } catch (ConfigurationException e) {
            System.err.println("WARNING: Error during PropertiesConfiguration initialization with auto reloading");
            e.printStackTrace();
            c = new PropertiesConfiguration();
        }
        c.setSynchronizer(getSynchronizer());
        return c;
    }


    public static LinkedHashSet<String> parseResourcesLocation(@Nullable final String locations) {
        return parseResourcesLocation(locations, null);
    }

    public static LinkedHashSet<String> parseResourcesLocation(@Nullable final String locations, @Nullable final String defaultLocation) {
        LinkedHashSet<String> resources = new LinkedHashSet<>();
        if (defaultLocation != null && !defaultLocation.isBlank())
            resources.add(defaultLocation);

        if (locations != null && !locations.isBlank())
            resources.addAll(Arrays.asList(locations.trim().split("\\s*,\\s*")));

        resources.removeIf(String::isBlank);
        return resources;
    }

    //this reads and merges read only properties from within jar resources
    public static CombinedConfiguration makeConfigFromResources(@NotNull final LinkedHashSet<String> resources, @Nullable PropertiesConfigurationLayout layout) {
        return makeConfigFromResources(SiriusConfigUtils.newCombinedConfiguration(), resources, layout);
    }

    public static CombinedConfiguration makeConfigFromResources(@NotNull CombinedConfiguration configToAddTo, @NotNull final LinkedHashSet<String> resources, @Nullable PropertiesConfigurationLayout layout) {
        List<String> reverse = new ArrayList<>(resources);
        Collections.reverse(reverse);
        for (String resource : reverse) {
            configToAddTo.addConfiguration(makeConfigFromStream(resource, layout), resource);
        }

        return configToAddTo;
    }

    public static PropertiesConfiguration makeConfigFromStream(@NotNull final String resource, @Nullable PropertiesConfigurationLayout layout) {
        final PropertiesConfiguration config = newConfiguration();
        try (InputStream input = PropertyManager.class.getResourceAsStream("/" + resource)) {
            if (input != null) {
                if (layout != null) {
                    layout.load(config, new InputStreamReader(input));
                } else {
                    new FileHandler(config).load(input);
                }
            }

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

    public static PropertiesConfiguration makeConfigFromMap(@Nullable final Map<String, String> values) {
        final PropertiesConfiguration config = newConfiguration();
        if (values != null)
            values.forEach(config::setProperty);
        return config;
    }


}