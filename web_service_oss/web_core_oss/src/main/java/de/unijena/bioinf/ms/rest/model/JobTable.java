
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.rest.model;


import de.unijena.bioinf.ms.rest.model.canopus.CanopusJob;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobInput;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJob;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobInput;
import de.unijena.bioinf.ms.rest.model.covtree.CovtreeJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobInput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJob;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJob;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobInput;
import de.unijena.bioinf.ms.rest.model.msnovelist.MsNovelistJobOutput;

/**
 * Defines available JobTable. A JobTable contains always the fields present {@link JobBase}
 * and the fields defined by the {@link JobTable#jobOutputType}
 */
public enum JobTable {

    JOBS_FINGERID(FingerprintJob.class, FingerprintJobInput.class, FingerprintJobOutput.class),
    JOBS_CANOPUS(CanopusJob.class, CanopusJobInput.class, CanopusJobOutput.class),
    JOBS_COVTREE(CovtreeJob.class, CovtreeJobInput.class, CovtreeJobOutput.class),
    JOBS_MSNOVELIST(MsNovelistJob.class, MsNovelistJobInput.class, MsNovelistJobOutput.class);

    public static final String JOBS_GENERIC = "GENERIC";

    public final Class<? extends Job<?>> jobType;
    public final Class<?> jobIntputType;
    public final Class<?> jobOutputType;

    JobTable(Class<? extends Job<?>> jobType, Class<?> jobIntputType, Class<?> jobOutputType) {
        this.jobType = jobType;
        this.jobIntputType = jobIntputType;
        this.jobOutputType = jobOutputType;
    }
}