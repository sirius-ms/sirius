/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
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

package de.unijena.bioinf.ms.rest.model.info;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.sql.Timestamp;

public class VersionsInfo {

    /**
     * this version number increments if custom databases change and are not longer valid. This is for example the case when the fingerprint computation changes.
     */
    public String databaseDate;
    public final DefaultArtifactVersion siriusGuiVersion;
    @Setter
    @Getter
    private DefaultArtifactVersion latestSiriusVersion = null;
    @Setter
    @Getter
    private String latestSiriusLink = null;
    //Expiry Dates
    public final boolean expired;
    public final Timestamp acceptJobs;
    public final Timestamp finishJobs;

    public VersionsInfo(String siriusGuiVersion, String databaseDate, boolean expired) {
        this(siriusGuiVersion, databaseDate, expired, null, null);
    }

    public VersionsInfo(String siriusGuiVersion, String databaseDate, boolean expired, Timestamp acceptJobs, Timestamp finishJobs) {
        this.siriusGuiVersion = new DefaultArtifactVersion(siriusGuiVersion);
        this.databaseDate = databaseDate;
        this.expired = expired;
        this.acceptJobs = acceptJobs;
        this.finishJobs = finishJobs;
    }

    public boolean outdated() {
        return expired() && !finishJobs();
    }

    public boolean expired() {
        return expired;
    }

    public boolean acceptJobs() {
        if (acceptJobs == null) return false;
        return acceptJobs.getTime() >= System.currentTimeMillis();
    }

    public boolean finishJobs() {
        if (finishJobs == null) return false;
        return finishJobs.getTime() >= System.currentTimeMillis();
    }

    public boolean databaseOutdated(String s) {
        return databaseDate.compareTo(s) > 0;
    }

    @Override
    public String toString() {
        return "Sirius-gui-version: " + siriusGuiVersion +
                ", Database-date: " + databaseDate +
                ", isExpired: " + expired +
                ", acceptJobs: " + (acceptJobs != null ? acceptJobs.toString() : "NULL") +
                ", finishJobs: " + (finishJobs != null ? finishJobs.toString() : "NULL");
    }

    public static boolean areMajorEqual(DefaultArtifactVersion v1, DefaultArtifactVersion v2) {
        return v1.getMajorVersion() == v2.getMajorVersion();
    }

    public static boolean areMinorEqual(DefaultArtifactVersion v1, DefaultArtifactVersion v2) {
        return areMajorEqual(v1, v2) && v1.getMinorVersion() == v2.getMinorVersion();
    }

    public static boolean areIncrementalEqual(DefaultArtifactVersion v1, DefaultArtifactVersion v2) {
        return areMinorEqual(v1, v2) && v1.getIncrementalVersion() == v2.getIncrementalVersion();
    }
}
