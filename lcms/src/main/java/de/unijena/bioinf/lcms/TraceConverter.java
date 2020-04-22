package de.unijena.bioinf.lcms;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CompoundTrace;
import de.unijena.bioinf.ChemistryBase.ms.lcms.IonTrace;
import de.unijena.bioinf.ChemistryBase.ms.lcms.Trace;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * converts traces in the lcms module to the trace format in ChemistryBase API
 */
class TraceConverter {

    private final ProcessedSample sample;
    private final FragmentedIon mainIon;
    private CoelutingTraceSet traceSet;

    private int[] scanIds;
    private long[] retentionTime;
    private float[] noiseLevels;

    public TraceConverter(ProcessedSample sample, FragmentedIon mainIon) {
        this.sample = sample;
        this.mainIon = mainIon;
        convert();
    }

    private void convert() {
        final ArrayList<IonGroup> allIons = new ArrayList<>();
        final ArrayList<ChromatographicPeak.Segment> segments = new ArrayList<>();
        segments.add(mainIon.getSegment());
        allIons.add(mainIon);
        for (CorrelatedIon ion : mainIon.getAdducts()) {
            allIons.add(ion.ion);
            segments.add(ion.ion.getSegment());
        }
        for (CorrelatedIon ion : mainIon.getInSourceFragments()) {
            allIons.add(ion.ion);
            segments.add(ion.ion.getSegment());
        }
        for (IonGroup ion : allIons) {
            for (CorrelationGroup g : ion.getIsotopes()) {
                segments.add(g.getRightSegment());
            }
        }
        // determine the background and trace
        Range<Integer> range = getCommonRange(segments);
        Range<Integer> background = extendRangeForBackground(mainIon, range);

        {
            final TIntArrayList scanids = new TIntArrayList();
            final TLongArrayList rets = new TLongArrayList();
            final TFloatArrayList levels = new TFloatArrayList();
            for (Scan scan : sample.run.getScans(background.lowerEndpoint(), background.upperEndpoint()).values()) {
                if (!scan.isMsMs()) {
                    scanids.add(scan.getIndex());
                    rets.add(scan.getRetentionTime());
                    levels.add((float)sample.ms1NoiseModel.getNoiseLevel(scan.getIndex(),mainIon.getMass()));
                }
            }
            scanIds = scanids.toArray();
            retentionTime = rets.toArray();
            noiseLevels = levels.toArray();
        }



        final ArrayList<IonTrace> adducts = new ArrayList<>(), insources = new ArrayList<>();
        // create Ion traces for each adduct and in-source fragment
        for (CorrelatedIon ion : mainIon.getAdducts()) {
            adducts.add(new IonTrace(isotopeTraces(ion.ion)));
        }
        for (CorrelatedIon ion : mainIon.getInSourceFragments()) {
            insources.add(new IonTrace((isotopeTraces(ion.ion))));
        }

        this.traceSet = new CoelutingTraceSet(sample.run.getIdentifier(),
                sample.run.getReference(),
                new CompoundTrace(isotopeTraces(mainIon), adducts.toArray(IonTrace[]::new),
                        insources.toArray(IonTrace[]::new)), retentionTime, scanIds, noiseLevels
        );

    }

    private Trace[] isotopeTraces(IonGroup ion) {
        final List<Trace> isotopes = new ArrayList<>();
        isotopes.add(toTrace(ion.getSegment()));
        for (CorrelationGroup i : ion.getIsotopes()) {
            isotopes.add(toTrace(i.getRightSegment()));
        }
        return isotopes.toArray(Trace[]::new);
    }

    private Trace toTrace(ChromatographicPeak.Segment segment) {
        final int indexOffset = Arrays.binarySearch(this.scanIds, segment.getStartIndex());
        if (indexOffset < 0) {
            throw new RuntimeException("ScanPoint " + segment.getPeak().getScanNumberAt(0) + " is not contained in trace " + Arrays.toString(this.scanIds));
        }

        final int lastIndex = Arrays.binarySearch(this.scanIds, segment.getEndIndex());
        if (indexOffset < 0) {
            throw new RuntimeException("ScanPoint " + segment.getPeak().getScanNumberAt(segment.getEndIndex()) + " is not contained in trace " + Arrays.toString(this.scanIds));
        }

        ChromatographicPeak background = segment.getPeak();
        int backgroundLeft = indexOffset;
        int backgroundRight = lastIndex;
        {
            final int left = background.getLeftEdge().getScanNumber();
            final int right = background.getLeftEdge().getScanNumber();
            for (; backgroundLeft>=0 && this.scanIds[backgroundLeft] >= left; --backgroundLeft) {

            }
            ++backgroundLeft;
            for (; backgroundRight<scanIds.length && this.scanIds[backgroundRight] <= right; ++backgroundRight) {

            }
            --backgroundRight;
        }
        int length = lastIndex-indexOffset+1;
        int completeLength = backgroundRight-backgroundLeft+1;
        final double[] masses = new double[completeLength];
        final float[] intensity = new float[completeLength];
        boolean needSmoothing = false;
        for (int k=backgroundLeft; k <= backgroundRight; ++k) {
            ScanPoint p = segment.getPeak().getScanPointForScanId(scanIds[k]);
            if (p==null) {
                LoggerFactory.getLogger(TraceConverter.class).warn("Detected a gap within a mass trace. No scan id " + scanIds[k] + " in trace " + segment.getPeak().scanNumbers());

                // add artificial peak
                p = new ScanPoint(scanIds[k], retentionTime[k],0d, 0d);
                needSmoothing = true;
            }

            masses[k-backgroundLeft] = p.getMass();
            intensity[k-backgroundLeft] = (float)p.getIntensity();
        }

        if (needSmoothing) {
            smooth(segment, masses, intensity);
        }

        return new Trace(backgroundLeft, indexOffset-backgroundLeft, length,  masses, intensity);
    }

    /*
    this is hopefully never called and just for backup
     */
    private void smooth(ChromatographicPeak.Segment segment, double[] masses, float[] intensity) {
        if (intensity[0]==0) {
            masses[0] = segment.getApexMass();
            LoggerFactory.getLogger(TraceConverter.class).warn("Gap is at the beginning. That does not make any sense!");
        }
        if (intensity[intensity.length-1]==0) {
            masses[intensity.length-1] = segment.getApexMass();
            LoggerFactory.getLogger(TraceConverter.class).warn("Gap is at the end. That does not make any sense!");
        }
        for (int k=1; k < masses.length-1; ++k) {
            if (intensity[k]==0) {
                int j=k+1;
                for (; j < masses.length; ++j) {
                    if (intensity[j]>0) break;
                }
                masses[k] = (masses[k-1]+masses[j])/2d;
            }
        }
    }

    private Range<Integer> extendRangeForBackground(FragmentedIon mainIon, Range<Integer> range) {
        int width = mainIon.getSegment().getEndIndex()-mainIon.getSegment().getStartIndex();
        int extension = Math.max(width/2, 5);
        return Range.closed(
            mainIon.getPeak().getScanNumberAt(Math.max(mainIon.getSegment().getStartIndex()-extension, 0)),
                mainIon.getPeak().getScanNumberAt(Math.min(mainIon.getSegment().getEndIndex()+extension, mainIon.getPeak().numberOfScans()-1))
        );
    }

    private Range<Integer> getCommonRange(ArrayList<ChromatographicPeak.Segment> segments) {
        int mindex=Integer.MAX_VALUE,maxdex=Integer.MIN_VALUE;
        for (ChromatographicPeak.Segment s : segments) {
            mindex = Math.min(mindex,s.getStartScanNumber());
            maxdex = Math.max(maxdex,s.getEndScanNumber());
        }
        return Range.closed(mindex,maxdex);
    }

    public CoelutingTraceSet asLCMSSubtrace() {
        return traceSet;
    }

}
