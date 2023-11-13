package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.lcms.ScanPointMapping;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.*;

/**
 * A trace chain consists of multiple contigous traces with gaps in between. It acts like a trace from outside and
 * returns 0 intensity and an average m/z for the values in between
 */
public class TraceChain implements Trace {
    /**
     * connect all traces that don't overlap starting from left to right
     * @param contigousTracesByMass
     * @return
     */

    // TODO: 1.) find overlapping trace 2.) cluster, using the overlapping masses as cluster center
    // erstmal normal wie jetzt in treemap einfügen, dann über alle overlaps gehen
    // bei jedem overlap mit X nehmen wir mz(I)-(mz(x)-mz(I))/2 als threshold wobei I der Initiale Trace ist
    // jetzt alle traces aus der Map rausschmeißen die über dem Threshold liegen

    public static TraceChain connect(List<ContiguousTrace> contigousTracesByMass) {
        TreeMap<Integer, ContiguousTrace> traceByIndex = new TreeMap<>();
        ContiguousTrace tr = contigousTracesByMass.get(0);
        traceByIndex.put(tr.startId(), tr);
        double minMass = Double.POSITIVE_INFINITY, maxMass = Double.NEGATIVE_INFINITY;
        for (int k=1; k < contigousTracesByMass.size(); ++k) {
            ContiguousTrace t = contigousTracesByMass.get(k);
            Map.Entry<Integer, ContiguousTrace> o = traceByIndex.floorEntry(t.startId());
            Map.Entry<Integer, ContiguousTrace> o2 = traceByIndex.floorEntry(t.endId());
            if ((o==null || o.getValue().endId() <= t.startId()) && (o2==null || o2.getValue().endId() <= t.startId())) {
                traceByIndex.put(t.startId(), t);
                minMass = Math.min(t.averagedMz(), minMass);
                maxMass = Math.max(t.averagedMz(), maxMass);
            }
        }
        return new TraceChain(traceByIndex.values().toArray(new ContiguousTrace[0]));
    }

    public static TraceChain extend(TraceChain chain, List<ContiguousTrace> contigousTracesByMass) {
        TreeMap<Integer, ContiguousTrace> traceByIndex = new TreeMap<>();
        for (ContiguousTrace t : chain.traces) traceByIndex.put(t.startId(), t);
        for (int k=0; k < contigousTracesByMass.size(); ++k) {
            ContiguousTrace t = contigousTracesByMass.get(k);
            Map.Entry<Integer, ContiguousTrace> o = traceByIndex.floorEntry(t.startId());
            Map.Entry<Integer, ContiguousTrace> o2 = traceByIndex.floorEntry(t.endId());
            if ((o==null || o.getValue().endId() <= t.startId()) && (o2==null || o2.getValue().endId() <= t.startId())) {
                traceByIndex.put(t.startId(), t);
            }
        }
        return new TraceChain(traceByIndex.values().toArray(new ContiguousTrace[0]));
    }

    private final ContiguousTrace[] traces;
    private final int highestTrace;
    private final double averagedMz, minMz, maxMz;

    private final transient ScanPointMapping mapping;

    private final int uid;

    public TraceChain(ContiguousTrace[] traces) {
        this(-1, traces);
    }

    public int getUid() {
        return uid;
    }

    public TraceChain(int uid, ContiguousTrace[] traces) {
        if (traces.length==0) throw new IllegalArgumentException("Empty trace chains are not allowed.");
        this.traces = traces;
        this.uid = uid;
        Arrays.sort(traces, Comparator.comparingInt(ContiguousTrace::startId));
        for (int i=1; i < traces.length; ++i) {
            if (traces[i].endId() <= traces[i-1].endId()) {
                throw new IllegalArgumentException("Traces in a TraceChain are not allowed to overlap: " + Arrays.toString(traces) + "\nthe following traces overlaps: " + traces[i] + " and " + traces[i-1]);
            }
        }
        this.highestTrace = findHighestTrace(traces);
        double[] stats = averageMzFromTraces(traces, traces[highestTrace].apexIntensity());
        this.averagedMz = stats[0];
        this.minMz = stats[1];
        this.maxMz = stats[2];
        this.mapping = traces[0].getMapping();

    }

    @Override
    public int startId() {
        return traces[0].startId();
    }

    @Override
    public int endId() {
        return traces[traces.length-1].endId();
    }

    @Override
    public int apex() {
        return traces[highestTrace].apex();
    }

    @Override
    public double mz(int index) {
        if (traces.length>4) return traces[binsearch(0, traces.length, index)].mz(index);
        for (int k=0; k < traces.length; ++k) {
            if (traces[k].endId() >= index) return traces[k].mz(index);
        }
        throw new IndexOutOfBoundsException();
    }

    private int binsearch(int fromIndex, int toIndex,
                                     int key) {
        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = traces[mid].endId();

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid; // key found
        }
        return low;//-(low + 1);  // key not found.
    }

    @Override
    public double averagedMz() {
        return averagedMz;
    }

    @Override
    public double minMz() {
        return minMz;
    }

    @Override
    public double maxMz() {
        return maxMz;
    }

    @Override
    public float intensity(int index) {
        if (index < traces[0].startId()) return 0f;
        if (index > traces[traces.length-1].endId()) return 0f;
        final int trindex = binsearch(0, traces.length, index);
        if (traces[trindex].startId() <= index && traces[trindex].endId() >= index) return traces[trindex].intensity(index);
        else return 0f;
    }

    @Override
    public int scanId(int index) {
        return mapping.getScanIdAt(index);
    }

    @Override
    public double retentionTime(int index) {
        return mapping.getRetentionTimeAt(index);
    }


    private static double[] averageMzFromTraces(ContiguousTrace[] traces, double norm) {
        double mxcum=0d;
        double intcum=0d;
        double maxMz = Double.NEGATIVE_INFINITY;
        double minMz = Double.POSITIVE_INFINITY;
        for (ContiguousTrace t : traces) {
            double normed = (t.apexIntensity() / norm);
            mxcum += t.averagedMz() * normed;
            intcum += normed;
            maxMz = Math.max(maxMz, t.maxMz());
            minMz = Math.min(minMz, t.minMz());
        }
        return new double[]{mxcum / intcum, minMz, maxMz};
    }

    private static int findHighestTrace(ContiguousTrace[] traces) {
        int i=0;
        double mx=traces[0].apexIntensity();
        for (int j=1; j < traces.length; ++j) {
            if (traces[j].apexIntensity() > mx) {
                i=j;
                mx = traces[j].apexIntensity();
            }
        }
        return i;
    }

    public ContiguousTrace[] getTraces() {
        return traces;
    }

    public int[] getTraceIds() {
        return Arrays.stream(traces).mapToInt(ContiguousTrace::uniqueId).toArray();
    }

    public TraceChain withUid(int chainId) {
        return new TraceChain(traces, highestTrace,averagedMz,minMz,maxMz,mapping,chainId);
    }

    protected TraceChain(ContiguousTrace[] traces, int highestTrace, double averagedMz, double minMz, double maxMz, ScanPointMapping mapping, int uid) {
        this.traces = traces;
        this.highestTrace = highestTrace;
        this.averagedMz = averagedMz;
        this.minMz = minMz;
        this.maxMz = maxMz;
        this.mapping = mapping;
        this.uid = uid;
    }
}
