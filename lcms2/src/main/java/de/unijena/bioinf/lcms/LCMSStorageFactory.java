package de.unijena.bioinf.lcms;

import de.unijena.bioinf.lcms.trace.LCMSStorage;

import java.io.IOException;

public interface LCMSStorageFactory extends AutoCloseable {

    LCMSStorage createNewStorage() throws IOException;

    default LCMSStorage createNewMergedStorage() throws IOException {
        return createNewStorage();
    }

    @Override
    default void close(){}
}
