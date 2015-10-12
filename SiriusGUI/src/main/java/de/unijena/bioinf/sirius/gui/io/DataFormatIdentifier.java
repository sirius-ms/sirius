package de.unijena.bioinf.sirius.gui.io;

import java.io.File;

public class DataFormatIdentifier {

    public DataFormatIdentifier() {
    }

    public DataFormat identifyFormat(File f) {
        final String name = f.getName().toLowerCase();
        if (name.endsWith(".mgf")) return DataFormat.MGF;
        if (name.endsWith(".csv") || name.endsWith(".tsv")) return DataFormat.CSV;
        if (name.endsWith(".ms") || name.endsWith(".ms2")) return DataFormat.JenaMS;
        return DataFormat.NotSupported;
    }

}
