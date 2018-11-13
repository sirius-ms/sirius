package de.unijena.bioinf.ChemistryBase.ms;

import java.util.Objects;

/**
 * 2D Peak which includes retention time, mz and intensity
 *
 * compare method first sorts by mz, then rt, to be consistent with {@link Peak}
 */
public class MzRTPeak extends Peak {
    private final double rt;

    public MzRTPeak(double rt, double mass, double intensity) {
        super(mass, intensity);
        this.rt = rt;
    }

    public double getRetentionTime(){
        return rt;
    }

    @Override
    public int compareTo(Peak o) {
        if (o instanceof MzRTPeak){
            int c = Double.compare(mass, o.mass);
            if (c!=0) return c;
            return Double.compare(rt, ((MzRTPeak)o).rt);
        } else {
            return super.compareTo(o);
        }

    }

    @Override
    public MzRTPeak clone() {
        return new MzRTPeak(rt, getMass(), getIntensity());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof MzRTPeak) {
            MzRTPeak p = (MzRTPeak) obj;
            return Math.abs(mass - p.mass) < DELTA
                    && Math.abs(intensity - p.intensity) < DELTA
                    && Math.abs(rt - p.rt) < DELTA;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rt);
    }

    @Override
    public String toString() {
        return "["+rt+","+mass+","+intensity+"]";
    }


}
