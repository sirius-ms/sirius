package de.unijena.bioinf.ChemistryBase.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

//todo this should be combineable with Property FileWatcherService
public class PersistentProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentProperties.class);
    private final String fileheader;
    private final Path propsSourceFile;
    private final Properties props;


    /*public PersistentProperties(Path propsSourceFile) throws IOException {
        this(propsSourceFile, "Auto generated Properties File");
    }*/

    public PersistentProperties(Path propertiesFile, Properties defaultProps, String fileheader) {
        this.fileheader = fileheader;
        this.propsSourceFile = propertiesFile;
        this.props = new Properties(defaultProps);

        if (Files.exists(propsSourceFile)) {
            try (InputStream stream = Files.newInputStream(propsSourceFile)) {
                Properties tmp = new Properties();
                tmp.load(stream);
                props.putAll(tmp);
            } catch (IOException e) {
                LOGGER.error("Could NOT load Properties from given properties file, falling back to default properties", e);
            }
        }

        PropertyManager.PROPERTIES.putAll(props);
    }


    /*public PersistentProperties(Path propsSourceFile, String fileheader) throws IOException {
        this.fileheader = fileheader;
        this.propsSourceFile = propsSourceFile;
        props = new Properties();
        props.load(Files.newInputStream(propsSourceFile, StandardOpenOption.READ));
    }*/

    public void addProperties(File properties) throws IOException {
        addProperties(properties.toPath());
    }

    public void addProperties(Path properties) throws IOException {
        Properties p = new Properties();
        p.load(Files.newInputStream(properties));
        addProperties(p);
    }

    public void addProperties(Properties properties) {
        PropertyManager.PROPERTIES.putAll(properties);
        PropertyManager.PROPERTIES.putAll(props);
    }

    public void addProperty(String propertyName, String propertyValue) {
        PropertyManager.PROPERTIES.setProperty(propertyName, propertyValue);
        PropertyManager.PROPERTIES.putAll(props);
    }

    public void addPropertyPersistent(String propertyName, String propertyValue) {
        addProperty(propertyName, propertyValue);
        storeProperties();
    }


    public void changeProperties(Properties properties) {
        PropertyManager.PROPERTIES.putAll(properties);
        props.putAll(properties);
    }

    public void changeProperty(String propertyName, String propertyValue) {
        PropertyManager.PROPERTIES.setProperty(propertyName, propertyValue);
        props.setProperty(propertyName, propertyValue);
    }

    public void changePropertiesPersistent(Properties properties) {
        changeProperties(properties);
        storeProperties();
    }


    public void changePropertyPersistent(String propertyName, String propertyValue) {
        changeProperty(propertyName, propertyValue);
        storeProperties();
    }

    public Properties getCopyOfPersistentProperties() {
        return new Properties(props);
    }

    private void storeProperties() {
        try {
            Files.deleteIfExists(propsSourceFile);
            try (OutputStream stream = Files.newOutputStream(propsSourceFile, StandardOpenOption.CREATE_NEW)) {
                props.store(stream, fileheader);
            } catch (IOException e) {
                LOGGER.error("Could not save new Properties file! Changes not saved!", e);
            }
        } catch (IOException e) {
            LOGGER.error("Could not remove old Properties file! Changes not saved!", e);
        }
    }
}
