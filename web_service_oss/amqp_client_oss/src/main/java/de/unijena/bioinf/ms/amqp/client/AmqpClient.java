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

package de.unijena.bioinf.ms.amqp.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ChemistryBase.utils.NetUtils;
import de.unijena.bioinf.fingerid.connection_pooling.PooledConnection;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.ms.amqp.client.jobs.AmqpWebJJob;
import de.unijena.bioinf.ms.amqp.client.jobs.JobMessage;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.rabbitmq.RabbitMqChannelPool;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class AmqpClient {
    protected static AtomicLong MESSAGE_COUNTER = new AtomicLong(0);

    public static long JOB_TIME_OUT = PropertyManager.getLong("de.unijena.bioinf.fingerid.web.job.timeout", 1000L * 60L * 60L); //default 1h
    protected static final String REGISTER_PREFIX = PropertyManager.getProperty("de.unijena.bioinf.ms.sirius.amqp.prefix.register", null, "register");
    protected static final String CLIENT_EXCHANGE = PropertyManager.getProperty("", null, "sirius.client.in");
    protected static final String CLIENT_TYPE = PropertyManager.getProperty("de.unijena.bioinf.ms.sirius.amqp.client", null, "sirius");
    protected final String clientID; //aka session key to allow multiple queues per userid -> multiple clients
    protected final String userID;
    protected final RabbitMqChannelPool channelPool;

    protected final List<String> consumerThreads = new ArrayList<>();
    protected final String consumerQ;
    protected final String registerRKey;
    protected final int threads;


    protected final Map<String, AmqpWebJJob<?, ?, ?>> messageJobs = new ConcurrentHashMap<>();


    public AmqpClient(@NotNull RabbitMqChannelPool channelPool, @NotNull String userID, @NotNull String clientID, int consumerThreads) {
        this.channelPool = channelPool;
        this.userID = userID;
        this.clientID = clientID;
        this.threads = consumerThreads;
        this.consumerQ = CLIENT_TYPE + "." + userID + "." + clientID;
        this.registerRKey = REGISTER_PREFIX + "." + CLIENT_TYPE + "." + userID + "." + clientID;

    }

    public void startConsuming(long timeout) {
        NetUtils.tryAndWaitAsJJob(() -> {
            try (PooledConnection<Channel> connection = channelPool.orderConnection()) {
                connection.connection.basicPublish(CLIENT_EXCHANGE, registerRKey, defaultProps().build(), new byte[]{});
                connection.connection.waitForConfirms(5000);
            }
        }, timeout);

        AMQP.Queue.DeclareOk ok = NetUtils.tryAndWaitAsJJob(() -> {
            try (PooledConnection<Channel> connection = channelPool.orderConnection()) {
                return connection.connection.queueDeclarePassive(consumerQ);
            }
        }, timeout);

        if (!ok.getQueue().equals(consumerQ))
            throw new IllegalArgumentException("Illegal q name returned from Server");

        LoggerFactory.getLogger(getClass()).info("Successfully created callback queue!");

        consumerThreads.add(NetUtils.tryAndWaitAsJJob(() -> {
            final Channel channel = channelPool.orderConnection().connection;
            return channel.basicConsume(consumerQ, false,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
                            long deliveryTag = envelope.getDeliveryTag();
                            //handle Message, should be submitted to SIRIUS Jobs System to do unwrapping in parallel without
                            // having many connection or blocking them too long
                            SiriusJobs.getGlobalJobManager().submitJob(new AMPQCallbackJJob(consumerTag, properties, body));
                            channel.basicAck(deliveryTag, false);
                        }
                    });

        }, timeout));
    }

    public <T, I, O, R> AmqpWebJJob<I, O, R> publish(@NotNull String routingPrefix, T jacksonSerializable, @NotNull Function<String, AmqpWebJJob<I, O, R>> jobBuilder) throws IOException {
        return publish(routingPrefix, jacksonSerializable, (body) -> new ObjectMapper().writeValueAsString(body), jobBuilder);
    }

    public <T, I, O, R> AmqpWebJJob<I, O, R> publish(@NotNull String routingPrefix, T body, @NotNull IOFunctions.IOFunction<T, String> jsonizer, @NotNull Function<String, AmqpWebJJob<I, O, R>> jobBuilder) throws IOException {
        return publish(routingPrefix, jsonizer.apply(body), jobBuilder);
    }

    public <I, O, R> AmqpWebJJob<I, O, R> publish(@NotNull String routingPrefix, String jsonBody, @NotNull Function<String, AmqpWebJJob<I, O, R>> jobBuilder) throws IOException {
        return publish(routingPrefix, jsonBody.getBytes(StandardCharsets.UTF_8.name()), jobBuilder);
    }

    public <I, O, R> AmqpWebJJob<I, O, R> publish(@NotNull String routingPrefix, byte[] body, @NotNull Function<String, AmqpWebJJob<I, O, R>> jobBuilder) throws IOException {
        //MessageID is used to identify the corresponding webJJob
        //The receiving Service needs to return a JobMessage with this ID.
        final String messageID = routingPrefix + "." + MESSAGE_COUNTER.incrementAndGet();
        final AmqpWebJJob<I, O, R> job = jobBuilder.apply(messageID);
        assert !messageJobs.containsKey(messageID);
        messageJobs.put(messageID, job);
        try (PooledConnection<Channel> connection = channelPool.orderConnection()) {
            connection.connection.basicPublish(CLIENT_EXCHANGE, decorateRoutingPrefix(routingPrefix),
                    defaultProps().messageId(job.getJobId()).build(), body);
            try {
                if (!connection.connection.waitForConfirms(5000))
                    LoggerFactory.getLogger(getClass()).warn("Could not confirm publication of Job '" + messageID + "' Jobs might not be delivered an is likely to timeout.");
            } catch (TimeoutException | InterruptedException e) {
                //should
                LoggerFactory.getLogger(getClass()).warn("Could not confirm publication of Job: " + messageID + System.lineSeparator() + e.getMessage());
            }
            return job;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }


    private String decorateRoutingPrefix(String prefix) {
        return prefix + "." + userID + "." + clientID;
    }

    private AMQP.BasicProperties.Builder defaultProps() {
        return new AMQP.BasicProperties.Builder()
                .contentEncoding(StandardCharsets.UTF_8.name())
                .contentType("application/json")
                .userId(userID)
                .appId("SIRIUS");
    }

    public class AMPQCallbackJJob extends BasicJJob<JobMessage<?>> {
        private final AMQP.BasicProperties properties;
        private final byte[] body;
        private final String consumerTag;

        public AMPQCallbackJJob(String consumerTag, AMQP.BasicProperties properties, byte[] body) {
            this.properties = properties;
            this.body = body;
            this.consumerTag = consumerTag;
        }


        @Override
        protected JobMessage<?> compute() throws Exception {
            JobMessage<?> messageJob = new ObjectMapper().readValue(body, new TypeReference<>() {
            });
            AmqpWebJJob<?, ?, ?> job = messageJobs.get(messageJob.getID());
            assert job.getJobId().equals(messageJob.getID());
            job.update(messageJob);
            return messageJob;
        }
    }
}
