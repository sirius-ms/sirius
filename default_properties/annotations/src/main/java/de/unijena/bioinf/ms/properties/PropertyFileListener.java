package de.unijena.bioinf.ms.properties;

import java.util.Properties;

public interface PropertyFileListener {
    void propertiesFileChanged(Properties nuProperties);
}
