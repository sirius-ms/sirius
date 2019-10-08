package de.unijena.bioinf.ChemistryBase.ms;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

// TODO: not supported yet
public class MultipleSources extends SourceLocation {

    protected Set<URL> allSources;

    public static MultipleSources leastCommonAncestor(File... sources) {
        try {
            String a = sources[0].getAbsolutePath();
            String b = sources[sources.length - 1].getAbsolutePath();
            for (int k = 0; k < Math.min(a.length(), b.length()); ++k) {
                if (a.charAt(k) != b.charAt(k)) {
                    return new MultipleSources(new File(a.substring(0, k)).toURI().toURL(), Arrays.stream(sources).map(x -> {
                        try {
                            return x.toURI().toURL();
                        } catch (MalformedURLException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }).toArray(URL[]::new));
                }
            }
            return new MultipleSources(sources[0].toURI().toURL(), Arrays.stream(sources).map(x -> {
                try {
                    return x.toURI().toURL();
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            }).toArray(URL[]::new));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public MultipleSources(URL parent, URL... sources) {
        super(parent);
        this.allSources = new HashSet<>(Arrays.asList(sources));
    }

    public Set<URL> getAllSources() {
        return allSources;
    }

    @Override
    public String toString() {
        return allSources.stream().map(x->x.toString()).collect(Collectors.joining(";"));
    }
}
