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

package de.unijena.bioinf.ms.amqp.client.jobs;

import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.rabbitmq.RabbitMqChannelPool;
import org.jetbrains.annotations.NotNull;

public class JobsClient {

    protected static final String QUEUE_CATEGORY = PropertyManager.getProperty("", null, "worker");
    protected static final String WORKER_QUEUE_TYPE = PropertyManager.getProperty("", null, "job");
    protected static final String WORKER_SUFFIX = PropertyManager.getProperty("", null, "ce");

    protected final String userID;
    protected final String clientID;
    protected final RabbitMqChannelPool channelPool;

    public JobsClient(@NotNull RabbitMqChannelPool channelPool, @NotNull String userID, @NotNull String clientID) {
        this.channelPool = channelPool;
        this.userID = userID;
        this.clientID = clientID;
    }


    protected String routingKey(String jobType, String ionMode) {
        return QUEUE_CATEGORY + "." + jobType + "." + ionMode + "." + WORKER_SUFFIX + "." + WORKER_QUEUE_TYPE + "." + userID + "." + clientID;
    }


    public void submitJob(){
//        channelPool.orderConnection().connection.basicConsume()
    }



}
