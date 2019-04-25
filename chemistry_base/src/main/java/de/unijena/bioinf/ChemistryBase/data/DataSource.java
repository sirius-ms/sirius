package de.unijena.bioinf.ChemistryBase.data;

import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class DataSource implements DataAnnotation, TreeAnnotation, Ms2ExperimentAnnotation {

    private final URL url;

    public static DataSource fromString(String x) {
        try {
            return new DataSource(URI.create(x).toURL());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public DataSource(URL url) {
        this.url = url;
    }

    public DataSource(File f) {
        try {
            this.url = f.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public URL getUrl() {
        return url;
    }

    public String toString() {
        return url.toString();
    }
}
