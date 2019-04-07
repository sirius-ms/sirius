package de.unijena.bioinf.ms.properties;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

//todo this should be combineable with Property FileWatcherService -> use configuration file watching
public class PersistentProperties {
    protected static final Logger LOGGER = LoggerFactory.getLogger(PersistentProperties.class);
    protected final PropertiesConfiguration config;


    public PersistentProperties(File propertiesFile, PropertiesConfiguration baseProps) {
        try {
            if (!propertiesFile.exists())
                new org.apache.commons.configuration2.io.FileHandler(baseProps).save(propertiesFile);
            baseProps = PropertyManager.loadPersistentPropertiesFile(propertiesFile);
        } catch (ConfigurationException e) {
            LOGGER.error("Could NOT load Properties from given properties file, falling back to default properties. Property changes during Runtime are NOT persistent!", e);
        }
        config = baseProps;
    }

    public void setProperty(String key, String value) {
        config.setProperty(key, value);
    }


    public void setAndStoreProperty(String propertyName, String propertyValue) {
        setProperty(propertyName, propertyValue);
        store();
    }

    public void setProperties(Properties properties) {
        properties.forEach((k, v) -> setProperty((String) k, (String) v));
    }

    public void setAndStoreProperties(Properties properties) {
        setProperties(properties);
        store();
    }

    public String getProperty(String key) {
        return PropertyManager.getProperty(key);
    }

    public synchronized void store() {
        try {
            new org.apache.commons.configuration2.io.FileHandler(config).save();
        } catch (ConfigurationException e) {
            LOGGER.error("Could not save new Properties file! Changes not saved!", e);
        }
    }

}
