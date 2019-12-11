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


import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobData;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobData;

//there jobtypes correspond to the different job-table names
public enum JobTable {
    TREE_JOB(null),
    FINGERPRINT_JOB(null),
    SIRIUS_FINGERID_JOB(FingerprintJobData.class),
    SIRIUS_CANOPUS_JOB(CanopusJobData.class);


//    protected final TypeReference<? extends JobDataWrapper<?>> typeRef;
    protected final Class<?> classRef;

//    JobTable(TypeReference<? extends JobDataWrapper<?>> typeRef) {
//        this.typeRef = typeRef;
//    }
    JobTable(Class<?> classRef) {
        this.classRef = classRef;
    }

   /* @JsonIgnoreProperties(ignoreUnknown = true)
    final static class JobDataWrapper<D> {
        D data;
    }*/


}
