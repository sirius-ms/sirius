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
import de.unijena.bioinf.ms.properties.PropertyManager;

import java.io.IOException;
import java.util.Map;

public class RabbitUtils {
    public final static String JOB_EX = PropertyManager.getProperty("", null, "jobs");
    public final static String DATA_EX = PropertyManager.getProperty("", null, "data");
//    public final static String MANAGE_EX = PropertyManager.getProperty("", null, "manage");

    public final static String SIRIUS_IN_EX = PropertyManager.getProperty("", null, "sirius.client.in");
    public final static String SIRIUS_OUT_EX = PropertyManager.getProperty("", null, "sirius.client.out");

    public final static String REGISTER_CLIENT_EX = PropertyManager.getProperty("", null, "client.register");
    public final static String DEAD_JOB_EX = PropertyManager.getProperty("", null, "jobs.dead");
    public final static String WORKER_EX = PropertyManager.getProperty("", null, "worker");


    public static void ensureBaseInfrastructure(Channel channel) throws IOException {
        //Alternative queue to manage clients
        channel.exchangeDeclare(REGISTER_CLIENT_EX, BuiltinExchangeType.FANOUT, true, false, true, null);
        channel.queueDeclare(REGISTER_CLIENT_EX, true, false, false, null);
        channel.queueBind(REGISTER_CLIENT_EX, REGISTER_CLIENT_EX, "");

        //dead letter queue for dead job messages
        channel.exchangeDeclare(DEAD_JOB_EX, BuiltinExchangeType.FANOUT, true, false, true, null);
        channel.queueDeclare(DEAD_JOB_EX, true, false, false, null);
        channel.queueBind(DEAD_JOB_EX, DEAD_JOB_EX, "");

        channel.exchangeDeclare(WORKER_EX, BuiltinExchangeType.TOPIC, true, false, false, null);

        channel.exchangeDeclare(JOB_EX, BuiltinExchangeType.DIRECT, true, false, false, Map.of("alternate-exchange", REGISTER_CLIENT_EX));
        channel.exchangeDeclare(DATA_EX, BuiltinExchangeType.DIRECT, true, false, false, null);

        channel.exchangeDeclare(SIRIUS_IN_EX, BuiltinExchangeType.TOPIC, true, false, false, null);
        channel.exchangeDeclare(SIRIUS_OUT_EX, BuiltinExchangeType.TOPIC, true, false, false, null);

        channel.exchangeBind(DATA_EX, SIRIUS_IN_EX, "data.#");
        channel.exchangeBind(JOB_EX, SIRIUS_IN_EX, "worker.#");
    }

}
