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

package de.unijena.bioinf.storage.blob.amqp;

import com.rabbitmq.client.*;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.connection_pooling.ConnectionPool;
import de.unijena.bioinf.fingerid.connection_pooling.PooledConnection;
import de.unijena.bioinf.rabbitmq.RabbitMqConnector;
import de.unijena.bioinf.storage.blob.BlobStorage;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitMqBlobStorage implements BlobStorage {
    enum ResourceRequest{
        EXISTS, GET, SET, DELETE
    }
    protected final ConnectionPool<Channel> channelPool;
    protected final String routingKey;
    protected final String exchange;


    public RabbitMqBlobStorage(ConnectionFactory client, String routingKey, String exchange) {
        this.routingKey = routingKey;
        this.exchange = exchange;
        final RabbitMqConnector connector = new RabbitMqConnector(client);
        connector.addExchange(this.exchange, "direct"); //all channels will have this exchange
        this.channelPool = new ConnectionPool<>(connector);
    }


    @Override
    public String getName() {
        return routingKey;
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        return false;
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            withStream.accept(out);
            try (PooledConnection<Channel> c = channelPool.orderConnection()){
                String callbackQueueName = c.connection.queueDeclare().getQueue();
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().contentEncoding(getCharset().name())
                        .replyTo(callbackQueueName).headers(Map.of("path", relative, "request", ResourceRequest.SET)).build();

                RpcClient rpcClient = new RpcClient(new RpcClientParams().channel(c.connection).replyTo(callbackQueueName).exchange(exchange).routingKey(routingKey));
                RpcClient.Response resp = rpcClient.doCall(props, out.toByteArray());
                if (resp.getProperties().getBodySize() != 0)
                    throw new IOException("Error duting RPC call: \n" + new String(resp.getBody(), Charset.forName(resp.getProperties().getContentEncoding())));
            } catch (InterruptedException | TimeoutException e) {
                throw  new IOException(e);
            }
        }
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {
        channelPool.close();
    }
}
