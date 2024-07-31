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

package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.ms.lcms.*;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import org.apache.commons.lang3.Range;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.unijena.bioinf.ChemistryBase.utils.RangeUtils.span;

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
//        System.out.println(background);

        {
            final TIntArrayList scanids = new TIntArrayList();
            final TLongArrayList rets = new TLongArrayList();
            final TFloatArrayList levels = new TFloatArrayList();
            for (Scan scan : sample.run.getScans(background.getMinimum(), background.getMaximum()).values()) {
                if (!scan.isMsMs()) {
                    scanids.add(scan.getIndex());
                    rets.add(scan.getRetentionTime());
                    levels.add((float) sample.ms1NoiseModel.getNoiseLevel(scan.getIndex(), mainIon.getMass()));
                }
            }
            scanIds = scanids.toArray();
            retentionTime = rets.toArray();
            noiseLevels = levels.toArray();
        }

        final ArrayList<IonTrace> adducts = new ArrayList<>(), insources = new ArrayList<>();
        // create Ion traces for each adduct and in-source fragment
        for (CorrelatedIon ion : mainIon.getAdducts()) {
            adducts.add(new IonTrace(isotopeTraces(ion.ion), correlationScoring(ion)));
        }
        for (CorrelatedIon ion : mainIon.getInSourceFragments()) {
            insources.add(new IonTrace(isotopeTraces(ion.ion), correlationScoring(ion)));
        }

        this.traceSet = new CoelutingTraceSet(sample.run.getIdentifier(),
                sample.run.getReference(),
                new CompoundTrace(isotopeTraces(mainIon), correlationScoring(mainIon), adducts.toArray(IonTrace[]::new),
                        insources.toArray(IonTrace[]::new)), retentionTime, scanIds, noiseLevels,
                Arrays.stream(mainIon.getMergedScans()).mapToInt(Scan::getIndex).toArray(), Arrays.stream(mainIon.getMergedScans()).mapToLong(Scan::getRetentionTime).toArray(),

                // ignore this for SIRIUS release 4.5
                // it is not necessary and we might change the internal
                // data format of reports in later versions
                mainIon.getAdditionalInfos().toArray(CompoundReport[]::new)
                //new CompoundReport[0]
        );

    }

    private double[] correlationScoring(CorrelatedIon ion) {
        double[] scoring = new double[ion.ion.getIsotopes().size()+1];
        scoring[0] = ion.correlation.score;
        for (int i=1; i <= ion.ion.getIsotopes().size(); ++i) {
            scoring[i] = ion.ion.getIsotopes().get(i-1).score;
        }
        return scoring;
    }

    private double[] correlationScoring(FragmentedIon mainIon) {
        double[] scoring = new double[mainIon.getIsotopes().size()+1];
        scoring[0] = 1d;
        for (int i=1; i <= mainIon.getIsotopes().size(); ++i) {
            scoring[i] = mainIon.getIsotopes().get(i-1).score;
        }
        return scoring;
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
        final int indexOffset = Arrays.binarySearch(this.scanIds, segment.getStartScanNumber());
        if (indexOffset < 0) {
            throw new RuntimeException("ScanPoint " + segment.getPeak().getScanNumberAt(0) + " is not contained in trace " + Arrays.toString(this.scanIds));
        }

        final int lastIndex = Arrays.binarySearch(this.scanIds, segment.getEndScanNumber());
        if (lastIndex < 0) {
            throw new RuntimeException("ScanPoint " + segment.getPeak().getScanNumberAt(segment.getEndIndex()) + " is not contained in trace " + Arrays.toString(this.scanIds));
        }

        ChromatographicPeak background = segment.getPeak();
        int backgroundLeft = indexOffset;
        int backgroundRight = lastIndex;
        {
            final int left = background.getLeftEdge().getScanNumber();
            final int right = background.getRightEdge().getScanNumber();
            for (; backgroundLeft>=0 && this.scanIds[backgroundLeft] >= left; --backgroundLeft) {

            }
            if (backgroundLeft< indexOffset) ++backgroundLeft;
            for (; backgroundRight<scanIds.length && this.scanIds[backgroundRight] <= right; ++backgroundRight) {

            }
            if (backgroundRight>lastIndex) --backgroundRight;
            if (scanIds[backgroundRight] > segment.getPeak().getRightEdge().getScanNumber()) {
                System.err.println("WTF is going on?");
            }
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
        int width = mainIon.getSegment().getEndIndex() - mainIon.getSegment().getStartIndex();
        int extension = Math.max(width / 2, 5);
        return span(Range.of(
                mainIon.getPeak().getScanNumberAt(Math.max(mainIon.getSegment().getStartIndex() - extension, 0)),
                mainIon.getPeak().getScanNumberAt(Math.min(mainIon.getSegment().getEndIndex() + extension, mainIon.getPeak().numberOfScans() - 1))
        ), range);
    }


    private Range<Integer> getCommonRange(ArrayList<ChromatographicPeak.Segment> segments) {
        int mindex=Integer.MAX_VALUE,maxdex=Integer.MIN_VALUE;
        for (ChromatographicPeak.Segment s : segments) {
            mindex = Math.min(mindex,s.getStartScanNumber());
            maxdex = Math.max(maxdex,s.getEndScanNumber());
        }
        return Range.of(mindex, maxdex);
    }

    public CoelutingTraceSet asLCMSSubtrace() {
        return traceSet;
    }

}
