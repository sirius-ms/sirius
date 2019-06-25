package de.unijena.bioinf.lcms;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.align.AlignedFeatures;
import de.unijena.bioinf.lcms.align.Aligner;
import de.unijena.bioinf.lcms.align.Cluster;
import de.unijena.bioinf.lcms.align.GapFilling;
import de.unijena.bioinf.lcms.noise.NoiseStatistics;
import de.unijena.bioinf.lcms.peakshape.CustomPeakShape;
import de.unijena.bioinf.lcms.peakshape.CustomPeakShapeFitting;
import de.unijena.bioinf.lcms.peakshape.PeakShape;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TDoubleArrayList;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LCMSProccessingInstance {

    protected HashMap<ProcessedSample, SpectrumStorage> storages;
    protected List<ProcessedSample> samples;

    public LCMSProccessingInstance() {
        this.samples = new ArrayList<>();
        this.storages = new HashMap<ProcessedSample, SpectrumStorage>();

    }



    public ProcessedSample addSample(LCMSRun run, SpectrumStorage storage) {
        final NoiseStatistics noiseStatisticsMs1 = new NoiseStatistics(20, 0.85), noiseStatisticsMs2 = new NoiseStatistics(10, 0.85);
        for (Scan s : run.getScans()) {
            if (s.isMsMs()) noiseStatisticsMs2.add(s, storage.getScan(s));
            else noiseStatisticsMs1.add(s,storage.getScan(s));
        }
        final ProcessedSample sample = new ProcessedSample(
                run, noiseStatisticsMs1.getLocalNoiseModel(), noiseStatisticsMs2.getGlobalNoiseModel(),
                new ChromatogramCache(), storage
        );
        this.samples.add(sample);
        this.storages.put(sample, storage);
        return sample;

    }

    public Feature makeFeature(ProcessedSample sample, FragmentedIon ion, boolean gapFilled) {
        final ArrayList<ScanPoint> trace = new ArrayList<>();
        final ArrayList<ScanPoint> debugTrace = new ArrayList<>();

        for (int a=ion.getSegment().getStartIndex(), b = ion.getSegment().getEndIndex(); a <= b; ++a) {
            trace.add(ion.getPeak().getScanPointAt(a));
        }


        for (int a=Math.max(0, ion.getSegment().getStartIndex()-10), b = Math.min(ion.getSegment().getEndIndex()+10, ion.getPeak().numberOfScans()-1); a <= b; ++a) {
            debugTrace.add(ion.getPeak().getScanPointAt(a));
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

        final ArrayList<SimpleSpectrum> correlatedFeatures = new ArrayList<>();
        {
            final SimpleMutableSpectrum isotope = toIsotopeSpectrum(ion, ionMass);
            correlatedFeatures.add(new SimpleSpectrum(isotope));
            for (CorrelatedIon adduct : ion.getAdducts()) {
                correlatedFeatures.add(new SimpleSpectrum(toIsotopeSpectrum(adduct.ion, adduct.ion.getPeak().getMzAt(adduct.ion.getPeak().findScanNumber(ion.getSegment().getApexScanNumber())))));
            }

        }

        PrecursorIonType ionType = PrecursorIonType.unknown(ion.getChargeState());
        if (ion.getDetectedIonType()!=null) {
            ionType = ion.getDetectedIonType();
        }

        if (ion.getPeakShape()==null)
            fitPeakShape(sample,ion);


        final Feature feature = new Feature(sample.run, ionMass, intensity, trace.toArray(new ScanPoint[0]), correlatedFeatures.toArray(new SimpleSpectrum[0]), gapFilled ? new SimpleSpectrum[0] : new SimpleSpectrum[]{ion.getMsMs().finishMerging()}, ionType, sample.recalibrationFunction,
                ion.getPeakShape().getPeakShapeQuality(), ion.getMsQuality(), ion.getMsMsQuality()

                );
        feature.completeTraceDebug = debugTrace.toArray(new ScanPoint[0]);
        feature.setAnnotation(PeakShape.class, fitPeakShape(sample,ion));
        return feature;
    }

    @NotNull
    private SimpleMutableSpectrum toIsotopeSpectrum(IonGroup ion, double ionMass) {
        final SimpleMutableSpectrum isotope = new SimpleMutableSpectrum();
        isotope.addPeak(ionMass, 1.0d);
        eachPeak:
        for (CorrelationGroup iso : ion.getIsotopes()) {
            final ChromatographicPeak l = iso.getLeft();
            final ChromatographicPeak r = iso.getRight();
            final ChromatographicPeak.Segment s = iso.getRightSegment();
            double ratios = 0d, mzs = 0d,intens=0d;
            int a = s.getFwhmStartIndex(); int b = s.getFwhmEndIndex(); int n = b-a+1;
            for (; a <= b; ++a) {
                double rInt = r.getIntensityAt(a);
                int iL = l.findScanNumber(r.getScanNumberAt(a));
                if (iL < 0) {
                    LoggerFactory.getLogger(LCMSProccessingInstance.class).warn("Strange isotope peak picked for feature " + ion);
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
        final List<FragmentedIon> ions = new Ms2CosineSegmenter().extractMsMSAndSegmentChromatograms(sample);
        ////
        sample.ions.clear(); sample.ions.addAll(ions);
        assert checkForDuplicates(sample);
        ////
        ListIterator<FragmentedIon> iter = ions.listIterator();
        final CorrelatedPeakDetector detector = new CorrelatedPeakDetector();
        while (iter.hasNext()) {
            final FragmentedIon ion = iter.next();
            if (!detector.detectCorrelatedPeaks(sample, ion))
                iter.remove();
        }
        assert checkForDuplicates(sample);
        sample.ions.clear();
        sample.ions.addAll(new IonIdentityNetwork().filterByIonIdentity(ions));
        assert checkForDuplicates(sample);
        sample.ions.forEach(x->x.getMsMs().applyNoiseFiltering());
        TDoubleArrayList peakWidths = new TDoubleArrayList();
        for (FragmentedIon f : sample.ions) peakWidths.add(f.getSegment().fwhm(0.1));
        peakWidths.sort();
        sample.meanPeakWidth = Statistics.robustAverage(peakWidths.toArray());
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

    public void detectFeatures() {
        for (ProcessedSample sample : samples) {
            detectFeatures(sample);
        }
    }

    public Cluster alignAndGapFilling() {
        boolean similarRt = true;
        double maxRt = samples.stream().mapToDouble(x->x.maxRT).max().getAsDouble();
        for (ProcessedSample s : samples) {
            s.maxRT = maxRt;
        }
        double error = new Aligner(false).estimateErrorTerm(samples);
        System.out.println("ERROR = " + error);

        final double initialError = error;
/*
        ///////////////////
        // find 15 % most abundant features and gapfill them in all samples
        //////////////////
        final HashSet<ChromatographicPeak.Segment> alreadyKnown = new HashSet<>();
        final SimpleMutableSpectrum mzAndRet = new SimpleMutableSpectrum();
        List<FragmentedIon> allIons = new ArrayList<>();
        for (ProcessedSample sample : samples) {
            allIons.addAll(sample.ions);
        }
        allIons.sort(Comparator.comparingDouble(FragmentedIon::getIntensity).reversed());
        allIons = allIons.subList(0, (int)Math.ceil(allIons.size()*0.15));

 */
        for (ProcessedSample sample : samples) {
            assert checkForDuplicates(sample) : sample.toString();
        }

        {
            final HashSet<ChromatographicPeak.Segment> segs = new HashSet<>();
            for (ProcessedSample sample : samples) {
                // add all segments as ions for alignment
                segs.clear();
                sample.ions.forEach(x->x.getPeak().getSegments().forEach(segs::add));
                sample.ions.forEach(x->segs.remove(x.getSegment()));
                for (ChromatographicPeak.Segment s : segs) {
                    final PeakShape shape = new CustomPeakShapeFitting().fit(sample,s.getPeak(),s);
                    if (shape.getPeakShapeQuality().betterThan(Quality.BAD)) {
                        final GapFilledIon ion = new GapFilledIon(s.getPeak(),s,null);
                        if (ion.getIntensity() >= sample.ms1NoiseModel.getSignalLevel(s.getApexScanNumber(), ion.getMass())) {
                            ion.setPeakShape(shape);
                            sample.otherIons.add(ion);
                        }
                    }
                }
            }
        }

        Cluster cluster = new Aligner(false).upgma(samples,5*error,true);//new Aligner().recalibrateRetentionTimes(this.samples);

        assert cluster.getMergedSamples().size() == getSamples().size();
        //cluster = new Aligner().realign(recalibrated,error);//.upgma(this.samples, error, true);
        error = cluster.estimateError();
        assert checkForDuplicates(cluster);
        for (ProcessedSample sample : samples) {
            assert checkForDuplicates(sample) : sample.toString();
        }
        final double errorFromClustering = error;
        System.out.println("Start Gap Filling #1");
        cluster = new GapFilling().gapFilling(this, cluster.deleteRowsWithNoMsMs(), error,cluster.estimatePeakShapeError(), true);
        assert checkForDuplicates(cluster);
        cluster = new Aligner(false).realign(cluster, error);
        assert checkForDuplicates(cluster);
        for (ProcessedSample sample : samples) {
            assert checkForDuplicates(sample) : sample.toString();
        }
        assert cluster.getMergedSamples().size() == getSamples().size();
        assert checkForDuplicates(cluster);
        new Aligner(false).recalibrateRetentionTimes(samples, cluster, error);
        error = cluster.estimateError();
        final double errorDueToRecalibration = error;
        cluster = new Aligner(false).upgma(samples,error,true).deleteRowsWithNoMsMs();
        cluster = new GapFilling().gapFilling(this, cluster, error, cluster.estimatePeakShapeError(), false);
        assert checkForDuplicates(cluster);
        for (ProcessedSample sample : samples) {
            assert checkForDuplicates(sample) : sample.toString();
        }
        assert cluster.getMergedSamples().size() == samples.size();
        final double finalError = cluster.estimateError();

        System.out.println("########################################");
        System.out.println("Initial Error: " + initialError);
        System.out.println("After clustering: " + errorFromClustering);
        System.out.println("After Recalibration: " + errorDueToRecalibration);
        System.out.println("After Gap-Filling: " + finalError);
        System.out.println("PeakShape Error: " + cluster.estimatePeakShapeError());

        cluster = new Aligner(false).realign(cluster,error).deleteRowsWithNoMsMs();

        return cluster;


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
}
