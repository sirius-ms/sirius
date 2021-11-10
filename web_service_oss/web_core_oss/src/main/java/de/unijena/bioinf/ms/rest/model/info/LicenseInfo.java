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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.sql.Date;
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseInfo {
    private String licensee;
    private String description;
    private String groupID;
    private Date expirationDate;
    private boolean countQueries;
    private int compoundLimit;
    private int countedCompounds = -1;
    private int compoundHashRecordingTime;
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

    public int getCompoundHashRecordingTime() {
        return compoundHashRecordingTime;
    }

    public void setCompoundHashRecordingTime(int timeInHours) {
        this.compoundHashRecordingTime = timeInHours;
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

    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonIgnore
    public boolean hasExpirationTime(){
        return getExpirationDate() != null;
    }

    @JsonIgnore
    public boolean isExpired() {
        if (!hasExpirationTime())
            return false;
        return getExpirationDate().getTime() < System.currentTimeMillis();
    }

    @JsonIgnore
    public long getExpirationTime() {
        if (expirationDate == null)
            return -1;
        return expirationDate.getTime();
    }

    @JsonIgnore
    public void setExpirationTime(long expirationDate) {
        this.expirationDate = new Date(expirationDate);
    }


    @JsonIgnore
    public boolean hasCompoundLimit() {
        return getCompoundLimit() > 0;
    }

    @JsonIgnore
    public LicenseInfo copyWithCounted(int countedCompounds){
        LicenseInfo nu  = new LicenseInfo();
        nu.setCountedCompounds(countedCompounds);
        nu.setLicensee(getLicensee());
        nu.setCountQueries(isCountQueries());
        nu.setCompoundHashRecordingTime(getCompoundHashRecordingTime());
        nu.setMaxQueriesPerCompound(getMaxQueriesPerCompound());
        nu.setGroupID(getGroupID());
        nu.setExpirationDate(getExpirationDate());
        return nu;
    }
}
