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

import de.unijena.bioinf.ChemistryBase.math.MathUtils;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.lcms.info.*;
import de.unijena.bioinf.ChemistryBase.ms.utils.PeaklistSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Ms2CosineSegmenter {

    protected CosineQueryUtils cosine;

    public Ms2CosineSegmenter() {
        cosine = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(20)));
    }

    public IsolationWindow learnIsolationWindow(LCMSProccessingInstance instance, ProcessedSample sample) {
        final TDoubleArrayList widths = new TDoubleArrayList();
        for (Scan s : sample.run.getScans()) {
            if (s.getPrecursor()!=null) {
                // check how many intensive peaks we see after the precursor
                final SimpleSpectrum spec = sample.storage.getScan(s);
                int K = Spectrums.getFirstPeakGreaterOrEqualThan(spec, s.getPrecursor().getMass());
                double noiseLevel = sample.ms2NoiseModel.getSignalLevel(s.getIndex(), s.getPrecursor().getMass())/2d;
                double maxMass = 0d;
                for (int i=K; i < spec.size(); ++i) {
                    if (spec.getIntensityAt(i)>noiseLevel) {
                        final double mz = spec.getMzAt(i)-s.getPrecursor().getMass();
                        if (mz>=17.5)
                            break; // we often see H2O gains...
                        maxMass = Math.max(maxMass, mz);
                        if (maxMass>10) {
                            LoggerFactory.getLogger(getClass()).debug(")/");
//                            System.err.println(")/");
                        }
                    }
                }
                if (maxMass>0.5) {
                    widths.add(maxMass);
                }
            }
        }
        widths.sort();
        if (widths.size()>3) {
            return new IsolationWindow(0d, widths.get((int)(widths.size()*0.9)));
        } else return new IsolationWindow(0,0.5d);
    }

    protected static class Ms2Scan {
        protected double precursorIntensity;
        protected Scan ms1Scan, ms2Scan;
        protected ChromatographicPeak ms1Feature;
        protected Set<ChromatographicPeak> chimerics;
        protected double chimericPollution;

        public Ms2Scan(double precursorIntensity, Scan ms1Scan, Scan ms2Scan, ChromatographicPeak ms1Feature, Set<ChromatographicPeak> chimerics, double chimericPollution) {
            this.precursorIntensity = precursorIntensity;
            this.ms1Scan = ms1Scan;
            this.ms2Scan = ms2Scan;
            this.ms1Feature = ms1Feature;
            this.chimerics = chimerics;
            this.chimericPollution = chimericPollution;
        }
    }

    /**
     * iterates over all MS/MS spectra in the run. Search for the corresponding MS1 feature. Outputs a list
     * of detected features together with their MS/MS. Sometimes we see two MS/MS of different but consecutive features
     * which have a high cosine. In this case we MIGHT merge this two features into one, if their retention time difference
     * is close enough (assuming we did a mistake in splitting the mass trace into two separate features in the beginning).
     * However, very often these are similar compounds (e.g. distinct stereoisomeres), and chemists might want to keep them
     * as separate compounds.
     */
    public List<FragmentedIon> extractMsMSAndSegmentChromatograms(LCMSProccessingInstance instance, ProcessedSample sample) {
        final ArrayList<FragmentedIon> ions = new ArrayList<>();
        // group all MSMS scans into chromatographic peaks
        final HashMap<MutableChromatographicPeak, ArrayList<Ms2Scan>> scansPerPeak = new HashMap<>();
        IsolationWindow isolationWindow = null;
        Scan lastMs1 = null;
        // go over each MS/MS scan and pick the preceeding MS scan
        for (Scan s : sample.run) {
            if (s.isMsMs()) {
                // if the precursor scan number is written in the file, use this instead of picking the closest one
                if (s.getPrecursor().getIndex()>0) {
                    lastMs1 = sample.run.getScanByNumber(s.getPrecursor().getIndex()).filter(x -> !x.isMsMs()).orElse(lastMs1);
                }
                if (lastMs1==null) {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("MS2 scan without preceeding MS1 scan is not supported yet.");
                    continue;
                }
                // now do feature detection at the expected mass of the precursor in the MS1 scan
                Optional<ChromatographicPeak> detectedFeature = sample.builder.detect(lastMs1, s.getPrecursor().getMass());
                final IsolationWindow window;
                if (detectedFeature.isEmpty()) {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).debug("No MS1 feature belonging to MS/MS " + s);
                } else {
                    final MutableChromatographicPeak F = detectedFeature.get().mutate();
                    if (s.getPrecursor().getIsolationWindow().isUndefined()) {
                        if (isolationWindow == null) {
                            LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("No isolation window defined. We have to estimate it from data.");
                            isolationWindow = learnIsolationWindow(instance, sample);
                            LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("Estimate isolation window: " + isolationWindow);
                        }
                        window = isolationWindow;
                    } else window = s.getPrecursor().getIsolationWindow();
                    // we might also see chimerics. So let us check that now
                    List<ChimericDetector.Chimeric> chromatographicPeaks = new ChimericDetector(window).searchChimerics(sample, lastMs1, s.getPrecursor(), F);
                    // calculate how much percent of the intensity which is gonna fragment is chimeric
                    double precursorIntensity = 0d;
                    int k = F.findScanNumber(lastMs1.getIndex());
                    if (k >= 0) {
                        precursorIntensity = F.getIntensityAt(k);
                    } else assert false;
                    // associate the MS2 scan with the detected feature. We might later see other MS2 scans of the same
                    // feature.
                    scansPerPeak.computeIfAbsent(F, (x) -> new ArrayList<Ms2Scan>()).add(new Ms2Scan(precursorIntensity, lastMs1, s, F, chromatographicPeaks.stream().map(x->x.peak).collect(Collectors.toSet()), chromatographicPeaks.stream().mapToDouble(x->x.estimatedIntensityThatPassesFilter).sum()));
                }
            } else {
                lastMs1 = s;
            }
        }

        TLongArrayList medianWidthOfPeaks = new TLongArrayList();
        for (ChromatographicPeak peak : scansPerPeak.keySet()) {
            for (ChromatographicPeak.Segment s : peak.getSegments()) {
                medianWidthOfPeaks.add(s.fwhm());
            }
        }
        medianWidthOfPeaks.sort();
        final long medianWidth = medianWidthOfPeaks.getQuick(medianWidthOfPeaks.size()/2);
        // now iterate over all MSMS and group them by segments:

        int numberOfScansOutside = 0, numberOfMultiple = 0, numberOfInside = 0;

        final TIntObjectHashMap<ArrayList<Scan>> perSegment = new TIntObjectHashMap<>();
        for (Map.Entry<MutableChromatographicPeak, ArrayList<Ms2Scan>> entry : scansPerPeak.entrySet()) {
            perSegment.clear();
            double bestChimericScore = 0d;
            double lowestChimeric = Double.POSITIVE_INFINITY;
            for (Ms2Scan ms2Scan : entry.getValue()) {
                bestChimericScore = Math.max(bestChimericScore, ms2Scan.precursorIntensity*Math.sqrt(ms2Scan.precursorIntensity)/Math.max(0.1,ms2Scan.chimericPollution));
                lowestChimeric = Math.min(lowestChimeric, ms2Scan.chimericPollution);
            }
            final double chimericThreshold = lowestChimeric+0.2d;
            final double scoreThreshold = bestChimericScore*0.75;
            final ArrayList<RejectedMsMs> rejectedMsMs = new ArrayList<>();
            final ArrayList<JoinedSegmentDuetoCosine> joinedSegments = new ArrayList<>();
            for (Ms2Scan ms2Scan : entry.getValue()) {
                // if this scan has a high chimeric pollution while the others do not, reject it
                if (ms2Scan.chimericPollution > chimericThreshold && (ms2Scan.precursorIntensity*Math.sqrt(ms2Scan.precursorIntensity)/Math.max(0.1,ms2Scan.chimericPollution) < scoreThreshold))
                {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).debug("Reject spectrum because of high chimeric pollution.");
                    rejectedMsMs.add(new RejectedMsMsDueToChimeric(ms2Scan.ms2Scan.getRetentionTime(), ms2Scan.ms2Scan.getIndex(), (float)ms2Scan.chimericPollution));
                    continue;
                }

                final Scan s = ms2Scan.ms2Scan;
                final Optional<ChromatographicPeak.Segment> segment = entry.getKey().getSegmentForScanId(s.getIndex());
                ChromatographicPeak.Segment ms2Segment = null;
                if (segment.isEmpty()) {
                    // it seems that the MS/MS scan is shot after the peak of the MS1. This might mean that either the
                    // instrument shot into noise, OR we shoot that many MS2 scans that we do not see the "end of the MS1"
                    // to allow the latter one, we check if we have a proper fwhm end. If not, we check if the retention
                    // time difference between the MS2 and the apex is small enough
                    for (ChromatographicPeak.Segment seg : entry.getKey().segments.descendingSet()) {
                        if (s.getIndex() > seg.getEndScanNumber()) {
                            if (seg.getFwhmEndIndex() == seg.getApexIndex()) {
                                LoggerFactory.getLogger(Ms2CosineSegmenter.class).debug("MS2 scan outside of an segment of an chromatographic peak because of BAD PEAK SHAPE");
                                ms2Segment = seg;
                            }
                            break;
                        }
                    }
                } else {
                    ms2Segment = segment.get();
                }
                if (ms2Segment!=null){
                    ++numberOfInside;
                    ArrayList<Scan> scans = perSegment.get(ms2Segment.getApexScanNumber());
                    if (scans == null) {
                        scans = new ArrayList<>();
                        perSegment.put(ms2Segment.getApexScanNumber(), scans);
                    }
                    scans.add(s);
                } else {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("MS2 scan outside of an segment of an chromatographic peak");
                    ++numberOfScansOutside;
                }
            }

            final int[] segmentIds = perSegment.keys();
            Arrays.sort(segmentIds);
            final MergedSpectrum[] spectraPerSegment = new MergedSpectrum[segmentIds.length];
            final CosineQuery[][] rejectedMsMsLowCosine = new CosineQuery[segmentIds.length][];
            int k=-1;
            for (int segmentId : segmentIds) {
                ++k;
                CosineQuery[] cos = perSegment.get(segmentId).stream().map(x->prepareForCosine(sample,x)).filter(Objects::nonNull).toArray(CosineQuery[]::new);
                if (cos.length==0) continue;
                MergedSpectrum mergedPeaks = (cos.length==1) ? cos[0].originalSpectrum : mergeViaClustering(sample,cos);
                spectraPerSegment[k] = mergedPeaks;
                int rejected = cos.length - mergedPeaks.getScans().size();
                rejectedMsMsLowCosine[k] = new CosineQuery[rejected];
                {
                    final TIntHashSet have = new TIntHashSet(mergedPeaks.getScans().stream().mapToInt(Scan::getIndex).toArray());
                    int j=0;
                    for (CosineQuery q : cos) {
                        if (!have.contains(q.originalSpectrum.getScans().get(0).getIndex())) {
                            rejectedMsMsLowCosine[k][j++] = q;
                        }
                    }
                }
            }

            // merge across segment ids
            HashSet<ChromatographicPeak.Segment> SEGS = new HashSet<>();
            MergedSpectrum merged = null;
            int j=-1;
            for (int i=0; i < segmentIds.length; ++i) {
                if (spectraPerSegment[i]==null) continue;
                if (merged==null) {
                    merged = spectraPerSegment[i];
                    j = i;
                    continue;
                }
                CosineQuery queryLeft = prepareForCosine(sample, merged);

                rejectedMsMs.addAll(rejectedByCosine(merged, queryLeft, rejectedMsMsLowCosine[j]));
                CosineQuery queryRight = prepareForCosine(sample, spectraPerSegment[i]);
                SpectralSimilarity cosine = queryLeft.cosine(queryRight);
                final MutableChromatographicPeak mutableChromatographicPeak = entry.getKey().mutate();
                if (cosine.similarity >= 0.75 && cosine.shardPeaks >= 4) {
                    // compute FWHM value for segment
                    final ChromatographicPeak.Segment left = mutableChromatographicPeak.getSegmentForScanId(segmentIds[j]).get();
                    final ChromatographicPeak.Segment right = mutableChromatographicPeak.getSegmentForScanId(segmentIds[i]).get();
                    long gap = entry.getKey().getRetentionTimeAt(right.getFwhmStartIndex()) - entry.getKey().getRetentionTimeAt(left.getFwhmEndIndex());
                    if (gap > medianWidth) {
                        // do not merge
                        //System.out.println("Do not merge " + queryLeft.originalSpectrum.getPrecursor().getMass() + " " + mutableChromatographicPeak.getIntensityAt(left.getApexIndex()) + " with " + mutableChromatographicPeak.getIntensityAt(right.getApexIndex()) + " with cosine "+ cosine.similarity + " (" + cosine.shardPeaks + " peaks), due to gap above " + medianWidth);
                        final FragmentedIon ms2Ion = instance.createMs2Ion(sample, merged, entry.getKey(), left);
                        ms2Ion.getAdditionalInfos().add(new SimilarMsMsButLargeGap((left.getPeak().getRetentionTimeAt(left.getEndIndex()) + right.getPeak().getRetentionTimeAt(left.getEndIndex()))/2,
                                        (float)cosine.similarity,
                                (short)cosine.shardPeaks,
                                queryRight.originalSpectrum.getScans().stream().mapToLong(Scan::getRetentionTime).toArray(),queryRight.originalSpectrum.getScans().stream().mapToInt(Scan::getIndex).toArray(), gap
                        ));
                        ms2Ion.getAdditionalInfos().addAll(rejectedMsMs);
                        rejectedMsMs.clear();
                        ms2Ion.getAdditionalInfos().addAll(joinedSegments);
                        joinedSegments.clear();
                        ions.add(ms2Ion);
                        if (!SEGS.add(left))
                            System.out.println("=/");
                        merged = spectraPerSegment[i];
                        j=i;
                    } else {
                        joinedSegments.add(new JoinedSegmentDuetoCosine(left.getPeak().getRetentionTimeAt(left.getEndIndex()), (float)cosine.similarity,(short)cosine.shardPeaks));
                        merged = merge(merged, spectraPerSegment[i]);
                        mutableChromatographicPeak.joinAllSegmentsWithinScanIds(segmentIds[j], segmentIds[i]);
                    }
                } else if (cosine.similarity < 0.75) {
                    if (queryLeft.spectrum.size() <= 3 || queryRight.spectrum.size() <= 3) {
                        //System.out.println("Low quality MSMS");
                    } else {
                        //System.out.println("Split segments");
                        final ChromatographicPeak.Segment left = mutableChromatographicPeak.getSegmentForScanId(segmentIds[j]).get();
                        final FragmentedIon ms2Ion = instance.createMs2Ion(sample, merged, entry.getKey(), left);
                        ms2Ion.getAdditionalInfos().addAll(rejectedMsMs);
                        rejectedMsMs.clear();
                        ms2Ion.getAdditionalInfos().add(new SplittedSegmentDueToLowCosine(
                                left.getPeak().getRetentionTimeAt(left.getEndIndex()),
                                (float)cosine.similarity, (short)cosine.shardPeaks,
                                queryRight.originalSpectrum.getScans().stream().mapToLong(Scan::getRetentionTime).toArray(),queryRight.originalSpectrum.getScans().stream().mapToInt(Scan::getIndex).toArray()));
                        ms2Ion.getAdditionalInfos().addAll(joinedSegments);
                        joinedSegments.clear();
                        ions.add(ms2Ion);
                        if (!SEGS.add(left))
                            System.out.println("=/");
                        merged = spectraPerSegment[i];
                        j = i;
                    }
                }
            }
            if (merged!=null) {
                TDoubleArrayList intensityAfterPrecursor = new TDoubleArrayList();
                final ChromatographicPeak.Segment left = entry.getKey().getSegmentForScanId(segmentIds[j]).get();
                FragmentedIon ms2Ion = instance.createMs2Ion(sample, merged, entry.getKey(), left);
                final HashSet<ChromatographicPeak> chimerics = new HashSet<>();
                double chimericPollution = 0d;
                for (Scan s : merged.getScans()) {
                    intensityAfterPrecursor.add(intensityAfterPrecursor(sample.storage.getScan(s),merged.getPrecursor().getMass()));
                    for (Ms2Scan t : entry.getValue()) {
                        if (t.ms2Scan.getIndex()==s.getIndex()) {
                            chimerics.addAll(t.chimerics);
                            chimericPollution = Math.max(chimericPollution,t.chimericPollution);
                        }
                    }
                }
                ms2Ion.setChimerics(new ArrayList<>(chimerics));
                ms2Ion.setChimericPollution(chimericPollution);
                ms2Ion.getAdditionalInfos().addAll(rejectedMsMs);
                rejectedMsMs.clear();
                ms2Ion.getAdditionalInfos().addAll(joinedSegments);
                joinedSegments.clear();
                if (chimericPollution>=0.33) {
                    ms2Ion.setMs2Quality(Quality.BAD);
                }
                ms2Ion.setIntensityAfterPrecursor(Statistics.robustAverage(intensityAfterPrecursor.toArray()));
                ions.add(ms2Ion);
                if (!SEGS.add(left))
                    System.out.println("=/");
            }
            // compute cosine between segments. Check if the cosine is low -> than its probably a different compound
            //System.out.println(ions.size());
        }

       LoggerFactory.getLogger(getClass()).info("Number of scans outside = " + numberOfScansOutside + ", number of scans inside = " + numberOfInside + ", number of multiplies = " + numberOfMultiple);

        return ions;

    }

    private List<RejectedMsMsDueToLowCosine> rejectedByCosine(MergedSpectrum merged, CosineQuery queryLeft, CosineQuery[] cosineQueries) {
        final ArrayList<RejectedMsMsDueToLowCosine> rejected = new ArrayList<>();
        for (CosineQuery q : cosineQueries) {
            final SpectralSimilarity cosine = queryLeft.cosine(q);
            final Scan scan = q.originalSpectrum.getScans().get(0);
            rejected.add(new RejectedMsMsDueToLowCosine(scan.getRetentionTime(), scan.getIndex(), (float)cosine.similarity, (short)cosine.shardPeaks));
        }
        return rejected;
    }

    private CosineQuery prepareForCosine(ProcessedSample sample, Scan scan) {
        return prepareForCosine(sample, new MergedSpectrum(scan, sample.storage.getScan(scan), scan.getPrecursor(), sample.ms2NoiseModel.getNoiseLevel(scan.getIndex(),scan.getPrecursor().getMass())));
    }
    private CosineQuery prepareForCosine(ProcessedSample sample, MergedSpectrum orig) {
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(orig);
        Spectrums.cutByMassThreshold(buffer,orig.getPrecursor().getMass()-20);
        final double noiseLevel = sample.ms2NoiseModel.getNoiseLevel(orig.getScans().get(0).getIndex(), orig.getScans().get(0).getPrecursor().getMass());
        Spectrums.applyBaseline(buffer, noiseLevel);
        orig.setNoiseLevel(noiseLevel);
        if (buffer.isEmpty()) return null;
        final SimpleSpectrum spec = Spectrums.extractMostIntensivePeaks(buffer,6,100);
        return new CosineQuery(orig, spec);
    }

    protected static class CosineQuery {
        private final double selfNorm;
        private final SimpleSpectrum spectrum;
        private final MergedSpectrum originalSpectrum;

        public CosineQuery(MergedSpectrum orig, SimpleSpectrum spectrum) {
            this.spectrum = spectrum;
            this.originalSpectrum = orig;
            this.selfNorm = new IntensityWeightedSpectralAlignment(new Deviation(20)).score(spectrum,spectrum).similarity;
        }

        public SpectralSimilarity cosine(CosineQuery other) {
            SpectralSimilarity score = new IntensityWeightedSpectralAlignment(new Deviation(20)).score(spectrum, other.spectrum);
            return new SpectralSimilarity(score.similarity / Math.sqrt(selfNorm*other.selfNorm), score.shardPeaks);
        }
    }

    private double intensityAfterPrecursor(SimpleSpectrum spectrum, double precursorMz) {
        // exclude isotopic peaks
        double offset = precursorMz+5;
        int k = Spectrums.getFirstPeakGreaterOrEqualThan(spectrum, offset);
        double before=0d,after=0d;
        for (int i=0; i < k; ++i)
            before += spectrum.getIntensityAt(i);
        for (int i=k; i < spectrum.size(); ++i)
            after += spectrum.getIntensityAt(i);
        if (after==0) return 0d;
        return after / (before+after);
    }

    public MergedSpectrum mergeViaClustering(ProcessedSample sample, CosineQuery[] cosines) {

        final double[][] matrix = new double[cosines.length][cosines.length];
        for (int i=0; i < matrix.length; ++i) {
            for (int j=0; j < i; ++j) {
                matrix[i][j] = matrix[j][i]= cosines[i].cosine(cosines[j]).similarity;
            }
        }
        final int[] indizes = new int[cosines.length];
        for (int j=0; j < indizes.length; ++j) indizes[j] = j;
        int maxI=0,maxJ=0, n=matrix.length;
        outerLoop:
        while (n > 1) {
            double max = 0d;
            for (int i=0; i< n; ++i) {
                for (int j=0; j < i; ++j) {
                    if (max < matrix[indizes[i]][indizes[j]]) {
                        max = matrix[indizes[i]][indizes[j]];
                        maxI = indizes[i]; maxJ = indizes[j];
                    }
                }
            }
            if (max < 0.75) {
                break outerLoop;
            } else {
                MergedSpectrum merged = merge(cosines[maxI].originalSpectrum, cosines[maxJ].originalSpectrum);
                cosines[maxI] = prepareForCosine(sample, merged);
                --n;
                for (int k=0; k <= n; ++k) {
                    if (indizes[k]==maxJ) {
                        indizes[k] = indizes[n];
                        break;
                    }
                }
                // recalculate cosines
                for (int i=0; i < n; ++i) {
                    final int index = indizes[i];
                    if (index != maxI)
                        matrix[index][maxI] = matrix[maxI][index] = cosines[maxI].cosine(cosines[index]).similarity;
                }
            }
        }
        double bestTic = 0d; MergedSpectrum bestSpec=null;
        for (int i=0; i < n; ++i) {
            double t = cosines[indizes[i]].originalSpectrum.totalTic();
            if (t > bestTic) {
                bestTic = t;
                bestSpec = cosines[indizes[i]].originalSpectrum;
            }
        }
        return bestSpec;
    }


    public static MergedSpectrum merge(MergedSpectrum left, MergedSpectrum right) {
        // we assume a rather large deviation as signal peaks should be contained in more than one
        // measurement
        for (Scan l : left.getScans()) {
            for (Scan r : right.getScans()) {
                assert l.getIndex()!=r.getIndex();
            }
        }
        final double lcosine = left.getNorm(), rcosine = right.getNorm();
        final List<MergedPeak> orderedByMz = new ArrayList<>(left.size());
        for (MergedPeak l : left) orderedByMz.add(l);
        final List<MergedPeak> append = new ArrayList<>();
        final Deviation deviation = new Deviation(20,0.01);
        final Spectrum<MergedPeak> orderedByInt;
        double cosine = 0d;
        final double parentMass = left.getPrecursor().getMass();
        {
            final List<MergedPeak> peaks = new ArrayList<>(right.size());
            for (MergedPeak p : right) peaks.add(p);
            peaks.sort(Comparator.comparingDouble(Peak::getIntensity).reversed());
            orderedByInt = new PeaklistSpectrum<MergedPeak>(peaks);
        }
        final BitSet assigned = new BitSet(orderedByMz.size());
        for (int k = 0; k < orderedByInt.size(); ++k) {
            final double mz =  orderedByInt.getMzAt(k);
            final int mz0 = Spectrums.indexOfFirstPeakWithin(left,mz,deviation);
            if (mz0 >= 0) {
                int mz1 = mz0+1;
                while (mz1 < left.size()) {
                    if (!deviation.inErrorWindow(mz, left.getMzAt(mz1)))
                        break;
                    ++mz1;
                }
                // eigentlich ist dafür das spectral alignment da -_-
                if (mz0 <= mz1) {
                    // merge!
                    int mostIntensive = -1;
                    double bestScore = Double.NEGATIVE_INFINITY;
                    for (int i = mz0; i < mz1; ++i) {
                        if (assigned.get(i))
                            continue;
                        final double massDiff = mz - left.getMzAt(i);
                        final double score =
                               MathUtils.erfc(3 * massDiff) / (deviation.absoluteFor(mz) * Math.sqrt(2)) * left.getIntensityAt(i);
                        if (score > bestScore) {
                            bestScore = score;
                            mostIntensive = i;
                        }
                    }
                    if (mostIntensive<0) {
                        append.add(orderedByInt.getPeakAt(k));
                        continue;
                    }
                    final MergedPeak leftPeak = orderedByMz.get(mostIntensive);
                    final MergedPeak rightPeak = orderedByInt.getPeakAt(k);
                    if (leftPeak.getMass()<left.getPrecursor().getMass()-20 && rightPeak.getMass()<right.getPrecursor().getMass()-20 && leftPeak.getIntensity()>left.getNoiseLevel() && rightPeak.getIntensity()>right.getNoiseLevel()) {
                        cosine += leftPeak.getIntensity()*rightPeak.getIntensity();
                    }
                    orderedByMz.set(mostIntensive, new MergedPeak(leftPeak,rightPeak));
                    assigned.set(mostIntensive);

                }
            } else {
                // append
                append.add(orderedByInt.getPeakAt(k));
            }
        }
        orderedByMz.addAll(append);
        final ArrayList<Scan> scans = new ArrayList<>(left.getScans());
        scans.addAll(right.getScans());
        MergedSpectrum mergedPeaks = new MergedSpectrum(left.getPrecursor(), orderedByMz, scans, cosine / Math.sqrt(lcosine*rcosine));
        mergedPeaks.setNoiseLevel(Math.max(left.getNoiseLevel(), right.getNoiseLevel()));
        return mergedPeaks;
    }


}
