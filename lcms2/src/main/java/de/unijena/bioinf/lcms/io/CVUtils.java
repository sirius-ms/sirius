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

package de.unijena.bioinf.lcms.io;

import uk.ac.ebi.jmzml.model.mzml.CVParam;

import java.util.HashMap;

public class CVUtils {

    public static double getTimeInSeconds(CVParam param) {
        TimeUnit timeUnit;
        if (param.getUnitAccession()!=null) {
            timeUnit = getTimeUnit(param.getUnitAccession());
        } else if (param.getUnitName()!=null) {
            timeUnit = getTimeUnit(param.getUnitName());
        } else {
            throw new IllegalArgumentException("Unknown time unit for " + param);
        }
        String value = param.getValue();
        return Double.parseDouble(value) * timeUnit.inSeconds;
    }

    public static long getTimeInMilliseconds(CVParam param) {
        return Math.round(getTimeInSeconds(param)*1000d);
    }

    private final static HashMap<String, TimeUnit> term2time;
    static {
        term2time = new HashMap<>();
        for (TimeUnit time : TimeUnit.values()) {
            term2time.put(time.cvId, time);
            term2time.put(time.cvName, time);
            if (!time.unitName.isEmpty())
                term2time.put(time.unitName,time);
        }
    }

    public static TimeUnit getTimeUnit(String term) {
        TimeUnit time = term2time.get(term);
        if (time==null)
            throw new IllegalArgumentException("Term '" + term + "' is not a valid time unit.");
        return time;
    }

    public enum TimeUnit {
        MILLISECONDS("UO:0000028","millisecond","ms",1e-3),
        MICROSECONDS("UO:0000029", "microsecond","us", 1e-6),
        NANOSECONDS("UO:0000150", "nanosecond", "ns", 1e-9),
        PICOSECONDS("UO:0000030","picosecond","ps",1e-12),
        SECONDS("UO:0000010","second","s",1),
        MINUTES("UO:0000031","minute","min",60),
        HOURS("UO:0000032", "hour", "h", 3600),
        DAYS("UO:0000033", "day", "", 3600*24),
        WEEKS("UO:0000034", "week", "", 3600*24*7),
        MONTHS("UO:0000035", "month", "", 3600*24*30),
        YEARS("UO:0000036", "year", "", 3600*24*365.25d);


        public final String cvId,cvName,unitName;
        public final double inSeconds;

        TimeUnit(String cvId, String cvName, String unitName, double inSeconds) {
            this.cvId = cvId;
            this.cvName = cvName;
            this.inSeconds = inSeconds;
            this.unitName = unitName;
        }

        }
}
