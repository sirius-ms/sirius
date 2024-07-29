/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid.connection_pooling;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;

public interface PooledDB<T> extends Closeable, AutoCloseable {
    void refresh() throws IOException;

    <R> R useConnection(PoolFunction<T, R> runWithConnection) throws SQLException, IOException, InterruptedException;

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
