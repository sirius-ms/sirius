package de.unijena.bioinf.ChemistryBase.chem.utils;

import java.io.*;

public abstract class DistributionReader {

    public abstract IsotopicDistribution read(Reader reader) throws IOException;

    public IsotopicDistribution fromFile(File f) throws IOException  {
        return fromInputStream(new FileInputStream(f));
    }

    public IsotopicDistribution fromClassPath(String name) throws IOException {
        return fromInputStream(DistributionReader.class.getResourceAsStream(name));
    }

    public IsotopicDistribution fromInputStream(InputStream json) throws IOException {
        return read(new InputStreamReader(json));
    }


}
