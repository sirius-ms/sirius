/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import de.unijena.bioinf.fingerid.connection_pooling.ConnectionPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a single connection and Pools channels of this connection.
 * It also maintains connection restoring and based on that channel restoring
 */
public class RabbitMqConnector implements ConnectionPool.Connector<Channel> {
    private final ConnectionFactory factory;
    private final Map<String,String> exchanges;

    public RabbitMqConnector(ConnectionFactory factory) {
        this.factory = factory;
        this.exchanges = new ConcurrentHashMap<>();
    }

    //todo fill me
    @Override
    public Channel open() throws IOException {
        return null;
    }

    @Override
    public void close(Channel connection) throws IOException {

    }

    @Override
    public boolean isValid(Channel connection) {
        return false;
    }

    public String addExchange(String key, String value) {
        return exchanges.put(key, value);
    }

    public String removeExchange(String key) {
        return exchanges.remove(key);
    }
}
