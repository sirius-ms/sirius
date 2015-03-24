package de.unijena.bioinf.babelms;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
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
        final String extName = name.substring(name.lastIndexOf('.'));
        final Class<? extends Parser<Ms2Experiment>> pc = knownEndings.get(extName);
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
    }
}
