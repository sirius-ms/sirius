package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;

import java.io.File;
import java.io.IOException;

public class LCMSParsing {

    public static LCMSRun parseRun(File source, SpectrumStorage storage) throws IOException {
        if (source.getName().toLowerCase().endsWith(".mzml")) {
            return parseRunFromMzMl(source, storage);
        } else if (source.getName().toLowerCase().endsWith(".mzxml")) {
            return parseRunFromMzXml(source, storage);
        }
        throw new IOException("Illegal file extension. Only .mzml and .mzxml are supported");
    }

    public static LCMSRun parseRunFromMzXml(File source, SpectrumStorage storage) throws IOException {
        return new MzXMLParser().parse(source, storage);
    }

    public static LCMSRun parseRunFromMzMl(File source, SpectrumStorage storage) throws IOException {
        return new MzMLParser().parse(source, storage);
    }
}
