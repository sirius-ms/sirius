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
import de.unijena.bioinf.rabbitmq.RabbitMqChannelPool;
import de.unijena.bioinf.storage.blob.BlobStorage;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of the SIRIUS BlobStorage Client API based on the AMQP protocol using RabbitMQ
 * Can e.g. be used to communicate wit the Backend Data-Service
 */
public class RabbitMqBlobStorage implements BlobStorage {
    enum ResourceRequest {
        EXISTS, GET, SET, DELETE
    }

    protected final ConnectionPool<Channel> channelPool;
    protected final String routingKey;
    protected final String exchange;

    public RabbitMqBlobStorage(@NotNull ConnectionFactory client, @NotNull String routingKey, @Nullable String exchange) {
        this(new RabbitMqChannelPool(client), routingKey, exchange == null || exchange.isBlank() ? "" : exchange);
    }

    public RabbitMqBlobStorage(@NotNull RabbitMqChannelPool channelPool, @NotNull String routingKey, @NotNull String exchange) {
        if (routingKey.isBlank())
            throw new IllegalArgumentException("RabbitMQ routing key cannot be empty");
        this.routingKey = routingKey;
        this.exchange = exchange;
        this.channelPool = channelPool;
        //add exchanges and queues to connector to ensure that they will be available when a channel is requested
        channelPool.putExchange(this.exchange, BuiltinExchangeType.DIRECT); //all channels will have this exchange
        channelPool.addBinding(routingKey, exchange, routingKey); // queue named like the routing key since we have a direct exchange anyways
    }


    @Override
    public String getName() {
        return routingKey;
    }

    @Override
    public boolean hasBlob(Path relative) throws IOException {
        RpcClient.Response resp = rpcRequest(relative, ResourceRequest.EXISTS);
        if (resp.getProperties().getBodySize() == 0)
            throw new IOException("Error during RPC call: 'EMPTY Response body!'");

        return Boolean.parseBoolean(new String(resp.getBody(), Charset.forName(resp.getProperties().getContentEncoding())));
    }

    protected RpcClient.Response rpcRequest(@NotNull Path relative, @NotNull ResourceRequest requestType) throws IOException {
        return rpcRequest(relative, requestType, null);
    }

    protected RpcClient.Response rpcRequest(@NotNull Path relative, @NotNull ResourceRequest requestType, byte[] message) throws IOException {
        try (PooledConnection<Channel> c = channelPool.orderConnection()) {
            String callbackQueueName = c.connection.queueDeclare().getQueue();
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder().contentEncoding(getCharset().name())
                    .replyTo(callbackQueueName).headers(Map.of("path", relative, "request", requestType)).build();

            RpcClient rpcClient = new RpcClient(new RpcClientParams().channel(c.connection).replyTo(callbackQueueName).exchange(exchange).routingKey(routingKey));
            return rpcClient.doCall(props, message != null ? message : new byte[]{});
        } catch (InterruptedException | TimeoutException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void withWriter(Path relative, IOFunctions.IOConsumer<OutputStream> withStream) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            withStream.accept(out);
            final RpcClient.Response resp = rpcRequest(relative, ResourceRequest.SET, out.toByteArray());
            if (resp.getProperties().getBodySize() != 0)
                throw new IOException("Error during RPC call: \n" + new String(resp.getBody(), Charset.forName(resp.getProperties().getContentEncoding())));
        }
    }

    @Override
    public InputStream reader(Path relative) throws IOException {
        RpcClient.Response resp = rpcRequest(relative, ResourceRequest.GET);
        return new ByteArrayInputStream(resp.getBody());
    }

    @Override
    public Iterator<Blob> listBlobs() throws IOException {
        throw new NotImplementedException("TOOD: implement Endpoint to list blobs"); //TODO implement list blobs
    }

    @Override
    public void close() throws IOException {
        channelPool.close();
    }
}
