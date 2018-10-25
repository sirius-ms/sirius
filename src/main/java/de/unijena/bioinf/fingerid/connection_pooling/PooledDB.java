package de.unijena.bioinf.fingerid.connection_pooling;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;

public interface PooledDB extends Closeable {
    void refresh() throws IOException;

    /*
     * This method should catch all connection related exceptions.
     * Only pool based exception should pass.
     * */
    boolean hasConnection(int timeout) throws SQLException, IOException, InterruptedException;

    default boolean hasConnection() throws SQLException, IOException, InterruptedException {
        return hasConnection(30);
    }

    int getMaxConnections();

    int getNumberOfIdlingConnections();
}
