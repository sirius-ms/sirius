package de.unijena.bioinf.lcms;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Bijective mapping between a contiguous ID and its corresponding Scan Number and Retention Time
 *
 * Note: scanIdentifiers is an super annoying artefact of the MzML format: scans have an ID that consists
 * of a string with an integer number. In basically all cases this id is scan=NUM, so storing hundred thousands
 * of strings that all are basically numbers is a vaste of memory. We store all IDs that follow the convention
 * as NULL and instead store the value as integer number. We only store IDs that have a different prefix.
 */
public class ScanPointMapping {

    protected Int2IntMap scan2id;

    protected double[] retentionTimes;
    protected int[] scanIndizes; // TODO: maybe we remove that and use scanIds instead?
    protected int[] scanIds;
    @Nullable protected String[] scanIdentifiers;

    public ScanPointMapping(double[] retentionTimes, int[] scanIds, String[] scanIdentifiers, Int2IntMap map) {
        this.retentionTimes = retentionTimes;
        this.scanIndizes = scanIds;
        this.scan2id = map;
        this.scanIdentifiers = scanIdentifiers;
    }

    public ScanPointMapping(double[] retentionTimes, int[] scanIds, String[] scanIdentifiers) {
        this(retentionTimes, scanIds, scanIdentifiers, new Int2IntOpenHashMap(scanIds.length));
        for (int k=0; k < scanIds.length; ++k) {
            scan2id.put(scanIds[k], k);
        }
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
        return scanIndizes[index];
    }

    /**
     * @return retention time in seconds as floating point number at the given index
     */
    public double getRetentionTimeAt(int index) {
        return retentionTimes[index];
    }

    public int[] getScanIdArray() {
        return scanIndizes;
    }

    @Nullable public String[] getScanIdentifiersArray() {
        return scanIdentifiers;
    }

    public double[] getRetentionTimeArray() {
        return retentionTimes;
    }


}
