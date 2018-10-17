package de.unijena.bioinf.ChemistryBase.utils;

import java.io.Closeable;

public interface PooledDB extends Closeable {
    void refresh() throws Exception;

    int getMaxConnections();
    int getNumberOfIdlingConnections();
}
