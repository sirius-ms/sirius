package de.unijena.bioinf.io.lcms;

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
            if (time.unitName.length()>0)
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
        PICOSECONDS("UO:0000030","picosecond","ps",1e-12),
        MINUTES("UO:0000031","minute","min",60),
        HOURS("UO:0000032", "hour", "h", 3600),
        DAYS("UO:0000033", "day", "", 3600*24),
        WEEKS("UO:0000034", "week", "", 3600*24*7),
        MONTHS("UO:0000035", "month", "", 3600*24*30),
        YEARS("UO:0000036", "year", "", 3600*24*365.25d);


        public final String cvId,cvName,unitName;
        public final double inSeconds;

        private TimeUnit(String cvId, String cvName, String unitName, double inSeconds) {
            this.cvId = cvId;
            this.cvName = cvName;
            this.inSeconds = inSeconds;
            this.unitName = unitName;
        }

        }
}
