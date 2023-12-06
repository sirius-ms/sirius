package de.unijena.bioinf.lcms;

import de.unijena.bioinf.model.lcms.LCMSRun;
import de.unijena.bioinf.model.lcms.Scan;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

/**
 * Bijective mapping between a contiguous ID and its corresponding Scan Number and Retention Time
 */
public class ScanPointMapping {

    protected Int2IntOpenHashMap scan2id;

    protected double[] retentionTimes;
    protected int[] scanids;

    public ScanPointMapping(double[] retentionTimes, int[] scanids, Int2IntOpenHashMap map) {
        this.retentionTimes = retentionTimes;
        this.scanids = scanids;
        this.scan2id = map;
    }

    public ScanPointMapping(double[] retentionTimes, int[] scanids) {
        this(retentionTimes, scanids, new Int2IntOpenHashMap(scanids.length));
        for (int k=0; k < scanids.length; ++k) {
            scan2id.put(scanids[k], k);
        }
    }

    public ScanPointMapping(LCMSRun run) {
        scan2id = new Int2IntOpenHashMap(run.getScans().size());
        scan2id.defaultReturnValue(-1);
        IntArrayList scanids = new IntArrayList(run.getScans().size());
        DoubleArrayList rets = new DoubleArrayList(run.getScans().size());
        int k=0;
        for (Scan s : run.getScans()) {
            if (!s.isMsMs()) {
                scan2id.put(s.getIndex(), k++);
                rets.add(s.getRetentionTime());
                scanids.add(s.getIndex());
            }
        }
        this.retentionTimes = rets.toDoubleArray();
        this.scanids = scanids.toIntArray();
    }

    public int idForRetentionTime(double rt) {
        int i = Arrays.binarySearch(retentionTimes, rt);
        if (i < 0) i = -(i+1);
        if (i>=retentionTimes.length) return retentionTimes.length-1;
        if (i > 0) {
            if (Math.abs(retentionTimes[i]-rt) < Math.abs(retentionTimes[i-1]-rt)) {
                return i;
            } else return i-1;
        }
        return i;
    }

    /**
     * @return number of scan points in this sample
     */
    public int length() {
        return retentionTimes.length;
    }

    /**
     * @return ID of the given scan number
     * @throws IllegalArgumentException if there is no ID for the given scan number
     */
    public int getIdForScan(int scanNumber) {
        int i= scan2id.get(scanNumber);
        if (i < 0) throw new IllegalArgumentException("Unknown scan number: " + scanNumber);
        return i;
    }

    /**
     * @return scan id at the given index
     */
    public int getScanIdAt(int index) {
        return scanids[index];
    }

    /**
     * @return retention time in seconds as floating point number at the given index
     */
    public double getRetentionTimeAt(int index) {
        return retentionTimes[index];
    }


}
