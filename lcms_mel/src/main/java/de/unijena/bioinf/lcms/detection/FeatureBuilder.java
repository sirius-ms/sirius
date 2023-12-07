/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2023 Bright Giant GmbH
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
 *  You should have received a copy of the GNU General Public License along with SIRIUS.
 *  If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.lcms.detection;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.io.IOThrowingConsumer;
import de.unijena.bioinf.ms.persistence.model.core.Feature;
import de.unijena.bioinf.ms.persistence.model.core.Run;
import de.unijena.bioinf.ms.persistence.model.core.Scan;
import de.unijena.bioinf.ms.persistence.model.core.Trace;
import de.unijena.bioinf.ms.persistence.storage.MsProjectDocumentDatabase;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDatabaseImpl;
import de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase;
import de.unijena.bioinf.storage.db.nosql.Database;
import de.unijena.bioinf.storage.db.nosql.Filter;
import de.unijena.bioinf.storage.db.nosql.nitrite.NitriteDatabase;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

public class FeatureBuilder {

    // storage
    private final Run run;
    private final MsProjectDocumentDatabase<? extends Database<?>> store;

    // feature mass candidates
    private final DoubleList candidates = new DoubleArrayList();

    // parameters
    private double gaussFilterStd = 5;
    private double peakTrimFactor = 3;
    private double intensityThresholdPercentile = 0.95;
    private int minIsotopePeaks = 1;

    private Scorer scorer = new Scorer.StudentScorer();

    private FeatureBuilder(long runId, MsProjectDocumentDatabase<? extends Database<?>> store) throws IOException, IllegalArgumentException {
        this.run = store.getStorage().findStr(Filter.build().eq("runId", runId), Run.class).findFirst().orElseThrow(() -> new IllegalArgumentException("No run with runID " + runId));
        this.store = store;
        if (store.getStorage().count(Filter.build().eq("runId", runId), Scan.class) == 0) {
            throw new IllegalArgumentException("Run with runID " + runId + " contains no MS1 scans.");
        }
    }

    private FeatureBuilder(long runId, MsProjectDocumentDatabase<? extends Database<?>> store, DoubleList candidateMasses) throws IOException, IllegalArgumentException {
        this(runId, store);
        this.candidates.addAll(candidateMasses);
    }

    public static FeatureBuilder withMSnFocus(long runId, MsProjectDocumentDatabase<? extends Database<?>> store) throws IOException, IllegalArgumentException {
        throw new UnsupportedOperationException("Not yet implemented");
//        FeatureBuilder builder = new FeatureBuilder(runId, store);
//        store.getStorage()
//                .findStr(Filter.build().eq("runId", runId), MSMSScan.class)
//                .filter(scan -> scan.getMzOfInterest() != null && Double.isFinite(scan.getMzOfInterest()))
//                .mapToDouble(MSMSScan::getMzOfInterest)
//                .distinct()
//                .forEach(builder.candidates::add);
//        // TODO for MS/MS, the candidates also have retention times!
//        if (builder.candidates.isEmpty()) {
//            throw new IllegalArgumentException("Run with runID " + runId + " contains no MSn scans.");
//        }
//        return builder;
    }

    public static FeatureBuilder withHighestIntensityFocus(long runId, MsProjectDocumentDatabase<? extends Database<?>> store) throws IOException {
        return new FeatureBuilder(runId, store);
    }

    public static FeatureBuilder withKnownCandidateMasses(long runId, MsProjectDocumentDatabase<? extends Database<?>> store, DoubleList candidateMasses) throws IOException, IllegalArgumentException {
        throw new UnsupportedOperationException("Not yet implemented");
//        // TODO for candidate masses, we should simply pick the rt with the highest intensity?
//        if (candidateMasses.isEmpty()) {
//            throw new IllegalArgumentException("No candidate masses specified.");
//        }
//        return new FeatureBuilder(runId, store, candidateMasses);
    }

    public FeatureBuilder gaussFilterStd(double gaussFilterStd) throws IllegalArgumentException {
        if (gaussFilterStd <= 0) throw new IllegalArgumentException("Gauss filter standard deviation must be > 0 (was " + String.format("%.2f", gaussFilterStd) + ").");
        this.gaussFilterStd = gaussFilterStd;
        return this;
    }

    public FeatureBuilder peakTrimFactor(double peakTrimFactor) {
        if (peakTrimFactor <= 0) throw new IllegalArgumentException("Peak trim factor must be > 0 (was " + String.format("%.2f", peakTrimFactor) + ").");
        this.peakTrimFactor = peakTrimFactor;
        return this;
    }

    public FeatureBuilder intensityThresholdPercentile(double intensityThresholdPercentile) {
        if (intensityThresholdPercentile <= 0 || intensityThresholdPercentile > 1) throw new IllegalArgumentException("Intensity threshold percentile must be in (0, 1] (was " + String.format("%.2f", intensityThresholdPercentile) + ").");
        this.intensityThresholdPercentile = intensityThresholdPercentile;
        return this;
    }

    public FeatureBuilder minIsotopePeaks(int minIsotopePeaks) {
        this.minIsotopePeaks = minIsotopePeaks;
        return this;
    }

    public FeatureBuilder scorer(Scorer scorer) {
        this.scorer = scorer;
        return this;
    }

    public void build(IOThrowingConsumer<Feature> featureConsumer) throws IOException {
        List<Trace> traces = new ArrayList<>();
        double intensityThreshold = buildTraces(traces);
        GaussFilter gaussFilter = new GaussFilter(gaussFilterStd);
        List<ChromatographicPeak> peaks = traces.stream().flatMap(trace -> {
            FilteredTrace filteredTrace = FilteredTrace.of(trace, new DoubleArrayList(gaussFilter.apply(trace.getIntensities().toDoubleArray())));
            return PersistentHomology.computePersistentHomology(filteredTrace, filteredTrace.getFilteredIntensities(), intensityThreshold, peakTrimFactor).stream();
        }).toList();

//        Map<Trace, List<ChromatographicPeak>> refMap = new HashMap<>();
//        for (ChromatographicPeak peak : peaks) {
//            Trace trace = peak.getTrace();
//            if (!refMap.containsKey(trace)) {
//                refMap.put(trace, new ArrayList<>());
//            }
//            refMap.get(trace).add(peak);
//        }
//        int k = 0;
//        for (Map.Entry<Trace, List<ChromatographicPeak>> entry : refMap.entrySet()) {
//            Trace trace = entry.getKey();
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter("/home/mel/lcms-data/polluted_citrus_traces/" + k + ".csv"))) {
//                for (int i = 0; i < trace.getRts().size(); i++) {
//                    writer.write(trace.getMzs().getDouble(i) + ", " + trace.getRts().getDouble(i) + ", " + trace.getIntensities().getDouble(i)+ "\n");
//                }
//            }
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter("/home/mel/lcms-data/polluted_citrus_peaks/" + k + ".csv"))) {
//                for (ChromatographicPeak peak : entry.getValue()) {
//                    writer.write(peak.getLeft() + ", " + peak.getBorn() + ", " + peak.getRight() + "\n");
//                }
//            }
//            k++;
//        }

        extractIsotopePatterns(peaks, intensityThreshold, isotope -> {
            long apexScanId = -1L;
            double maxInt = Double.NEGATIVE_INFINITY;
            SimpleMutableSpectrum spectrum = new SimpleMutableSpectrum();
            for (ChromatographicPeak peak : isotope) {
                double pInt = peak.getApexIntensity();
                if (pInt > maxInt) {
                    apexScanId = peak.getApexScanId();
                }
                spectrum.addPeak(peak.getApexMass(), pInt);
            }

            Feature feature = Feature.builder()
                    .featureId(-1L)
                    .apexScanId(apexScanId)
                    .isotopePattern(new SimpleSpectrum(spectrum))
                    .ionMass(isotope.get(0).getAverageMass())
                    .blank(run.getRunType() == Run.Type.BLANK)
                    .traces(isotope.stream().map(ChromatographicPeak::toTrace).toList())
                    .build();
            featureConsumer.consume(feature);
        });

    }

    private double buildTraces(List<Trace> traces) throws IOException {
        List<Scan> scans = store.getStorage()
                .findStr(Filter.build().eq("runId", run.getRunId()), Scan.class)
                .sorted(Comparator.comparingDouble(Scan::getScanTime))
                .toList();
        List<MaxPeakSpectrum> spectra = scans.stream().map(scan -> new MaxPeakSpectrum(scan.getPeaks())).toList();

        // trace apex intensities should to be significant
        double[] allIntensities = scans.stream().flatMapToDouble(scan -> IntStream.range(0, scan.getPeaks().size()).mapToDouble(i -> scan.getPeaks().getIntensityAt(i))).toArray();
        double intensityThreshold = Quickselect.quickselectInplace(allIntensities, 0, allIntensities.length, (int) Math.floor(allIntensities.length * intensityThresholdPercentile));

        while (true) {
            int maxSpectrumIndex = -1, maxPeakIndex = -1;
            double maxIntensity = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < spectra.size(); i++) {
                if (spectra.get(i).isEmpty())
                    continue;
                double intensity = spectra.get(i).getIntensityAt(spectra.get(i).getMaxIndex());
                if (intensity > maxIntensity) {
                    maxSpectrumIndex = i;
                    maxPeakIndex = spectra.get(i).getMaxIndex();
                    maxIntensity = intensity;
                }
            }

            if (maxIntensity < intensityThreshold)
                break;

            Peak maxPeak = spectra.get(maxSpectrumIndex).removePeakAt(maxPeakIndex);
            TraceCandidate traceCandidate = new TraceCandidate(maxSpectrumIndex, maxPeak);
            extendLowerRT(traceCandidate, spectra);
            extendHigherRT(traceCandidate, spectra);

            if (traceCandidate.getSpectrumIndices().size() > 3) {
                traces.add(traceCandidate.toTrace(run, scans));
            }
        }

        return intensityThreshold;
    }

    private void extendLowerRT(TraceCandidate traceCandidate, List<MaxPeakSpectrum> spectra) {
        int idx = traceCandidate.getSpectrumIndices().getInt(0);
        if (idx > 0 && !spectra.get(idx - 1).isEmpty()) {
            int closestPeakIdx = getClosestPeakIndex(spectra.get(idx - 1), traceCandidate.getMasses().getDouble(0), traceCandidate.getIntensities().getDouble(0));
            if (closestPeakIdx > -1) {
                traceCandidate.getSpectrumIndices().add(0, idx - 1);
                Peak peak = spectra.get(idx - 1).removePeakAt(closestPeakIdx);
                traceCandidate.getMasses().add(0, peak.getMass());
                traceCandidate.getIntensities().add(0, peak.getIntensity());
                extendLowerRT(traceCandidate, spectra);
            }
        }
    }

    private void extendHigherRT(TraceCandidate traceCandidate, List<MaxPeakSpectrum> spectra) {
        int idx = traceCandidate.getSpectrumIndices().getInt(traceCandidate.getSpectrumIndices().size() - 1);
        if (idx < spectra.size() - 1 && !spectra.get(idx + 1).isEmpty()) {
            int closestPeakIdx = getClosestPeakIndex(spectra.get(idx + 1), traceCandidate.getMasses().getDouble(traceCandidate.getMasses().size() - 1), traceCandidate.getIntensities().getDouble(traceCandidate.getIntensities().size() - 1));
            if (closestPeakIdx > -1) {
                traceCandidate.getSpectrumIndices().add(idx + 1);
                Peak peak = spectra.get(idx + 1).removePeakAt(closestPeakIdx);
                traceCandidate.getMasses().add(peak.getMass());
                traceCandidate.getIntensities().add(peak.getIntensity());
                extendHigherRT(traceCandidate, spectra);
            }
        }
    }

    private int getClosestPeakIndex(MaxPeakSpectrum spectrum, double mass, double intensity) {
        DoubleList candidateMasses = new DoubleArrayList();
        DoubleList candidateIntensities = new DoubleArrayList();
        int startIndex = Integer.MAX_VALUE, stopIndex = Integer.MIN_VALUE;
        for (int i = 0; i < spectrum.size(); i++) {
            double mz = spectrum.getMzAt(i);
            if (mz >= mass - 1d && mz <= mass + 1d) {
                double intens = spectrum.getIntensityAt(i);
                startIndex = Math.min(i, startIndex);
                stopIndex = Math.max(i, stopIndex);
                candidateMasses.add(mz);
                candidateIntensities.add(intens);
            }
        }
        if (startIndex < stopIndex) {
            DoubleList massScored = scorer.score(candidateMasses, mass);
            DoubleList intScored = scorer.score(candidateIntensities, intensity);

            double maxScore = Double.NEGATIVE_INFINITY;
            int maxIndex = -1;
            for (int i = 0; i < massScored.size(); i++) {
                if (massScored.getDouble(i) + intScored.getDouble(i) > maxScore) {
                    maxScore = massScored.getDouble(i) + intScored.getDouble(i);
                    maxIndex = i;
                }
            }
            return maxIndex > -1 ? startIndex + maxIndex : -1;
        } else {
            return startIndex < Integer.MAX_VALUE ? startIndex : -1;
        }
    }

    private void extractIsotopePatterns(List<ChromatographicPeak> peaks, double intensityThreshold, IOThrowingConsumer<List<ChromatographicPeak>> isotopeConsumer) throws IOException {
        PriorityQueue<ChromatographicPeak> queue = new PriorityQueue<>((a, b) -> Double.compare(b.getApexIntensity(), a.getApexIntensity()));
        queue.addAll(peaks);
        TreeMap<Double, List<ChromatographicPeak>> mzMap = new TreeMap<>();
        peaks.forEach(peak -> {
            double apexMass = peak.getApexMass();
            if (!mzMap.containsKey(apexMass)) {
                mzMap.put(apexMass, new ArrayList<>());
            }
            mzMap.get(apexMass).add(peak);
        });

        while (!queue.isEmpty()) {
            ChromatographicPeak apex = queue.peek();
            if (apex.getApexIntensity() < intensityThreshold)
                break;

            List<ChromatographicPeak> isotope = new ArrayList<>();
            isotope.add(apex);
            extendLowerMass(apex, queue, mzMap, isotope);
            extendHigherMass(apex, queue, mzMap, isotope);

            if (isotope.size() >= minIsotopePeaks) {
                isotopeConsumer.consume(isotope);
            }
        }
    }

    private void extendLowerMass(ChromatographicPeak apex, PriorityQueue<ChromatographicPeak> queue, TreeMap<Double, List<ChromatographicPeak>> mzMap, List<ChromatographicPeak> isotopePattern) {
        queue.remove(apex);
        mzMap.remove(apex.getApexMass());
        double apexMass = apex.getApexMass();
        double apexRT = apex.getApexRT();
        List<ChromatographicPeak> candidates = mzMap.subMap(apexMass - 1.5, false, apexMass - 0.5, false).values()
                .stream().flatMap(List::stream).filter(c -> {
                    double cRT = c.getApexRT();
                    return apexRT - 0.1 <= cRT && cRT <= apexRT + 0.1;
                }).toList();
        if (!candidates.isEmpty()) {
            ChromatographicPeak selection = selectPeak(apexMass, apexRT, candidates);
            if (selection != null) {
                isotopePattern.add(0, selection);
                extendLowerMass(selection, queue, mzMap, isotopePattern);
            }
        }
    }

    private void extendHigherMass(ChromatographicPeak apex, PriorityQueue<ChromatographicPeak> queue, TreeMap<Double, List<ChromatographicPeak>> mzMap, List<ChromatographicPeak> isotopePattern) {
        queue.remove(apex);
        mzMap.remove(apex.getApexMass());
        double apexMass = apex.getApexMass();
        double apexRT = apex.getApexRT();
        List<ChromatographicPeak> candidates = mzMap.subMap(apexMass + 0.5, false, apexMass + 1.5, false).values()
                .stream().flatMap(List::stream).filter(c -> {
                    double cRT = c.getApexRT();
                    return apexRT - 0.1 <= cRT && cRT <= apexRT + 0.1;
                }).toList();
        if (!candidates.isEmpty()) {
            ChromatographicPeak selection = selectPeak(apexMass, apexRT, candidates);
            if (selection != null) {
                isotopePattern.add(selection);
                extendHigherMass(selection, queue, mzMap, isotopePattern);
            }
        }
    }

    private ChromatographicPeak selectPeak(double apexMass, double apexRT, List<ChromatographicPeak> candidates) {
        ChromatographicPeak selection = null;
        if (candidates.size() > 1) {
            DoubleList candidateMasses = new DoubleArrayList();
            DoubleList candidateRTs = new DoubleArrayList();

            DoubleList massScored = scorer.score(candidateMasses, apexMass);
            DoubleList rtScored = scorer.score(candidateRTs, apexRT);

            double maxScore = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < massScored.size(); i++) {
                if (massScored.getDouble(i) + rtScored.getDouble(i) > maxScore) {
                    maxScore = massScored.getDouble(i) + rtScored.getDouble(i);
                    selection = candidates.get(i);
                }
            }

        } else {
            selection = candidates.get(0);
        }
        return selection;
    }

    public static void main(String[] args) throws IOException {
        String storeLocation = "/home/mel/store.nitrite";

        try (NitriteDatabase db = new NitriteDatabase(Path.of(storeLocation), SiriusProjectDocumentDatabase.buildMetadata())) {
            Run run = db.findAllStr(Run.class).findFirst().orElseThrow();
            SiriusProjectDatabaseImpl<?> ps = new SiriusProjectDatabaseImpl<>(db);
            FeatureBuilder.withHighestIntensityFocus(run.getRunId(), ps)
                    .gaussFilterStd(5)
                    .intensityThresholdPercentile(0.95)
                    .peakTrimFactor(3)
                    .minIsotopePeaks(1)
                    .scorer(new Scorer.StudentScorer())
                    .build(feature -> {
                List<Trace> traces = feature.getTraces();
                double apexMass = 0;
                double apexRT = 0;
                double maxInt = 0;
                for (Trace trace : traces) {
                    for (int i = 0; i < trace.getIntensities().size(); i++) {
                        if (trace.getIntensities().getDouble(i) > maxInt) {
                            apexMass = trace.getMzs().getDouble(i);
                            apexRT = trace.getRts().getDouble(i);
                            maxInt = trace.getIntensities().getDouble(i);
                        }
                    }
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter("/home/mel/lcms-data/polluted_citrus_features/" +String.format("%.2f", apexMass) + "_" + String.format("%.2f", apexRT) + "_" + traces.size() + ".csv"))) {
                    for (Trace trace : traces) {
                        for (int i = 0; i < trace.getIntensities().size(); i++) {
                            writer.write(trace.getMzs().getDouble(i) + ", " + trace.getRts().getDouble(i) + ", " + trace.getIntensities().getDouble(i) + "\n");
                        }
                    }
                }
            });

        }
    }

}
