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
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AmqpClient {
    public static long JOB_TIME_OUT = PropertyManager.getLong("de.unijena.bioinf.fingerid.web.job.timeout", 1000L * 60L * 60L); //default 1h
    protected static final String REGISTER_PREFIX = PropertyManager.getProperty("de.unijena.bioinf.ms.sirius.amqp.prefix.register", null, "register");
    protected static final String CLIENT_EXCHANGE = PropertyManager.getProperty("", null, "sirius.client.in");
    protected static final String CLIENT_TYPE = PropertyManager.getProperty("de.unijena.bioinf.ms.sirius.amqp.client", null, "sirius");
    protected final String clientID; //aka session key to allow multiple queues per userid -> multiple clients
    protected final String userID;
    protected final RabbitMqChannelPool channelPool;


    protected final Map<String, AmqpWebJJob<?, ?, ?>> messageJobs = new ConcurrentHashMap<>();
    protected final List<String> consumers = new ArrayList<>();
    protected final String consumerQ;
    protected final String registerRKey;
    protected final int threads;

    public AmqpClient(@NotNull RabbitMqChannelPool channelPool, @NotNull String userID, @NotNull String clientID, int threads) {
        this.channelPool = channelPool;
        this.userID = userID;
        this.clientID = clientID;
        this.threads = threads;
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

        consumers.add(NetUtils.tryAndWaitAsJJob(() -> {
            final Channel channel = channelPool.orderConnection().connection;
            return channel.basicConsume(consumerQ, false,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope,
                                                   AMQP.BasicProperties properties, byte[] body) throws IOException {
                            long deliveryTag = envelope.getDeliveryTag();
                            //submit handling to SIRIUS Jobs System
                            SiriusJobs.getGlobalJobManager().submitJob(new AMPQCallbackJJob(consumerTag, properties, body));
                            channel.basicAck(deliveryTag, false);
                        }
                    });

        }, timeout));
    }

    //todo save publishing mechanism that tracks jobs

    public  <T> void publish(@NotNull String routingPrefix, T jacksonSerializable) throws IOException {
        publish(routingPrefix, jacksonSerializable, (body) -> new ObjectMapper().writeValueAsString(body));
    }

    public <T> void publish(@NotNull String routingPrefix, T body, IOFunctions.IOFunction<T,String> jsonizer) throws IOException {
        publish(routingPrefix,jsonizer.apply(body));
    }

    public void publish(@NotNull String routingPrefix, String jsonBody) {
        publish(routingPrefix, jsonBody.getBytes(ContentType.APPLICATION_JSON.getCharset()));
    }

    public void publish(@NotNull String routingPrefix, byte[] body) {
        NetUtils.tryAndWaitAsJJob(() -> {
            try (PooledConnection<Channel> connection = channelPool.orderConnection()) {
                connection.connection.basicPublish(CLIENT_EXCHANGE, decorateRoutingPrefix(routingPrefix), defaultProps().build(), body);
                connection.connection.waitForConfirms(5000);
            }
        }, 30000);
    }


    private String decorateRoutingPrefix(String prefix) {
        return prefix + "." + userID + "." + clientID;
    }

    private AMQP.BasicProperties.Builder defaultProps() {
        return new AMQP.BasicProperties.Builder().contentEncoding(ContentType.APPLICATION_JSON.getCharset().name()).contentType(ContentType.APPLICATION_JSON.getMimeType()).appId("SIRIUS");
    }


    public class AMPQCallbackJJob extends BasicJJob<JobMessage<?>> {

        final AMQP.BasicProperties properties;
        final byte[] body;
        private final String consumerTag;

        public AMPQCallbackJJob(String consumerTag, AMQP.BasicProperties properties, byte[] body) {
            super(JobType.CPU);
            this.consumerTag = consumerTag;
            this.properties = properties;
            this.body = body;
        }

        @Override
        protected JobMessage<?> compute() throws Exception {
            JobMessage<?> message = new ObjectMapper().readValue(body, new TypeReference<>() {
            });
            AmqpWebJJob<?, ?, ?> job = messageJobs.get(message.getJobId());
            assert job.getJobId().equals(message.getJobId());
            job.update(message);
            return message;
        }
    }
}
