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

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.exceptions.InvalidInputData;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.math.ExponentialDistribution;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.jjobs.ProgressJJob;
import de.unijena.bioinf.lcms.align.*;
import de.unijena.bioinf.lcms.ionidentity.IonNetwork;
import de.unijena.bioinf.lcms.noise.Ms2NoiseStatistics;
import de.unijena.bioinf.lcms.noise.NoiseStatistics;
import de.unijena.bioinf.lcms.peakshape.CustomPeakShape;
import de.unijena.bioinf.lcms.peakshape.CustomPeakShapeFitting;
import de.unijena.bioinf.lcms.peakshape.PeakShape;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import de.unijena.bionf.spectral_alignment.CosineQueryUtils;
import de.unijena.bionf.spectral_alignment.IntensityWeightedSpectralAlignment;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.Range;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LCMSProccessingInstance {

    protected InternalStatistics internalStatistics;

    protected HashMap<ProcessedSample, SpectrumStorage> storages;
    protected List<ProcessedSample> samples;
    protected MemoryFileStorage ms2Storage;
    protected AtomicInteger numberOfMs2Scans = new AtomicInteger();
    protected volatile boolean centroided = true;
    protected MassToFormulaDecomposer formulaDecomposer = new MassToFormulaDecomposer(
            new ChemicalAlphabet(MolecularFormula.parseOrThrow("CHNOPS").elementArray())
    );

    protected Set<PrecursorIonType> detectableIonTypes;

    public LCMSProccessingInstance() {
        this.samples = new ArrayList<>();
        try {
            this.ms2Storage = new MemoryFileStorage();
        } catch (IOException e) {
            throw new RuntimeException();
        }
        this.storages = new HashMap<ProcessedSample, SpectrumStorage>();
        this.detectableIonTypes = new HashSet<>(Arrays.asList(
                PrecursorIonType.fromString("[M+Na]+"),
                PrecursorIonType.fromString("[M+K]+"),
                PrecursorIonType.fromString("[M+H]+"),
                PrecursorIonType.fromString("[M-H2O+H]+"),
                PrecursorIonType.fromString("[M-H4O2+H]+"),
                PrecursorIonType.fromString("[M-H2O+Na]+"),
                PrecursorIonType.fromString("[M + FA + H]+"),
                PrecursorIonType.fromString("[M + ACN + H]+"),
                PrecursorIonType.fromString("[M + ACN + Na]+"),
                //PrecursorIonType.fromString("[M-H+Na2]+"),
                PrecursorIonType.fromString("[M+NH3+H]+"),
                PrecursorIonType.fromString("[M-H]-"),
                PrecursorIonType.fromString("[M+Cl]-"),
                PrecursorIonType.fromString("[M+Br]-"),
                PrecursorIonType.fromString("[M-H2O-H]-"),

                        PrecursorIonType.fromString("[M - H2O - H]-"),
                PrecursorIonType.fromString("[M + CH2O2 - H]-"),
                PrecursorIonType.fromString("[M + C2H4O2 - H]-"),
                PrecursorIonType.fromString("[M + H2O - H]-"),
                PrecursorIonType.fromString("[M - H3N - H]-"),
                PrecursorIonType.fromString("[M - CO2 - H]-"),
                PrecursorIonType.fromString("[M - CH2O3 - H]-"),
                PrecursorIonType.fromString("[M - CH3 - H]-"),
                PrecursorIonType.fromString("[M+Na-2H]-")

        ));


    }

    public Optional<InternalStatistics> getInternalStatistics() {
        return Optional.ofNullable(internalStatistics);
    }

    public void trackStatistics() {
        this.internalStatistics = new InternalStatistics();
    }

    public CoelutingTraceSet getTraceset(ProcessedSample sample, FragmentedIon ion) {
        return new TraceConverter(sample, ion).asLCMSSubtrace();
    }

    public Set<PrecursorIonType> getDetectableIonTypes() {
        return detectableIonTypes;
    }

    public void setDetectableIonTypes(Set<PrecursorIonType> detectableIonTypes) {
        this.detectableIonTypes = detectableIonTypes;
    }

    public MemoryFileStorage getMs2Storage() {
        return ms2Storage;
    }

    public FragmentedIon createMs2Ion(ProcessedSample sample, MergedSpectrumWithCollisionEnergies merged, MutableChromatographicPeak peak, ChromatographicPeak.Segment segment) {
        Scan[] scans = new Scan[merged.numberOfEnergies()];
        CollisionEnergy[] energies = new CollisionEnergy[merged.numberOfEnergies()];
        SimpleSpectrum[] toMerge = new SimpleSpectrum[merged.numberOfEnergies()];
        Quality bestQuality = Quality.UNUSABLE;
        for (int k=0; k < merged.numberOfEnergies(); ++k) {
            energies[k] = merged.energyAt(k);
            final int id = numberOfMs2Scans.incrementAndGet();
            final SimpleSpectrum spec = merged.spectrumAt(k).finishMerging();
            final Quality quality = merged.spectrumAt(k).getQuality(spec);
            if (quality.betterThan(bestQuality)) bestQuality = quality;
            final SimpleSpectrum spec2 = Spectrums.extractMostIntensivePeaks(spec, 8, 50);
            toMerge[k] = spec2;
            final Scan someScan = merged.spectrumAt(k).getScans().get(0);
            final Scan scan = new Scan(id, someScan.getPolarity(),peak.getRetentionTimeAt(segment.getApexIndex()),merged.energyAt(k),spec.size(), Spectrums.calculateTIC(spec), true, someScan.getPrecursor());
            scans[k] = scan;
            ms2Storage.add(scan, spec);
        }
        SimpleSpectrum mergedAll = Spectrums.mergeSpectra(new Deviation(10), true, false, Arrays.asList(toMerge));
        final FragmentedIon ion = new FragmentedIon(scans[0].getPolarity(), scans,energies, new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(20))).createQueryWithIntensityTransformationNoLoss(mergedAll, merged.getPrecursor().getMass(), true), bestQuality, peak, segment, merged.getAllScans().toArray(Scan[]::new));
        if (internalStatistics!=null) trackMs2Statistics(sample, merged, ion);
        return ion;
    }

    private void trackMs2Statistics(ProcessedSample sample, MergedSpectrumWithCollisionEnergies merged, FragmentedIon ion) {
        final CosineQueryUtils utils = new CosineQueryUtils(new IntensityWeightedSpectralAlignment(new Deviation(10)));
        final Ms2CosineSegmenter segmenter = new Ms2CosineSegmenter();
        for (MergedSpectrum spec : merged.getSpectra()){
            final List<Scan> scans = spec.getScans();
            final List<Ms2CosineSegmenter.CosineQuery> transformed = scans.stream().map(x->segmenter.prepareForCosine(sample, x)).collect(Collectors.toList());
            for (int i=0; i < scans.size(); ++i) {
                internalStatistics.retentionTimeWindow.add(sample.getMs2IsolationWindowOrLearnDefault(scans.get(i), this).getWindowWidth());
                for (int j = 0; j < i; ++j) {
                    final SpectralSimilarity cosine = transformed.get(i).cosine(transformed.get(j));
                    internalStatistics.msmsMergedCosines.add(cosine.similarity);
                }
            }
        }
        internalStatistics.numberOfPeaksPerMSMs.add(ion.getMsMs().size());
        internalStatistics.msmsEntropy.add(ion.getMsMs().entropy());

    }

    /**
     * has to be called after alignment
     */
    public IonNetwork detectAdductsWithGibbsSampling(Cluster alignedFeatures) {
        final IonNetwork network = new IonNetwork();
        final CorrelatedPeakDetector detector = new CorrelatedPeakDetector(detectableIonTypes);
        final JobManager jobManager = SiriusJobs.getGlobalJobManager();
        final List<BasicJJob<Object>> jobs = new ArrayList<>();
        for (AlignedFeatures features : alignedFeatures.getFeatures()) {
            features.getFeatures().forEach((key, value) -> jobs.add(jobManager.submitJob(new BasicJJob<Object>() {
                @Override
                protected Object compute() throws Exception {
                    if (!value.isAdductDetectionDone()) detector.detectCorrelatedPeaks(key, value);
                    value.setDetectedIonType(PrecursorIonType.unknown(value.getPolarity()));
                    return null;
                }
            })));
        }
        jobs.forEach(JJob::takeResult);
        jobs.clear();
        for (AlignedFeatures features : alignedFeatures.getFeatures()) {
            network.addNode(features);
        }
        network.addCorrelatedEdgesForAllNodes(this);
        network.deleteSingletons();

        // debugging statistics
        TObjectIntHashMap<Ionization> selectedIonTypeCounter = new TObjectIntHashMap<>();
        TObjectIntHashMap<PrecursorIonType> possibleIonTypeCounter = new TObjectIntHashMap<>();
        int[] ncompounds=new int[1];

        final ArrayList<AlignedFeatures> features = new ArrayList<>();
        network.gibbsSampling(this, (feature,types,prob)->{
            for (FragmentedIon ion : feature.getFeatures().values()) {
                // we only consider adducts which are at least 1/5 as likely as the most likely option
                final double threshold = Arrays.stream(prob).max().orElse(0d)/5d;
                final HashSet<PrecursorIonType> set = new HashSet<>();
                boolean unknown = false;
                final TObjectDoubleHashMap<Ionization> probForIons = new TObjectDoubleHashMap<>();
                for (PrecursorIonType t : this.detectableIonTypes) {
                    if (t.getCharge()==ion.getPolarity()) {
                        probForIons.putIfAbsent(t.getIonization(), 0d);
                    }
                }
                double unknownProb = 0d;
                for (int k=0; k < types.length; ++k) {
                    if (types[k].isIonizationUnknown()) {
                        unknownProb += prob[k];
                        if (prob[k]>=threshold)
                            unknown=true;
                    } else {
                        if (prob[k] > 0) {
                            probForIons.adjustOrPutValue(types[k].getIonization(), prob[k], prob[k]);
                        }
                        if (prob[k] >= threshold) {
                            set.add(types[k]);
                            // whenever we add an adduct, add its ionization, too
                            // because we cannot distinguish adducts from in-source
                            // fragments
                            // exception: damned, for some adducts we do not want that.
                            if (!types[k].hasMultipleIons()) {
                                set.add(types[k].withoutAdduct());
                                set.add(types[k].withoutInsource());
                            }
                        }
                    }
                }
                final double unknownProbability = unknownProb;
                // add ionizations for each ion type which has probability above
                final double ionThreshold = Arrays.stream(probForIons.values()).max().orElse(0d)/5d;
                probForIons.forEachEntry((ionKey,probability)->{
                   if (unknownProbability + probability >= ionThreshold) {
                       set.add(PrecursorIonType.getPrecursorIonType(ionKey));
                   }
                   return true;
                });
                ion.setPossibleAdductTypes(set);
                if (!unknown && set.size()==1) ion.setDetectedIonType(set.iterator().next());
                else ion.setDetectedIonType(PrecursorIonType.unknown(ion.getPolarity()));

                Set<Ionization> ionizations = new HashSet<>();
                for (PrecursorIonType type : set) {
                    possibleIonTypeCounter.adjustOrPutValue(type, 1, 1);
                    if (!type.isIonizationUnknown()) ionizations.add(type.getIonization());
                }
                if (ionizations.size()==1) {
                    selectedIonTypeCounter.adjustOrPutValue(ionizations.iterator().next(), 1, 1);
                }
                ncompounds[0]++;
            }
        });
        network.reinsertLikelyCorrelatedEdgesIntoFeatures();


        System.out.println("#### LCMS Preprocessing STATISTICS #####");
        possibleIonTypeCounter.forEachEntry((key,count)->{
            System.out.printf("%s %d times (%.2f %%)\n", key.toString(), count, count*100.0d/ncompounds[0] );
            return true;
        });
        System.out.println("--------------- SELECTED ---------------");
        int[] other = new int[1];
        selectedIonTypeCounter.forEachEntry((key,count)->{
            System.out.printf("%s %d times (%.2f %%)\n", key.toString(), count, count*100.0d/ncompounds[0] );
            other[0] += count;
            return true;
        });
        if (internalStatistics!=null) {
            final float N = ncompounds[0];
            internalStatistics.hplus = Math.max(0,selectedIonTypeCounter.get(PrecursorIonType.fromString("[M+H]+").getIonization())) / N;
            internalStatistics.potassium = Math.max(0,selectedIonTypeCounter.get(PrecursorIonType.fromString("[M+K]+").getIonization())) / N;
            internalStatistics.sodium = Math.max(0,selectedIonTypeCounter.get(PrecursorIonType.fromString("[M+Na]+").getIonization())) / N;
            internalStatistics.waterLoss = (Math.max(0,possibleIonTypeCounter.get(PrecursorIonType.fromString("[M-H2O+H]+"))) + Math.max(0,possibleIonTypeCounter.get(PrecursorIonType.fromString("[M-H4O2+H]+")))) / N;
            internalStatistics.ammonium = Math.max(0,possibleIonTypeCounter.get(PrecursorIonType.fromString("[M+NH3+H]+"))) / N;
            Set<PrecursorIonType> common = new HashSet<>(Arrays.asList(
               PrecursorIonType.getPrecursorIonType("[M+H]+"),
                    PrecursorIonType.getPrecursorIonType("[M+K]+"),
                    PrecursorIonType.getPrecursorIonType("[M+Na]+"),
                    PrecursorIonType.getPrecursorIonType("[M-H2O+H]+"),
                    PrecursorIonType.getPrecursorIonType("[M-H4O2+H]+"),
                    PrecursorIonType.getPrecursorIonType("[M+NH3+H]+"),
                    PrecursorIonType.getPrecursorIonType("[M+?]+")
            ));
            possibleIonTypeCounter.forEachEntry((key,count)->{
                if (!common.contains(key)) {
                    internalStatistics.strangeAdducts += count;
                }
                return true;
            });
            internalStatistics.strangeAdducts /= N;
        }
        System.out.printf("Multiple possibilities: %d times (%.2f %%)\n", ncompounds[0]-other[0], (ncompounds[0]-other[0]+0d)*100.0d/ncompounds[0] );
        System.out.println("########################################");
        System.out.println();

        /*try {
            network.writeToFile(this, new File("/home/kaidu/network.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        if (internalStatistics!=null) network.collectStatistics(internalStatistics);
        return network;
    }

    public boolean isCentroided() {
        return centroided;
    }

    public ProcessedSample addSample(LCMSRun run, SpectrumStorage storage) throws InvalidInputData {
        return addSample(run,storage,true);
    }

    public ProcessedSample addSample(LCMSRun run, SpectrumStorage storage, boolean enforceMs2) throws InvalidInputData {
        final NoiseStatistics noiseStatisticsMs1 = new NoiseStatistics(100, 0.2, 1000)/*, noiseStatisticsMs2 = new NoiseStatistics(10, 0.85, 60)*/;

        Ms2NoiseStatistics ms2NoiseStatistics = new Ms2NoiseStatistics();

        boolean hasMsMs = false;
        for (Scan s : run.getScans()) {
            if (!s.isCentroided()) {
                this.centroided = false;
                LoggerFactory.getLogger(LCMSProccessingInstance.class).warn("Scan " + s + " is in PROFILED mode. SIRIUS does only support centroided spectra. Ignore this scan.");
                continue;
            }
            if (s.isMsMs()) {
                hasMsMs = true;
                //noiseStatisticsMs2.add(s, storage.getScan(s));
                ms2NoiseStatistics.add(s,storage.getScan(s));
            } else {
                noiseStatisticsMs1.add(s,storage.getScan(s));
            }
        }

        if (enforceMs2 && !hasMsMs) throw new InvalidInputData("Run has no MS/MS spectra.");

        if (hasMsMs) ms2NoiseStatistics.done();

        final ProcessedSample sample = new ProcessedSample(
                run, noiseStatisticsMs1.getLocalNoiseModel(), ms2NoiseStatistics,
                new ChromatogramCache(), storage
        );
        synchronized (this) {
            this.samples.add(sample);
            this.storages.put(sample, storage);
        }
        return sample;
    }

    public Feature makeFeature(ProcessedSample sample, FragmentedIon ion, boolean gapFilled) {
        int charge = ion.getPolarity();
        if (charge == 0) {
            if (ion.getMsMsScans()!=null && ion.getMsMsScans()[0].getPolarity()!=null) {
                charge = ion.getMsMsScans()[0].getPolarity().charge;
            } else {
                LoggerFactory.getLogger(LCMSProccessingInstance.class).warn("Unknown polarity. Set polarity to POSITIVE");
                charge = 1;
            }
        }


        final double ionMass;
        {
            int a = ion.getSegment().getFwhmStartIndex(), b = ion.getSegment().getFwhmEndIndex();
            double mz = 0d, intens = 0d;
            for (int i=a; i <= b; ++i) {
                ScanPoint p = ion.getPeak().getScanPointAt(i);
                mz += p.getMass()*p.getIntensity();
                intens += p.getIntensity();
            }
            mz /= intens;
            ionMass = mz;
        }

        // TODO: mal nachlesen wie man am sinnvollsten quanifiziert
        final double intensity = ion.getPeak().getIntensityAt(ion.getSegment().getApexIndex());
        int isoLen = 0;
        final ArrayList<SimpleSpectrum> correlatedFeatures = new ArrayList<>();
        {
            final SimpleMutableSpectrum isotope = new SimpleMutableSpectrum(ion.getIsotopesAsSpectrum());
            isoLen = isotope.size();
            correlatedFeatures.add(new SimpleSpectrum(isotope));
            for (CorrelatedIon adduct : ion.getAdducts()) {
                correlatedFeatures.add(adduct.ion.getIsotopesAsSpectrum());
            }

        }

        PrecursorIonType ionType = PrecursorIonType.unknown(charge);
        if (ion.getDetectedIonType()!=null) {
            ionType = ion.getDetectedIonType();
        }

        if (ion.getPeakShape()==null)
            fitPeakShape(sample,ion);


        SimpleSpectrum[] spectra = ion.getMsMsScans()==null ? new SimpleSpectrum[0] : Arrays.stream(ion.getMsMsScans()).map(x->ms2Storage.getScan(x)).toArray(SimpleSpectrum[]::new);
        CollisionEnergy[] energies = ion.getEnergies();


        final Feature feature = new Feature(sample.run, ionMass, intensity, getTraceset(sample,ion), correlatedFeatures.toArray(SimpleSpectrum[]::new), 0, spectra,sample.ms2NoiseInformation,energies, ionType, ion.getPossibleAdductTypes(), sample.recalibrationFunction,
                ion.getPeakShape().getPeakShapeQuality(), ion.getMsQuality(), ion.getMsMsQuality(),ion.getChimericPollution()

                );
        feature.setAnnotation(PeakShape.class, fitPeakShape(sample,ion));
        return feature;
    }

    @NotNull
    public static SimpleMutableSpectrum toIsotopeSpectrum(IonGroup ion, double ionMass) {
        return toIsotopeSpectrum(ion.getIsotopes(), ionMass);
    }


    @NotNull
    public static SimpleMutableSpectrum toIsotopeSpectrum(List<CorrelationGroup> ions, double ionMass) {
        final SimpleMutableSpectrum isotope = new SimpleMutableSpectrum();
        isotope.addPeak(ionMass, 1.0d);
        eachPeak:
        for (CorrelationGroup iso : ions) {
            final ChromatographicPeak l = iso.getLeft();
            final ChromatographicPeak r = iso.getRight();
            final ChromatographicPeak.Segment s = iso.getRightSegment();
            double ratios = 0d, mzs = 0d,intens=0d;
            int a = s.getFwhmStartIndex(); int b = s.getFwhmEndIndex(); int n = b-a+1;
            for (; a <= b; ++a) {
                double rInt = r.getIntensityAt(a);
                int iL = l.findScanNumber(r.getScanNumberAt(a));
                if (iL < 0) {
                    LoggerFactory.getLogger(LCMSProccessingInstance.class).warn("Strange isotope peak picked for feature " + iso.getLeft());
                    break eachPeak;
                }
                ratios += rInt / l.getScanPointAt(iL).getIntensity();
                mzs +=  (r.getIntensityAt(a)*r.getMzAt(a));
                intens += r.getIntensityAt(a);
            }
            isotope.addPeak(mzs/intens, ratios/n);
        }
        return isotope;
    }
    public void detectFeatures(ProcessedSample sample) {
        final List<FragmentedIon> ions = new Ms2CosineSegmenter().extractMsMSAndSegmentChromatograms(this, sample);
        detectFeatures(sample,ions);
    }


    void detectFeatures(ProcessedSample sample, List<FragmentedIon> ions) {
        ////
        sample.ions.clear(); sample.ions.addAll(ions);
        assert checkForDuplicates(sample);
        ////
        {
            final double[] intensityAfterPrec = new double[sample.ions.size()];
            int n=0;
            for (int k=0; k < sample.ions.size(); ++k) {
                if (sample.ions.get(k).getMsMsQuality().betterThan(Quality.BAD)) {
                    intensityAfterPrec[k] = sample.ions.get(k).getIntensityAfterPrecursor();
                    ++n;
                }
            }
            Arrays.sort(intensityAfterPrec,0,n);

            int k=n/2;
            while (k < n && intensityAfterPrec[k] <= 0) {
                ++k;
            }

            if (k>=n) {
                sample.intensityAfterPrecursorDistribution = null;
            } else {
                sample.intensityAfterPrecursorDistribution = ExponentialDistribution.getMedianEstimator().extimateByMedian(intensityAfterPrec[k]);
                LoggerFactory.getLogger(LCMSProccessingInstance.class).info("Median intensity after precursor in MS/MS: " + intensityAfterPrec[k]);
            }
        }
        ListIterator<FragmentedIon> iter = ions.listIterator();
        final CorrelatedPeakDetector detector = new CorrelatedPeakDetector(detectableIonTypes);
        while (iter.hasNext()) {
            final FragmentedIon ion = iter.next();
            if (!detector.detectCorrelatedPeaks(sample, ion))
                iter.remove();
        }
        assert checkForDuplicates(sample);
        sample.ions.clear();
        sample.ions.addAll(ions);
        /*
        sample.ions.clear();
        sample.ions.addAll(new IonIdentityNetwork().filterByIonIdentity(ions));
        assert checkForDuplicates(sample);
         */


        TDoubleArrayList peakWidths = new TDoubleArrayList(),peakWidthsToHeight = new TDoubleArrayList();
        for (FragmentedIon f : sample.ions) {
            final long fwhm = f.getSegment().fwhm(0.2);
            peakWidths.add(fwhm);
            peakWidthsToHeight.add(fwhm/f.getIntensity());
        }
        peakWidths.sort();
        peakWidthsToHeight.sort();
        sample.meanPeakWidth = Statistics.robustAverage(peakWidths.toArray());
        sample.meanPeakWidthToHeightRatio = Statistics.robustAverage(peakWidthsToHeight.toArray());
        for (int k=0; k < peakWidthsToHeight.size(); ++k) {
            peakWidthsToHeight.set(k, Math.pow(peakWidthsToHeight.get(k)-sample.meanPeakWidthToHeightRatio,2));
        }
        sample.meanPeakWidthToHeightRatioStd = Math.sqrt(Statistics.robustAverage(peakWidthsToHeight.toArray()));
        for (FragmentedIon ion : ions) {
            fitPeakShape(sample, ion);
        }
    }

    public PeakShape fitPeakShape(ProcessedSample sample, FragmentedIon ion) {
        /*
        final GaussianShape gaus = new GaussianFitting().fit(sample, ion.getPeak(), ion.getSegment());
        final LaplaceShape laplace = new LaplaceFitting().fit(sample, ion.getPeak(), ion.getSegment());
        if (gaus.getScore()>laplace.getScore()) {
            ion.setPeakShape(gaus);
            return gaus;
        } else {
            ion.setPeakShape(laplace);
            return laplace;
        }
         */
        final CustomPeakShape fit = new CustomPeakShapeFitting().fit(sample, ion.getPeak(), ion.getSegment());
        ion.setPeakShape(fit);
        return fit;
    }

    /**
     *
     */
    void addAllSegmentsAsPseudoIons() {
        for (ProcessedSample sample : samples) {
            final HashSet<ChromatographicPeak.Segment> allSegments = new HashSet<ChromatographicPeak.Segment>();
            for (FragmentedIon ion : sample.ions) {
                allSegments.add(ion.getSegment());
            }
            for (FragmentedIon ion : sample.gapFilledIons) {
                allSegments.add(ion.getSegment());
            }
            for (FragmentedIon ion : sample.ions) {
                for (ChromatographicPeak.Segment s : ion.getPeak().getSegments().values()) {
                    if (s.isNoise()) continue;
                    if (!allSegments.contains(s)) {
                        final GapFilledIon newIon = new GapFilledIon(Polarity.of(ion.getPolarity()), ion.getPeak(), s, ion);
                        fitPeakShape(sample, newIon);
                        sample.gapFilledIons.add(newIon);
                        allSegments.add(s);
                    }
                }
            }
        }
    }

    public void detectFeatures() {
        for (ProcessedSample sample : samples) {
            detectFeatures(sample);
        }
    }

    public Cluster alignAndGapFilling() {
        return alignAndGapFilling(null);
    }

    private void setPeakShapeQualities(JobManager manager, Cluster c) {
        final List<BasicJJob> jobs = new ArrayList<>();
        for (final AlignedFeatures f : c.getFeatures()) {
            jobs.add(manager.submitJob(new BasicJJob<Object>(){
                @Override
                protected Object compute() throws Exception {
                    for (var pair : f.getFeatures().entrySet()) {
                        if (pair.getValue().getPeakShape()==null) {
                            pair.getValue().setPeakShape(fitPeakShape(pair.getKey(),pair.getValue()));
                        }
                    }
                    return null;
                }
            }));
        }
        jobs.forEach(JJob::takeResult);
    }

    public Cluster alignAndGapFilling(ProgressJJob<?> jobWithProgress) {
        final int maxProgress = 7;
        int currentProgress = 0;
        JobManager manager = SiriusJobs.getGlobalJobManager();
        if (jobWithProgress!=null) jobWithProgress.updateProgress(0, maxProgress, currentProgress++, "Estimate retention time shifts between samples");
        boolean similarRt = true;
        int numberOfUnalignedIons = 0;
        double maxRt = samples.stream().mapToDouble(x->x.maxRT).max().getAsDouble();
        for (ProcessedSample s : samples) {
            s.maxRT = maxRt;
            numberOfUnalignedIons += s.ions.size();
        }
        //double error = new Aligner(false).estimateErrorTerm(samples);
        LaplaceDistribution error = new Aligner(false).estimateErrorLaplace(samples);
        System.out.println("ERROR = " + error.getScale());

        final double initialError = error.getScale();
        int n=0;
        if (jobWithProgress!=null) jobWithProgress.updateProgress(0, maxProgress, currentProgress++, "Filter data: remove ions with low quality MS/MS spectrum and MS1 peak");
        System.out.println("Start with " + numberOfUnalignedIons + " unaligned ions.");
        System.out.println("Remove features with low MS/MS quality that do not align properly");System.out.flush();
        int deleted = manager.submitJob(new Aligner(false).prealignAndFeatureCutoff2(samples, 4*error.getScale(), 1)).takeResult();
        System.out.println("Remove " + deleted + " features that do not align well. Keep " + (numberOfUnalignedIons-deleted) + " features." );

        addAllSegmentsAsPseudoIons();
        if (jobWithProgress!=null) jobWithProgress.updateProgress(0, maxProgress, currentProgress++, "Start first alignment ");
        BasicJJob<Cluster> clusterJob = new Aligner2(error).align(samples);//new Aligner().recalibrateRetentionTimes(this.samples);
        manager.submitJob(clusterJob);
        Cluster cluster = clusterJob.takeResult();
        error = cluster.estimateLaplaceError();
        final double errorFromClustering = error.getScale();
        clusterJob = new GapFilling().gapFillingInParallel(this, cluster.deleteRowsWithNoMsMs(), error.getScale(),cluster.estimatePeakShapeError(), Quality.GOOD);
        manager.submitJob(clusterJob);
        cluster = clusterJob.takeResult();

        if (jobWithProgress!=null) jobWithProgress.updateProgress(0, maxProgress, currentProgress++, "Estimate parameters and start second alignment");
        clusterJob = new Aligner2(error).align(samples);
        manager.submitJob(clusterJob);
        cluster = clusterJob.takeResult();

        if (jobWithProgress!=null) jobWithProgress.updateProgress(0, maxProgress, currentProgress++, "Recalibrate retention times");
        setPeakShapeQualities(manager, cluster);
        manager.submitJob(new Aligner(false).recalibrateRetentionTimes(samples, cluster, error.getScale())).takeResult();
        error = cluster.estimateLaplaceError();
        final double errorDueToRecalibration = error.getScale();
        addAllSegmentsAsPseudoIons();
        if (jobWithProgress!=null) jobWithProgress.updateProgress(0, maxProgress, currentProgress++, "Start third alignment");
        clusterJob  = new Aligner2(error).align(samples);
        manager.submitJob(clusterJob);
        cluster = clusterJob.takeResult().deleteRowsWithNoMsMs();

        System.out.println("Start Gapfilling #2");System.out.flush();
        clusterJob = new GapFilling().gapFillingInParallel(this, cluster, error.getScale(), cluster.estimatePeakShapeError(), Quality.DECENT);
        manager.submitJob(clusterJob);
        cluster = clusterJob.takeResult();


        final double finalError = cluster.estimateError(true);

        System.out.println("########################################");
        System.out.println("Initial Error: " + initialError);
        System.out.println("After clustering: " + errorFromClustering);
        System.out.println("After Recalibration: " + errorDueToRecalibration);
        System.out.println("After Gap-Filling: " + finalError);
        System.out.println("PeakShape Error: " + cluster.estimatePeakShapeError());
        if (jobWithProgress!=null) jobWithProgress.updateProgress(0, maxProgress, currentProgress++, "Start final alignment");
        cluster = manager.submitJob(new Aligner2(error).align(samples)).takeResult();
        double numberOfFeatures = cluster.getFeatures().length;
        cluster = cluster.deleteRowsWithNoMsMs();
        cluster = cluster.deleteRowsWithNoIsotopes();
        if (internalStatistics!=null) collectAlignmentStatistics(cluster, numberOfFeatures - cluster.getFeatures().length);


        double numberOfFeatures2 = cluster.getFeatures().length;
        System.out.println("Remove " + (100d - 100d*numberOfFeatures2/numberOfFeatures ) +  " % of the data due to low quality. There are " + cluster.getFeatures().length + " features in total."); System.out.flush();
        if (samples.size()>=50) cluster = cluster.deleteRowsWithTooFewEntries(4);
        int after = cluster.getFeatures().length;
        System.out.println("Done."); System.out.flush();
        System.out.println("Total number of features is " + cluster.getFeatures().length);
        if (internalStatistics!=null) collectFeatureStatistics(Arrays.stream(cluster.getFeatures()).flatMap(x->x.getFeatures().values().stream()).toArray(FragmentedIon[]::new));
        return cluster;


    }

    private void collectFeatureStatistics(FragmentedIon[] ions) {
        HashMap<ChromatographicPeak,Integer> peaks = new HashMap<>();
        for (FragmentedIon ion : ions) {
            if (ion.isCompound()) {
                internalStatistics.numberOfCorrelatedPeaksPerFeature.add(
                        ion.getAdducts().size() + ion.getInSourceFragments().size()
                );
                internalStatistics.precursorMasses.add(ion.getMass());
                if (ion.getMass()>1000) internalStatistics.numberOfPeaksWithMassAbove1000++;
                internalStatistics.chimericPollution.add(ion.getChimericPollution());
                internalStatistics.numberOfIsotopePeaksPerFeature.add(ion.getIsotopes().size());
                ion.getIsotopes().forEach(x -> internalStatistics.isotopicCorrelation.add(x.getCorrelation()));
                internalStatistics.featureWidths.add(ion.getSegment().retentionTimeWidth());
                internalStatistics.featureFWHM.add(ion.getSegment().fwhm());
                internalStatistics.featureHeights.add(ion.getSegment().getApexIntensity());
                final Range<Integer> integerRange = ion.getSegment().calculateFWHM(0.25);
                internalStatistics.scanPointsPerFeaturesAt25.add(integerRange.getMaximum()-integerRange.getMinimum()+1);
                if (peaks.containsKey(ion.getPeak())) {
                    internalStatistics.segmentsPerPeak.add(ion.getPeak().segments.size());
                }
                peaks.compute(ion.getPeak(), (key, value)->value==null ? 1 : value+1);
            }
        }
        peaks.forEach((k,v)->internalStatistics.msmsPerPeak.add(v));
    }

    private void collectAlignmentStatistics(Cluster cluster, double deletedFeatures) {
        final double n = this.samples.size();
        internalStatistics.numberOfFeatures=cluster.getFeatures().length;
        for (AlignedFeatures f : cluster.getFeatures()) {
            final FragmentedIon[] xs = f.getFeatures().values().toArray(FragmentedIon[]::new);
            long maximum = 0L;
            for (int i=0; i < xs.length; ++i) {
                for (int j=0; j < i; ++j) {
                    final long ret = Math.abs(xs[i].getRetentionTime() - xs[j].getRetentionTime());
                    internalStatistics.retentionTimeShift.add((float)ret);
                    maximum = Math.max(maximum, ret);
                }
            }
            internalStatistics.numberOfSamplesPerFeature.add(f.getFeatures().size());
            internalStatistics.ratioOfSamplesPerFeature.add(f.getFeatures().size()/n);
            if (xs.length>1) internalStatistics.maximumRetentionTimeShiftPerFeature.add((float)maximum);
        }
        internalStatistics.deletedFeatures = (float)(deletedFeatures/(cluster.getFeatures().length + deletedFeatures));
    }


    private boolean checkForDuplicates(Cluster cluster) {
        final HashSet<ChromatographicPeak.Segment> alLSegments = new HashSet<>();
        for (AlignedFeatures al  : cluster.getFeatures()) {
            for (FragmentedIon I : al.getFeatures().values()) {
                if (!alLSegments.add(I.getSegment())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkForDuplicates(ProcessedSample sample) {
        final HashMap<ChromatographicPeak.Segment, FragmentedIon> set = new HashMap<>();
        for (FragmentedIon ion : sample.ions) {
            if (set.putIfAbsent(ion.getSegment(), ion)!=null)
                return false;
        }
        for (FragmentedIon ion : sample.gapFilledIons) {
            if (set.putIfAbsent(ion.getSegment(), ion)!=null)
                return false;
        }
        return true;
    }

    public ConsensusFeature[] makeConsensusFeatures(Cluster cluster) {
        return new Aligner(false).makeFeatureTable(this, cluster);
    }


    public List<ProcessedSample> getSamples() {
        return samples;
    }

    public SimpleSpectrum getMs2(Scan msMsScan) {
        return ms2Storage.getScan(msMsScan);
    }

    public MassToFormulaDecomposer getFormulaDecomposer() {
        return formulaDecomposer;
    }




}
