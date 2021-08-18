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

package de.unijena.bioinf.ms.rest.model.info;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LicenseInfo {
    private String licensee;
    private boolean countQueries;
    private int compoundLimit;
    private int countedCompounds = -1;
    private int maxRecordingTimeHoursCompoundHash;
    private int maxQueriesPerCompound;


    public String getLicensee() {
        return licensee;
    }

    public void setLicensee(String licensee) {
        this.licensee = licensee;
    }

    public boolean isCountQueries() {
        return countQueries;
    }

    public void setCountQueries(boolean countQueries) {
        this.countQueries = countQueries;
    }

    public int getMaxRecordingTimeHoursCompoundHash() {
        return maxRecordingTimeHoursCompoundHash;
    }

    public void setMaxRecordingTimeHoursCompoundHash(int maxRecordingTimeHoursCompoundHash) {
        this.maxRecordingTimeHoursCompoundHash = maxRecordingTimeHoursCompoundHash;
    }

    public int getMaxQueriesPerCompound() {
        return maxQueriesPerCompound;
    }

    public void setMaxQueriesPerCompound(int maxQueriesPerCompound) {
        this.maxQueriesPerCompound = maxQueriesPerCompound;
    }

    public int getCompoundLimit() {
        return compoundLimit;
    }

    public void setCompoundLimit(int compoundLimit) {
        this.compoundLimit = compoundLimit;
    }

    public int getCountedCompounds() {
        return countedCompounds;
    }

    public void setCountedCompounds(int countedCompounds) {
        this.countedCompounds = countedCompounds;
    }

    @JsonIgnore
    public boolean hasCompoundLimit() {
        return getCompoundLimit() > 0;
    }
}
