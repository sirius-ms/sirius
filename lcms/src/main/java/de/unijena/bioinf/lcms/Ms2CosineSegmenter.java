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
import de.unijena.bioinf.ChemistryBase.ms.*;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Ms2CosineSegmenter {

    protected CosineQueryUtils cosine;

    public Ms2CosineSegmenter() {
        cosine = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(20)));
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

                final IsolationWindow window = sample.getMs2IsolationWindowOrLearnDefault(s, instance);

                // now do feature detection at the expected mass of the precursor in the MS1 scan
                Optional<ChromatographicPeak> detectedFeature = sample.builder.detect(lastMs1, s.getPrecursor().getMass(), window);
                if (detectedFeature.isEmpty()) {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).debug("No MS1 feature belonging to MS/MS " + s);
                } else {
                    final MutableChromatographicPeak F = detectedFeature.get().mutate();
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
            for (ChromatographicPeak.Segment s : peak.getSegments().values()) {
                medianWidthOfPeaks.add(s.fwhm());
            }
        }
        medianWidthOfPeaks.sort();
        final long medianWidth = medianWidthOfPeaks.getQuick(medianWidthOfPeaks.size()/2);
        // now iterate over all MSMS and group them by segments:

        int numberOfScansOutside = 0, numberOfMultiple = 0, numberOfInside = 0;

        final TIntObjectHashMap<ArrayList<Scan>> perSegment = new TIntObjectHashMap<>(), perSegmentBad = new TIntObjectHashMap<>();

        for (Map.Entry<MutableChromatographicPeak, ArrayList<Ms2Scan>> entry : scansPerPeak.entrySet()) {
            perSegment.clear();
            perSegmentBad.clear();
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
                    for (ChromatographicPeak.Segment seg : entry.getKey().segments.descendingMap().values()) {
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

                    if (ms2Segment.isNoise()) {
                        LoggerFactory.getLogger(Ms2CosineSegmenter.class).warn("MS2 scan shot into the noise. Reject it.");
                        continue;
                    }

                    ++numberOfInside;
                    // check if it is within FHWM25%
                    TIntObjectHashMap<ArrayList<Scan>> map;
                    if (ms2Segment.getPeak().index2scanNumber(ms2Segment.calculateFWHMMinPeaks(0.25,3)).contains(s.getIndex())) {
                    map = perSegment;
                    } else {
                        map = perSegmentBad;
                    }
                    ArrayList<Scan> scans = map.get(ms2Segment.getApexScanNumber());
                    if (scans == null) {
                        scans = new ArrayList<>();
                        map.put(ms2Segment.getApexScanNumber(), scans);
                    }
                    scans.add(s);
                } else {
                    LoggerFactory.getLogger(Ms2CosineSegmenter.class).debug("MS2 scan outside of an segment of an chromatographic peak");
                    ++numberOfScansOutside;
                }
            }
            // merge maps: when we do not have high quality scans for a given scan number, use the low quality instead
            perSegmentBad.forEachEntry((scanNumber, scans) -> {
                if (!perSegment.contains(scanNumber) || perSegment.get(scanNumber).size()==0) {
                    perSegment.put(scanNumber, scans);
                }
                return true;
            });

            final int[] segmentIds = perSegment.keys();
            Arrays.sort(segmentIds);
            final MergedSpectrumWithCollisionEnergies[] spectraPerSegment = new MergedSpectrumWithCollisionEnergies[segmentIds.length];
            final CosineQuery[][] rejectedMsMsLowCosine = new CosineQuery[segmentIds.length][];
            int k=-1;
            for (int segmentId : segmentIds) {
                ++k;
                CosineQuery[] cos = perSegment.get(segmentId).stream().map(x->prepareForCosine(sample,x)).filter(Objects::nonNull).toArray(CosineQuery[]::new);
                if (cos.length==0) continue;
                Set<CollisionEnergy> energies = Arrays.stream(cos).map(CosineQuery::getCollisionEnergy).collect(Collectors.toSet());
                List<MergedSpectrum> mergedSpecs = new ArrayList<>();
                for (CollisionEnergy energy : energies) {
                    CosineQuery[] cosWithEnergy = Arrays.stream(cos).filter(x->x.getCollisionEnergy().equals(energy)).toArray(CosineQuery[]::new);
                    if (cosWithEnergy.length==1) {
                        mergedSpecs.add(cosWithEnergy[0].originalSpectrum);
                    } else if (cosWithEnergy.length>1) {
                        mergedSpecs.add( mergeViaClustering(sample,cosWithEnergy) );
                    }
                }
                spectraPerSegment[k] = new MergedSpectrumWithCollisionEnergies(mergedSpecs.toArray(MergedSpectrum[]::new));
                final List<Scan> usedScans = spectraPerSegment[k].getAllScans();
                int rejected = cos.length - usedScans.size();
                rejectedMsMsLowCosine[k] = new CosineQuery[rejected];
                {
                    final TIntHashSet have = new TIntHashSet(usedScans.stream().mapToInt(Scan::getIndex).toArray());
                    int j=0;
                    for (CosineQuery q : cos) {
                        if (!have.contains(q.originalSpectrum.getScans().get(0).getIndex())) {
                            //todo we should connect this to jobProgress
                            LoggerFactory.getLogger(getClass()).debug(have + " <-- " + q.originalSpectrum.getScans().get(0));
                            rejectedMsMsLowCosine[k][j++] = q;
                        }
                    }
                }
            }

            // merge across segment ids
            HashSet<ChromatographicPeak.Segment> SEGS = new HashSet<>();
            MergedSpectrumWithCollisionEnergies merged = null;
            int j=-1;
            for (int i=0; i < segmentIds.length; ++i) {
                if (spectraPerSegment[i] == null) continue;
                if (merged == null) {
                    merged = spectraPerSegment[i];
                    j = i;
                    continue;
                }
                CosineQuery[] queriesLeft = merged.getSpectra().stream().map(this::prepareForCosine).toArray(CosineQuery[]::new);
                rejectedMsMs.addAll(rejectedByCosine(merged,queriesLeft, rejectedMsMsLowCosine[j]));
                double maxCosine = 0;
                double mx = Double.NEGATIVE_INFINITY;
                int maxNumberOfSharedPeaks = 0;
                CosineQuery bestLeft=null, bestRight=null;
                for (int en = 0; en < merged.numberOfEnergies(); ++en) {
                    final MergedSpectrum mergedEn = merged.spectrumAt(en);
                    final CollisionEnergy energy = mergedEn.getCollisionEnergy();
                    final Optional<MergedSpectrum> otherSegMerged = spectraPerSegment[i].spectrumFor(energy);
                    if (otherSegMerged.isEmpty()) continue;
                    CosineQuery queryLeft = queriesLeft[en];
                    CosineQuery queryRight = prepareForCosine(sample, otherSegMerged.get());
                    SpectralSimilarity cosine = queryLeft.cosine(queryRight);
                    double mx2 = Math.min(cosine.sharedPeaks,6)*cosine.similarity;
                    if (mx2 > mx) {
                        maxCosine = cosine.similarity;
                        mx = mx2;
                        maxNumberOfSharedPeaks = cosine.sharedPeaks;
                        bestLeft = queryLeft;
                        bestRight = queryRight;
                    }
                }
                final MutableChromatographicPeak mutableChromatographicPeak = entry.getKey().mutate();
                if (maxCosine >= 0.75 && maxNumberOfSharedPeaks >= 6) {
                    // compute FWHM value for segment
                    final ChromatographicPeak.Segment left = mutableChromatographicPeak.getSegmentForScanId(segmentIds[j]).get();
                    final ChromatographicPeak.Segment right = mutableChromatographicPeak.getSegmentForScanId(segmentIds[i]).get();
                    long gap = entry.getKey().getRetentionTimeAt(right.getFwhmStartIndex()) - entry.getKey().getRetentionTimeAt(left.getFwhmEndIndex());
                    if (gap > medianWidth) {
                        // do not merge
                        //System.out.println("Do not merge " + queryLeft.originalSpectrum.getPrecursor().getMass() + " " + mutableChromatographicPeak.getIntensityAt(left.getApexIndex()) + " with " + mutableChromatographicPeak.getIntensityAt(right.getApexIndex()) + " with cosine "+ cosine.similarity + " (" + cosine.sharedPeaks + " peaks), due to gap above " + medianWidth);
                        final FragmentedIon ms2Ion = instance.createMs2Ion(sample, merged, entry.getKey(), left);
                        ms2Ion.getAdditionalInfos().add(new SimilarMsMsButLargeGap((left.getPeak().getRetentionTimeAt(left.getEndIndex()) + right.getPeak().getRetentionTimeAt(left.getEndIndex())) / 2,
                                (float) maxCosine,
                                (short) maxNumberOfSharedPeaks,
                                spectraPerSegment[i].getAllScans().stream().mapToLong(Scan::getRetentionTime).toArray(), spectraPerSegment[i].getAllScans().stream().mapToInt(Scan::getIndex).toArray(), gap
                        ));
                        ms2Ion.getAdditionalInfos().addAll(rejectedMsMs);
                        rejectedMsMs.clear();
                        ms2Ion.getAdditionalInfos().addAll(joinedSegments);
                        joinedSegments.clear();
                        ions.add(ms2Ion);
                        if (!SEGS.add(left))
                            System.out.println("=/");
                        merged = spectraPerSegment[i];
                        j = i;
                    } else {
                        joinedSegments.add(new JoinedSegmentDuetoCosine(left.getPeak().getRetentionTimeAt(left.getEndIndex()), (float) maxCosine, (short) maxNumberOfSharedPeaks));
                        merged = merge(merged, spectraPerSegment[i]);
                        mutableChromatographicPeak.joinAllSegmentsWithinScanIds(segmentIds[j], segmentIds[i]);
                    }
                } else if (maxCosine < 0.75 && bestLeft!=null && bestRight!=null) {
                    if (bestLeft.spectrum.size() <= 3 || bestRight.spectrum.size() <= 3) {
                        //System.out.println("Low quality MSMS");
                    } else {
                        //System.out.println("Split segments");
                        final ChromatographicPeak.Segment left = mutableChromatographicPeak.getSegmentForScanId(segmentIds[j]).get();
                        final FragmentedIon ms2Ion = instance.createMs2Ion(sample, merged, entry.getKey(), left);
                        ms2Ion.getAdditionalInfos().addAll(rejectedMsMs);
                        rejectedMsMs.clear();
                        ms2Ion.getAdditionalInfos().add(new SplittedSegmentDueToLowCosine(
                                left.getPeak().getRetentionTimeAt(left.getEndIndex()),
                                (float) maxCosine, (short) maxNumberOfSharedPeaks,
                                spectraPerSegment[i].getAllScans().stream().mapToLong(Scan::getRetentionTime).toArray(), spectraPerSegment[i].getAllScans().stream().mapToInt(Scan::getIndex).toArray()));
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
                double chimericPollutionRelative = 0d;
                for (Scan s : merged.getAllScans()) {
                    intensityAfterPrecursor.add(intensityAfterPrecursor(sample.storage.getScan(s),merged.spectrumAt(0).getPrecursor().getMass()));
                    for (Ms2Scan t : entry.getValue()) {
                        if (t.ms2Scan.getIndex()==s.getIndex()) {
                            chimerics.addAll(t.chimerics);
                            chimericPollutionRelative = Math.max(chimericPollutionRelative, t.chimericPollution / t.precursorIntensity);
                            chimericPollution = Math.max(chimericPollution,t.chimericPollution);
                        }
                    }
                }
                ms2Ion.setChimerics(new ArrayList<>(chimerics));
                ms2Ion.setChimericPollution(chimericPollutionRelative);
                ms2Ion.getAdditionalInfos().addAll(rejectedMsMs);
                rejectedMsMs.clear();
                ms2Ion.getAdditionalInfos().addAll(joinedSegments);
                joinedSegments.clear();
                if (chimericPollutionRelative>=0.50) {
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


    private List<RejectedMsMsDueToLowCosine> rejectedByCosine(MergedSpectrumWithCollisionEnergies merged, CosineQuery[] queryLeft, CosineQuery[] cosineQueries) {
        final ArrayList<RejectedMsMsDueToLowCosine> rejected = new ArrayList<>();
        for (CosineQuery q : cosineQueries) {
            for (int k=0; k < queryLeft.length; ++k) {
                if (merged.energyAt(k).equals(q.originalSpectrum.getCollisionEnergy())) {
                    final SpectralSimilarity cosine = queryLeft[k].cosine(q);
                    final Scan scan = q.originalSpectrum.getScans().get(0);
                    rejected.add(new RejectedMsMsDueToLowCosine(scan.getRetentionTime(), scan.getIndex(), (float)cosine.similarity, (short)cosine.sharedPeaks));
                }
            }
        }
        return rejected;
    }

    public CosineQuery prepareForCosine(ProcessedSample sample, Scan scan) {
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
    private CosineQuery prepareForCosine(MergedSpectrum orig) {
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum(orig);
        Spectrums.cutByMassThreshold(buffer,orig.getPrecursor().getMass()-20);
        final double noiseLevel = orig.getNoiseLevel();
        Spectrums.applyBaseline(buffer, noiseLevel);
        if (buffer.isEmpty()) return null;
        final SimpleSpectrum spec = Spectrums.extractMostIntensivePeaks(buffer,6,100);
        return new CosineQuery(orig, spec);
    }

    public static class CosineQuery {
        private final double selfNorm;
        private final SimpleSpectrum spectrum;
        private final MergedSpectrum originalSpectrum;

        public CosineQuery(MergedSpectrum orig, SimpleSpectrum spectrum) {
            this.spectrum = spectrum;
            this.originalSpectrum = orig;
            this.selfNorm = new IntensityWeightedSpectralAlignment(new Deviation(20)).score(spectrum,spectrum).similarity;
        }

        public CollisionEnergy getCollisionEnergy() {
            return originalSpectrum.getCollisionEnergy();
        }

        public MergedSpectrum getOriginalSpectrum() {
            return originalSpectrum;
        }

        public SpectralSimilarity cosine(CosineQuery other) {
            SpectralSimilarity score = new IntensityWeightedSpectralAlignment(new Deviation(20)).score(spectrum, other.spectrum);
            return new SpectralSimilarity(score.similarity / Math.sqrt(selfNorm*other.selfNorm), score.sharedPeaks);
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

    public MergedSpectrum mergeViaClustering(@Nullable ProcessedSample sample, CosineQuery[] cosines) {

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
                cosines[maxI] = sample!=null ? prepareForCosine(sample, merged) : prepareForCosine(merged);
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


    public static MergedSpectrumWithCollisionEnergies merge(MergedSpectrumWithCollisionEnergies a, MergedSpectrumWithCollisionEnergies b) {
        MergedSpectrumWithCollisionEnergies merged = new MergedSpectrumWithCollisionEnergies();
        final Set<CollisionEnergy> allEnergies = new HashSet<>();
        allEnergies.addAll(a.getCollisionEnergies());
        allEnergies.addAll(b.getCollisionEnergies());
        for (CollisionEnergy energy : allEnergies) {
            final Optional<MergedSpectrum> left = a.spectrumFor(energy);
            final Optional<MergedSpectrum> right = b.spectrumFor(energy);
            if (left.isEmpty()) {
                merged.getSpectra().add(right.get());
            } else if (right.isEmpty()) {
                merged.getSpectra().add(left.get());
            } else {
                MergedSpectrum merg = merge(left.get(),right.get());
                merged.getSpectra().add(merg);
            }
        }
        return merged;
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
