package de.unijena.bioinf.lcms;

import de.unijena.bioinf.lcms.trace.LCMSStorage;

import java.io.IOException;

public interface LCMSStorageFactory {

    public LCMSStorage createNewStorage() throws IOException;

}
