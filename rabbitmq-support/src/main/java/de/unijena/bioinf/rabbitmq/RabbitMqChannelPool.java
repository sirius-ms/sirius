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

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.fingerid.connection_pooling.ConnectionPool;
import de.unijena.bioinf.fingerid.connection_pooling.PooledConnection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;


public class RabbitMqChannelPool extends ConnectionPool<Channel> {
    protected final AtomicBoolean refreshBindings = new AtomicBoolean(false);

    final Lock decorationLock = new ReentrantLock();
    private final Map<String, BuiltinExchangeType> exchanges;
    private final Set<Binding> queueExchBinds;


    public RabbitMqChannelPool(ConnectionFactory factory, int capacity) {
        super(new RabbitMqConnector(factory), capacity);
        this.exchanges = new HashMap<>();
        this.queueExchBinds = new HashSet<>();
    }

    public RabbitMqChannelPool(ConnectionFactory factory) {
        this(factory, 5);
    }

    @Override
    public PooledConnection<Channel> orderConnection() throws InterruptedException, IOException {
        synchronized (refreshBindings) { // refresh queued changes in rabbitmq infrastructure
            final PooledConnection<Channel> rc = super.orderConnection();
            if (refreshBindings.get())
                decorateChannel(rc.connection);
            return rc;
        }
    }

    protected void changeAnRefresh(Runnable doWith) {
        changeAnRefresh(() -> {
            doWith.run();
            return true;
        });
    }

    protected <R> R changeAnRefresh(Supplier<R> doWith) {
        try {
            connectionLock.lock();
            R it = withDecorationLock(doWith);
            refreshBindings.set(true);
            return it;
        } finally {
            connectionLock.unlock();
        }
    }

    void withDecorationLock(Runnable doWith) {
        withDecorationLock(() -> {
            doWith.run();
            return true;
        });
    }

    <T> T withDecorationLock(Supplier<T> doWith) {
        try {
            decorationLock.lock();
            return doWith.get();
        } finally {
            decorationLock.unlock();
        }
    }

    <R> R withDecorationLockIO(IOFunctions.IOSupplier<R> doWith) throws IOException {
        try {
            decorationLock.lock();
            return doWith.get();
        } finally {
            decorationLock.unlock();
        }
    }

    void withDecorationLockIO(IOFunctions.IORunnable doWith) throws IOException {
        withDecorationLockIO(() -> {
            doWith.run();
            return true;
        });
    }


    protected Channel decorateChannel(@NotNull Channel channel) throws IOException {
        return withDecorationLockIO(() -> {
            for (Map.Entry<String, BuiltinExchangeType> e : exchanges.entrySet())
                channel.exchangeDeclare(e.getKey(), e.getValue(), false, false, null);
            for (Binding binding : queueExchBinds) {
                channel.queueDeclare(binding.gueue, false, false, true, null);
                channel.queueBind(binding.gueue, binding.exchange, binding.routingKey, null);
            }
            return channel;
        });
    }


    public void putExchanges(Map<String, BuiltinExchangeType> values) {
        changeAnRefresh(() -> exchanges.putAll(values));
    }

    public BuiltinExchangeType putExchange(String key, BuiltinExchangeType value) {
        return changeAnRefresh(() -> exchanges.put(key, value));
    }

    public BuiltinExchangeType removeExchange(String key) {
        return changeAnRefresh(() -> exchanges.remove(key));
    }

    public void removeExchanges(Iterable<String> keys) {
        changeAnRefresh(() -> keys.forEach(exchanges::remove));
    }

    public boolean addBinding(@NotNull String gueue, @NotNull String exchange, @NotNull String routingKey) {
        return addBinding(Binding.of(gueue, exchange, routingKey));
    }

    public boolean addBinding(Binding binding) {
        return changeAnRefresh(() -> queueExchBinds.add(binding));
    }

    public void addBindings(Iterable<Binding> bindings) {
        changeAnRefresh(() -> bindings.forEach(queueExchBinds::add));
    }

    public boolean removeBinding(@NotNull String gueue, @NotNull String exchange, @NotNull String routingKey) {
        return removeBinding(Binding.of(gueue, exchange, routingKey));
    }

    public boolean removeBinding(Binding binding) {
        return changeAnRefresh(() -> queueExchBinds.remove(binding));
    }

    public void removeBinding(Iterable<Binding> binding) {
        changeAnRefresh(() -> binding.forEach(queueExchBinds::remove));
    }


    public static class Binding {
        @NotNull
        final String gueue;
        @NotNull
        final String exchange;
        @NotNull
        final String routingKey;

        private Binding(@NotNull String gueue, @NotNull String exchange, @NotNull String routingKey) {
            this.gueue = gueue;
            this.exchange = exchange;
            this.routingKey = routingKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Binding)) return false;
            Binding binding = (Binding) o;
            return gueue.equals(binding.gueue) && exchange.equals(binding.exchange) && routingKey.equals(binding.routingKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gueue, exchange, routingKey);
        }

        static Binding of(@NotNull String gueue, @NotNull String exchange, @NotNull String routingKey) {
            return new Binding(gueue, exchange, routingKey);
        }
    }
}
