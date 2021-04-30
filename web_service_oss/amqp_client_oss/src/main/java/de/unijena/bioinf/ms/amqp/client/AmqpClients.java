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

import de.unijena.bioinf.ms.properties.PropertyManager;

public class AmqpClients {

    protected static final String QUEUE_CATEGORY = PropertyManager.getProperty("", null, "worker");
    protected static final String WORKER_QUEUE_TYPE = PropertyManager.getProperty("", null, "job");
    protected static final String WORKER_SUFFIX = PropertyManager.getProperty("", null, "ce");


    public static String jobRoutePrefix(String jobType, boolean pos) {
        return QUEUE_CATEGORY + "." + jobType + "." + (pos ? "pos" : "neg") + "." + WORKER_SUFFIX + "." + WORKER_QUEUE_TYPE;
    }

    public static String dataRoutingKeyPrefix(String data, boolean pos) {
        return null; //todo
    }

    public static String modelRoutingKeyPrefix(String model, boolean pos) {
        return null; //todo
    }
}
