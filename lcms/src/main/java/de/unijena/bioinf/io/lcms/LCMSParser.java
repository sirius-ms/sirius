package de.unijena.bioinf.io.lcms;

import de.unijena.bioinf.lcms.SpectrumStorage;
import de.unijena.bioinf.model.lcms.LCMSRun;

import java.io.File;
import java.io.IOException;

public interface LCMSParser {
    LCMSRun parse(File file, SpectrumStorage storage) throws IOException;

}