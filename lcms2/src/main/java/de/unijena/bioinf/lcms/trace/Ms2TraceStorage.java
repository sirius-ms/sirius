package de.unijena.bioinf.lcms.trace;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.ScanPointMapping;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Storage for fragment chromatograms of a data independent acquisition.
 * <p>
 * Fragment traces are grouped into channels. A channel is a set of MS2 scans that all isolate the same m/z ranges
 * at the same collision energy, and it has a retention time axis of its own - see
 * {@code de.unijena.bioinf.lcms.deconvolution.DiaChannel}. Traces of one channel are indexed against that axis and
 * never against the MS1 axis of the sample, since a channel is sampled much more coarsely than the MS1 scans.
 * <p>
 * Every channel gets its own rectangle map, so a lookup can only ever return a trace of the channel it was asked
 * about; two fragments of the same m/z that co-elute in two different isolation windows cannot be confused. The
 * traces themselves share one map, and their uids are unique across the whole sample.
 * <p>
 * Deliberately separate from {@link TraceStorage}: that one carries the MS2-to-trace assignment of a DDA
 * measurement, which has no meaning here, and it indexes everything against a single axis, which is precisely what
 * we must not do for DIA.
 */
public class Ms2TraceStorage {

    /** two traces are the same if their m/z at a given scan point differ by no more than this */
    private static final double MZ_IDENTITY = 1e-8;

    private final MVStore store;

    private final MVMap<Integer, ContiguousTrace> traceMap;

    private final AtomicInteger uids;

    /** rectangle maps are opened lazily, one per channel */
    private final Int2ObjectMap<TraceRectangleMap> rectangleMaps = new Int2ObjectOpenHashMap<>();

    private final Int2IntOpenHashMap tracesPerChannel = new Int2IntOpenHashMap();

    Ms2TraceStorage(MVStore store) {
        this.store = store;
        this.traceMap = store.openMap("ms2.traces",
                new MVMap.Builder<Integer, ContiguousTrace>().valueType(new ContigousTraceDatatype()));
        this.uids = new AtomicInteger(0);
    }

    /**
     * @param channelId id of the DIA channel
     * @param axis      retention time axis of that channel; every trace handed out for this channel is bound to it
     * @return a view on the traces of one channel
     */
    public ChannelTraces forChannel(int channelId, ScanPointMapping axis) {
        return new ChannelTraces(channelId, axis, rectangleMapFor(channelId));
    }

    private TraceRectangleMap rectangleMapFor(int channelId) {
        synchronized (rectangleMaps) {
            TraceRectangleMap map = rectangleMaps.get(channelId);
            if (map == null) {
                map = new TraceRectangleMapByRVMap(store, "ms2.rect." + channelId);
                rectangleMaps.put(channelId, map);
            }
            return map;
        }
    }

    /**
     * @return total number of fragment traces of all channels
     */
    public int numberOfTraces() {
        return traceMap.size();
    }

    /**
     * Retention times are doubles, but a {@link Rect} and the spatial index behind it store them as floats.
     * Narrowing rounds to the nearest float and can therefore shrink an interval by up to half a float step, which
     * at a retention time of an hour is 1.2e-4 s - more than twice {@link Rect#FLOATING_POINT_TOLERANCE}. Any
     * comparison that puts a float bound of a rectangle against a double retention time is therefore unreliable
     * towards the end of a long run, which is why {@code find} does not make one: it uses the rectangle purely as a
     * spatial pre-filter and decides containment on scan point indices.
     * <p>
     * Widening the bounds outwards by one float step on top of that guarantees that the stored rectangle really
     * covers the interval the trace spans, whatever a caller derives its query coordinates from. Note that this is
     * not needed for the queries in {@code find} alone: those narrow the same double with the same conversion, so
     * the two floats compare equal by construction. It costs a fraction of a millisecond of rectangle area and
     * removes the need to think about it again.
     */
    private static Rect widenToFloat(Rect rect) {
        rect.minMz = Math.nextDown(rect.minMz);
        rect.maxMz = Math.nextUp(rect.maxMz);
        rect.minRt = Math.nextDown(rect.minRt);
        rect.maxRt = Math.nextUp(rect.maxRt);
        return rect;
    }

    /**
     * A trace whose intensities are all zero has no m/z statistics: the threshold
     * {@link ContiguousTrace} weighs its masses against is a tenth of the apex intensity, so no peak passes it, and
     * the average m/z comes out as NaN with an inverted infinite m/z range. Such a rectangle would poison the
     * spatial index - comparisons against NaN are always false, so it would neither be found nor keep the index
     * consistent. Retention times cannot do this, they are bounded by the length of the run.
     */
    private static void requireFiniteBounds(ContiguousTrace trace) {
        if (!Double.isFinite(trace.averagedMz()) || !Double.isFinite(trace.minMz()) || !Double.isFinite(trace.maxMz())
                || !Double.isFinite(trace.retentionTime(trace.startId())) || !Double.isFinite(trace.retentionTime(trace.endId()))) {
            throw new IllegalArgumentException("Refusing to store a fragment trace without finite bounds. This " +
                    "usually means that all of its intensities are zero: " + trace);
        }
    }

    /**
     * The traces of a single DIA channel, bound to that channel's retention time axis.
     */
    public class ChannelTraces {

        @Getter
        private final int channelId;

        /** the retention time axis of this channel; NOT the MS1 axis of the sample */
        @Getter
        private final ScanPointMapping axis;

        private final TraceRectangleMap rectangles;

        private ChannelTraces(int channelId, ScanPointMapping axis, TraceRectangleMap rectangles) {
            this.channelId = channelId;
            this.axis = axis;
            this.rectangles = rectangles;
        }

        /**
         * @param trace a trace over the scan points of this channel
         * @return the stored trace, with a uid assigned
         */
        public ContiguousTrace add(ContiguousTrace trace) {
            requireFiniteBounds(trace);
            final ContiguousTrace stored = trace.withUID(uids.getAndIncrement());
            traceMap.put(stored.getUid(), stored);
            // the rectangle is kept in retention time, so that a lookup never has to translate between the
            // channel's scan points and those of any other axis
            rectangles.addRect(widenToFloat(stored.rectWithRts()));
            synchronized (tracesPerChannel) {
                tracesPerChannel.addTo(channelId, 1);
            }
            return stored.withMapping(axis);
        }

        /**
         * @return the trace with the given uid, bound to this channel's axis, or null
         */
        public ContiguousTrace get(int uid) {
            final ContiguousTrace trace = traceMap.get(uid);
            return trace == null ? null : trace.withMapping(axis);
        }

        /**
         * Looks for a trace of this channel that already covers the given peak, so that the same peak is not
         * picked into two different traces.
         *
         * @param channelIndex index into this channel's axis, i.e. the scan point the peak was seen at
         * @param mz           m/z of the peak
         */
        /**
         * Whether this peak already belongs to a trace of this channel, and if so to which.
         * <p>
         * Only the presence of an answer is meaningful. About one lookup in a hundred has more than one trace that
         * covers the peak, and which of them comes back depends on the order the rectangle index happens to return
         * them in, so a caller that treats the identity as significant is not reproducible. See
         * {@link #all()} for the complete set.
         */
        public Optional<ContiguousTrace> find(int channelIndex, double mz, Deviation allowedMassDeviation) {
            if (channelIndex < 0 || channelIndex >= axis.length()) return Optional.empty();
            final double retentionTime = axis.getRetentionTimeAt(channelIndex);
            final double delta = allowedMassDeviation.absoluteFor(mz);
            for (Rect rect : rectangles.overlappingRectangle(new Rect(mz - delta, mz + delta, retentionTime, retentionTime, mz))) {
                // Note that the rectangle is only a pre-filter: its retention time bounds are floats and therefore
                // not exact, so the authoritative test is whether the scan point lies within the trace itself,
                // which is an exact comparison of scan point indices.
                if (!allowedMassDeviation.inErrorWindow(mz, rect.avgMz)) continue;
                final ContiguousTrace trace = get(rect.id);
                if (trace != null && trace.inRange(channelIndex) && Math.abs(trace.mz(channelIndex) - mz) <= MZ_IDENTITY) {
                    return Optional.of(trace);
                }
            }
            return Optional.empty();
        }

        /**
         * Every trace of this channel, bound to its axis, in an order that depends only on the traces themselves.
         * <p>
         * The rectangle index returns them in an order of its own that follows its page layout, which is not stable
         * between two runs on the same data. Anything built from this list - a graph's node indices, the columns of a
         * factorisation - would inherit that, so the order is fixed here by mass and position instead.
         */
        public List<ContiguousTrace> all() {
            final List<ContiguousTrace> traces = new ArrayList<>(size());
            for (Rect rect : rectangles) {
                final ContiguousTrace trace = get(rect.id);
                if (trace != null) traces.add(trace);
            }
            traces.sort(java.util.Comparator.comparingDouble(ContiguousTrace::averagedMz)
                    .thenComparingInt(ContiguousTrace::startId)
                    .thenComparingInt(ContiguousTrace::endId));
            return traces;
        }

        /**
         * @return number of traces stored for this channel
         */
        public int size() {
            synchronized (tracesPerChannel) {
                return tracesPerChannel.get(channelId);
            }
        }
    }
}
