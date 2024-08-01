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

import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public final class PersistentProperties {
    protected static final Logger LOGGER = LoggerFactory.getLogger(PersistentProperties.class);

    protected final File propertiesFile;
    protected final PropertiesConfiguration config;
    protected final FileHandler fileHandler;
    protected final PropertyFileWatcher watcher;


    protected PersistentProperties(File propertiesFile, PropertiesConfiguration baseProps, boolean watchFile) {
        if (!propertiesFile.exists()) {
            try {
                new FileHandler(baseProps).save(propertiesFile);
            } catch (ConfigurationException e) {
                LOGGER.error("Could NOT write default Properties from given properties file, falling back to default properties. Property changes during Runtime may NOT persistent!", e);
            }
        }

        this.propertiesFile = propertiesFile;
        if (watchFile) {
            this.watcher = new PropertyFileWatcher(propertiesFile.toPath());
            this.config = SiriusConfigUtils.newConfiguration(watcher);
        } else {
            this.config = SiriusConfigUtils.newConfiguration(propertiesFile);
            this.watcher = null;
        }
        fileHandler = new FileHandler(config);
    }

    public void setProperty(String key, String value) {
        config.setProperty(key, value);
    }


    public synchronized void setAndStoreProperty(String propertyName, String propertyValue) {
        setProperty(propertyName, propertyValue);
        store();
    }

    public void setProperties(Properties properties) {
        properties.forEach((k, v) -> setProperty((String) k, (String) v));
    }

    public synchronized void setAndStoreProperties(Properties properties) {
        setProperties(properties);
        store();
    }

    public PropertyFileWatcher startWatcher(JobManager manager) {
        if (watcher == null) return null;
        return manager.submitJob(watcher);
    }

    public void stopWatcher() {
        if (watcher != null)
            watcher.stop();
    }

    public boolean isWatching() {
        if (watcher == null) return false;
        return watcher.getState() == JJob.JobState.RUNNING;
    }

    public synchronized void store() {
        try {
            fileHandler.save(propertiesFile);
        } catch (ConfigurationException e) {
            LOGGER.error("Could not save new Properties file! Changes not saved!", e);
        }
    }

    public Properties asProperties(){
        return PropertyManager.asProperties(config);
    }

}
