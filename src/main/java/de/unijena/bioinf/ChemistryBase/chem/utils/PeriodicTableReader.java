package de.unijena.bioinf.ChemistryBase.chem.utils;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.io.*;

public abstract class PeriodicTableReader {

    public abstract void read(PeriodicTable table, Reader reader) throws IOException;

    public void fromFile(PeriodicTable table, File f) throws IOException  {
        fromInputStream(table, new FileInputStream(f));
    }

    public void fromClassPath(PeriodicTable table, String name) throws IOException {
        fromInputStream(table, DistributionReader.class.getResourceAsStream(name));
    }

    public void fromInputStream(PeriodicTable table, InputStream json) throws IOException {
        read(table, new InputStreamReader(json));
    }

}
