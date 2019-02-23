package de.unijena.bioinf.ms.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;

//todo this should be combineable with Property FileWatcherService
//todo move to Propertyconfiguration
public class PersistentProperties extends Properties {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentProperties.class);
    private final String fileheader;
    private final Path propsSourceFile;

    public PersistentProperties(Path propertiesFile, Properties defaultProps, String fileheader) {
        super();
        this.fileheader = fileheader;
        this.propsSourceFile = propertiesFile;
        putAll(defaultProps);

        if (Files.exists(propsSourceFile)) {
            try (InputStream stream = Files.newInputStream(propsSourceFile)) {
                Properties tmp = new Properties();
                tmp.load(stream);
                putAll(tmp);
            } catch (IOException e) {
                LOGGER.error("Could NOT load Properties from given properties file, falling back to default properties", e);
            }
        }

        PropertyManager.setProperties(this);
    }


    @Override
    public void putAll(Map<?, ?> properties) {
        throw new IllegalArgumentException();
//        PropertyManager.setProperties(properties);
//        super.putAll(properties);
    }

    @Override
    public Object put(Object key, Object value) {
        PropertyManager.setProperty((String) key, value);
        return super.put(key, value);
    }

    @Override
    public Object setProperty(String key, String value) {
        return put(key, value);
    }


    public void setAndStoreProperty(String propertyName, String propertyValue) {
        setProperty(propertyName, propertyValue);
        store();
    }

    public void setProperties(Properties properties) {
        putAll(properties);
    }

    public void setProperties(File properties) throws IOException {
        setProperties(properties.toPath());
    }

    public void setProperties(Path properties) throws IOException {
        Properties p = new Properties();
        p.load(Files.newInputStream(properties));
        putAll(p);
    }

    public void setAndStoreProperties(Properties properties) {
        setProperties(properties);
        store();
    }

    public Properties getCopyOfPersistentProperties() {
        return new Properties(this);
    }

    public synchronized void store() {
        try {
            Files.deleteIfExists(propsSourceFile);
            try (OutputStream stream = Files.newOutputStream(propsSourceFile, StandardOpenOption.CREATE_NEW)) {
                store(stream, fileheader);
            } catch (IOException e) {
                LOGGER.error("Could not save new Properties file! Changes not saved!", e);
            }
        } catch (IOException e) {
            LOGGER.error("Could not remove old Properties file! Changes not saved!", e);
        }
    }
}
