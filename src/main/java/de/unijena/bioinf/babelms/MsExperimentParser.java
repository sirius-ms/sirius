package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.babelms.ms.JenaMsParser;

import java.io.File;
import java.util.HashMap;

public class MsExperimentParser {

    private final HashMap<String, Class<? extends Parser<Ms2Experiment>>> knownEndings;

    public MsExperimentParser() {
        this.knownEndings = new HashMap<String, Class<? extends Parser<Ms2Experiment>>>();
        addKnownEndings();
    }

    public GenericParser<Ms2Experiment> getParser(File f) {
        final String name = f.getName();
        final int i = name.lastIndexOf('.');
        if (i < 0) return null; // no parser found
        final String extName = name.substring(i);
        final Class<? extends Parser<Ms2Experiment>> pc = knownEndings.get(extName);
        if (pc==null) return null;
        try {
            return new GenericParser<Ms2Experiment>(pc.newInstance());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void addKnownEndings() {
        knownEndings.put(".ms", (Class<? extends Parser<Ms2Experiment>>)JenaMsParser.class);
        knownEndings.put(".mgf", MgfParser.class);
    }
}
