/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.ms.rest.model;


import de.unijena.bioinf.ms.rest.model.canopus.CanopusJob;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.SiriusPredictionJob;

/**
 * Defines available JobTable. A JobTable contains always the fields present {@link JobBase}
 * and the fields defined by the {@link JobTable#jobOutputType}
 */
public enum JobTable {
//    TREE_JOB(null, null, null),
//    FINGERPRINT_JOB(null, null, null),
    JOBS_FINGERID(SiriusPredictionJob.class, FingerprintJobInput.class, FingerprintJobOutput.class),
    JOBS_CANOPUS(CanopusJob.class, CanopusJobInput.class, CanopusJobOutput.class);


    public final Class<? extends Job<?>> jobType;
    public final Class<?> jobIntputType;
    public final Class<?> jobOutputType;

    JobTable(Class<? extends Job<?>> jobType, Class<?> jobIntputType, Class<?> jobOutputType) {
        this.jobType = jobType;
        this.jobIntputType = jobIntputType;
        this.jobOutputType = jobOutputType;
    }
}
