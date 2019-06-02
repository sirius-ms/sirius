package de.unijena.bioinf.lcms;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.model.lcms.*;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CorrelatedPeakDetector {

    protected final LCMSProccessingInstance instance;
    protected static final Range<Double>[] ISO_RANGES = new Range[]{
            Range.closed(0.99664664 - 0.002, 1.00342764 + 0.002),
            Range.closed(1.99653883209004 - 0.002, 2.0067426280592295 + 0.002),
            Range.closed(2.9950584 - 0.002, 3.00995027 + 0.002),
            Range.closed(3.99359037 - 0.002, 4.01300058 + 0.002),
            Range.closed(4.9937908 - 0.002, 5.01572941 + 0.002)
    };

    public CorrelatedPeakDetector(LCMSProccessingInstance instance) {
        this.instance = instance;
    }

    public boolean doIAmAnIsotope(FragmentedIon ion, TDoubleArrayList alreadyAnnotatedMzs) {
        // we assume that the peak might be either the second or the third isotopic peak
        Scan ms1Scan = instance.getLcms().getScanByNumber(ion.getSegment().getApexScanNumber()).get();
        final SimpleSpectrum ms1 = instance.getStorage().getScan(ms1Scan);
        final ScanPoint ionPeak = ion.getPeak().getScanPointAt(ion.getSegment().getApexIndex());
        int peakBefore = Spectrums.mostIntensivePeakWithin(ms1, ionPeak.getMass() - 1.0034d, new Deviation(20,0.01));
        if (peakBefore < 0) return false;
        double intensity = ms1.getIntensityAt(peakBefore);
        if (intensity/ionPeak.getIntensity() < 0.33) {
            // this is not the second peak. Maybe the third peak of a bromine/chlorine pattern?
            peakBefore = Spectrums.mostIntensivePeakWithin(ms1, ionPeak.getMass() - 2.0016d, new Deviation(20,0.01));
            if (peakBefore < 0) return false;
            intensity = ms1.getIntensityAt(peakBefore);
            if (intensity/ionPeak.getIntensity() < 0.33)
                return false;
        }
        Optional<ChromatographicPeak> peakBeforeChr = instance.getBuilder().detectExact(ms1Scan, ms1.getMzAt(peakBefore));
        if (!peakBeforeChr.isPresent()) return false;
        Optional<ChromatographicPeak.Segment> segmentForScanId = peakBeforeChr.get().getSegmentForScanId(ms1Scan.getScanNumber());
        if (!segmentForScanId.isPresent()) return false;
        List<CorrelationGroup> correlationGroups = detectIsotopesFor(peakBeforeChr.get(), segmentForScanId.get(), ion.getChargeState());
        for (CorrelationGroup g : correlationGroups) {
            int scanNumber = g.getRight().findScanNumber(ms1Scan.getScanNumber());
            if (scanNumber >= 0 && g.getCorrelation() >= 0.95 && Math.abs(g.getRight().getMzAt(scanNumber) - ionPeak.getMass())<1e-8) {
                System.out.println(ion + " :: IS AN ISOTOPE!!! ");
                System.out.println(g.getLeft().getScanPointAt(g.getLeft().findScanNumber(ms1Scan.getScanNumber())));
                for (CorrelationGroup h : correlationGroups) {
                    System.out.println(h.getRight().getScanPointAt(h.getRight().findScanNumber(ms1Scan.getScanNumber())) + " " + h);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Detect and annotate correlated adducts and isotopes.
     * @param ion
     * @return false, if the ion itself is an isotope
     */
    public boolean detectCorrelatedPeaks(FragmentedIon ion) {
        // ensure that we do not double-annotate a peak
        final TDoubleArrayList alreadyAnnotatedMzs = new TDoubleArrayList();
        // 1. detect isotopes of main feature
        detectIsotopesAndChargeStateFor(ion, alreadyAnnotatedMzs);

        // do I am an isotope peak myself?
        if (doIAmAnIsotope(ion, alreadyAnnotatedMzs)) {
            return false;
        }

        // 2. detect adducts and in-source fragments
        detectAdductsAndInSourceFor(ion,alreadyAnnotatedMzs);
        // 3. detect isotopes of adducts/in-source fragments
        detectInSourceFragmentsFor(ion,alreadyAnnotatedMzs);
        return true;

    }

    private void detectInSourceFragmentsFor(FragmentedIon ion, TDoubleArrayList alreadyAnnotatedMzs) {
        final Deviation dev = new Deviation(20);
        final MergedSpectrum spectrum = ion.getMsMs();
        Scan ms1Scan = instance.getLcms().getScanByNumber(ion.getSegment().getApexScanNumber()).get();
        final SimpleSpectrum ms1 = instance.getStorage().getScan(ms1Scan);
        final double basePeak = Spectrums.getMaximalIntensity(spectrum);
        double precursor = ion.getMsMs().getPrecursor().getMass();
        for (int k=0; k < spectrum.size(); ++k) {
            if (spectrum.getMzAt(k) >= (precursor-2))
                continue;
            if (spectrum.getIntensityAt(k)/basePeak >= 0.05) {
                final double peak = spectrum.getMzAt(k);
                int l = Spectrums.mostIntensivePeakWithin(ms1, peak, dev);
                if (l<0)
                    continue;
                final double intensity = ms1.getIntensityAt(l) / ion.getPeak().getIntensityAt(ion.getSegment().getApexIndex());
                if (intensity >= 0.1) {
                    if (alreadyFound(alreadyAnnotatedMzs, ms1.getMzAt(l)))
                        continue;
                    // might be a in-source fragment
                    final Optional<ChromatographicPeak> detection = instance.getBuilder().detectExact(ms1Scan,ms1.getMzAt(l));
                    if (detection.isPresent()) {

                        CorrelationGroup correlate = correlate(ion.getPeak(), ion.getSegment(), detection.get());
                        if (correlate==null) continue;
                        if (correlate.getCorrelation() >= 0.95) {
                            ion.getInSourceFragments().add(new CorrelatedIon(correlate, ionWithIsotopes(correlate.getRight(), correlate.getRightSegment(), ion.getChargeState())));
                            System.out.println(ion + " :: Found IN-SOURCE FRAGMENT with delta m/z = " + (ion.getMsMs().getPrecursor().getMass() - peak)  + ", " + correlate );
                            System.out.println("Add insource " + correlate.getRight().getScanPointForScanId(ms1Scan.getScanNumber()).getMass());
                            List<CorrelationGroup> isos = detectIsotopesFor(correlate.getRight(), correlate.getRightSegment(), 1);
                            alreadyAnnotatedMzs.add(ms1.getMzAt(l));
                            for (CorrelationGroup isotopePeak : isos) {
                                alreadyAnnotatedMzs.add(isotopePeak.getRight().getScanPointForScanId(ms1Scan.getScanNumber()).getMass());
                                System.out.println("Add insource isotope " + isotopePeak.getRight().getScanPointForScanId(ms1Scan.getScanNumber()).getMass());
                            }
                            System.out.println(Arrays.toString(alreadyAnnotatedMzs.toArray()));
                        }
                    }
                }
            }
        }
    }

    private boolean alreadyFound(TDoubleArrayList alreadyannotated, double mz) {
        for (int aap=0; aap < alreadyannotated.size(); ++aap) {
            if (Math.abs(mz-alreadyannotated.getQuick(aap))<1e-6) {
                return true;
            }
        }
        return false;
    }

    private void detectAdductsAndInSourceFor(FragmentedIon ion, TDoubleArrayList alreadyAnnotatedMzs) {
        final List<PrecursorIonType> adductTypes = new ArrayList<>(Arrays.asList(
                PrecursorIonType.fromString("[M+Na]+"),
        PrecursorIonType.fromString("[M+K]+"),
                PrecursorIonType.fromString("[M+H]+")
        ));

        final ArrayList<PrecursorIonType> detectedIonTypes = new ArrayList<>(), possibleIonTypes = new ArrayList<>();
        final ArrayList<CorrelatedIon> adducts = new ArrayList<>();
        ScanPoint scanPoint = ion.getPeak().getScanPointAt(ion.getSegment().getApexIndex());
        Scan ms1Scan = instance.getLcms().getScanByNumber(scanPoint.getScanNumber()).get();
        for (PrecursorIonType ionType : adductTypes) {
            for (PrecursorIonType other : adductTypes) {
                if (other!=ionType) {
                    final double delta = other.getModificationMass() - ionType.getModificationMass();
                    Optional<ChromatographicPeak> detect = instance.getBuilder().detect(ms1Scan, scanPoint.getMass() + delta);
                    if (detect.isPresent()) {
                        double peakMass = detect.get().getScanPointForScanId(ms1Scan.getScanNumber()).getMass();
                        if (alreadyFound(alreadyAnnotatedMzs, peakMass))
                            continue;
                        // add ion as possibleIonType. But first make correlation analysis
                        CorrelationGroup correlate = correlate(ion.getPeak(), ion.getSegment(), detect.get());
                        if (correlate != null && correlate.getCorrelation() >= 0.9) {
                            adducts.add(new CorrelatedIon(correlate, ionWithIsotopes(correlate.getRight(), correlate.getRightSegment(), ion.getChargeState())));
                            detectedIonTypes.add(ionType);
                            System.out.println(ion.toString() + " :: Found " + ionType.toString() + " -> " + other.toString() + " with correlation " + correlate);
                            alreadyAnnotatedMzs.add(peakMass);
                        } else {
                            possibleIonTypes.add(ionType);
                            System.out.println(ion.toString() + " :: There might be " + ionType.toString() + " -> " + other.toString()+ ". Correlate:  " + String.valueOf(correlate));
                        }
                    }
                }
            }
        }
        if (detectedIonTypes.size()==0)
            System.out.println(ion.toString() + " :: NO adducts found.");
        ion.getAdducts().addAll(adducts);

    }

    public void detectIsotopesAndChargeStateFor(FragmentedIon ion, TDoubleArrayList alreadyAnnotatedMzs) {
        final Scan ms1Scan = instance.getLcms().getScanByNumber(ion.getSegment().getApexScanNumber()).get();
        // try different chare states
        for (int charge = 1; charge < 4; ++charge) {
            final List<CorrelationGroup> isoPeaks = detectIsotopesFor(ion.getPeak(), ion.getSegment(), charge);
            if (isoPeaks.size() > 0) {
                for (CorrelationGroup isotopePeak : isoPeaks) {
                    alreadyAnnotatedMzs.add(isotopePeak.getRight().getScanPointForScanId(ms1Scan.getScanNumber()).getMass());
                }
                ion.setChargeState(charge);
                ion.addIsotopes(isoPeaks);
                System.out.println(ion +  " Found " + isoPeaks.size() + " isotopes with correlations " + isoPeaks.stream().mapToDouble(CorrelationGroup::getCorrelation).min().getAsDouble() + " .. " + isoPeaks.stream().mapToDouble(CorrelationGroup::getCorrelation).max().getAsDouble());
                return;
            }
        }
    }

    public IonGroup ionWithIsotopes(ChromatographicPeak peak, ChromatographicPeak.Segment segment, int charge) {
        return new IonGroup(peak,detectIsotopesFor(peak,segment,charge));
    }


    public List<CorrelationGroup> detectIsotopesFor(ChromatographicPeak peak, ChromatographicPeak.Segment segment, int charge) {

        Scan scan = instance.getLcms().getScanByNumber(segment.getApexScanNumber()).get();
        final SimpleSpectrum spectrum = instance.getStorage().getScan(scan);
        final List<CorrelationGroup> isoPeaks = new ArrayList<>();
        final double mz = peak.getMzAt(segment.getApexIndex());
        forEachIsotopePeak:
        for (int k = 0; k < ISO_RANGES.length; ++k) {
            // try to detect +k isotope peak
            final double maxMz = mz + ISO_RANGES[k].upperEndpoint();
            final int a = Spectrums.indexOfFirstPeakWithin(spectrum, mz + ISO_RANGES[k].lowerEndpoint(), maxMz);
            if ( a < 0) continue;
            int nsize = isoPeaks.size();
            for (int i=a; i < spectrum.size(); ++i) {
                if (spectrum.getMzAt(i) > maxMz)
                    break;
                instance.getBuilder().detectExact(scan, spectrum.getMzAt(i)).map(x->correlate(peak, segment, x)).filter(x->x.getCorrelation() >= 0.9).ifPresent(isoPeaks::add);
            }
            if (isoPeaks.size() <= nsize) {
                break forEachIsotopePeak;
            }
        }
        return isoPeaks;
    }


    protected CorrelationGroup correlate(ChromatographicPeak main, ChromatographicPeak.Segment mainSegment, ChromatographicPeak mightBeCorrelated) {
        // overlap both
        int start = mainSegment.getStartScanNumber();
        int end = mainSegment.getEndScanNumber();

        int k = mightBeCorrelated.findScanNumber(start);
        if (k < 0) {
            k = -(k+1);
        }
        int otherStart = k;
        if (otherStart >= mightBeCorrelated.numberOfScans())
            return null;
        int otherEnd;
        for (otherEnd=otherStart+1; otherEnd < mightBeCorrelated.numberOfScans(); ++otherEnd) {
            if (mightBeCorrelated.getScanNumberAt(otherEnd) > end)
                break;
        }
        --otherEnd;
        assert otherEnd < mightBeCorrelated.numberOfScans();
        if (otherEnd < otherStart) {
            // no overlap
            return null;
        }

        final ChromatographicPeak.Segment otherSegment = mightBeCorrelated.createSegmentFromIndizes(otherStart,otherEnd);

        if (main.getIntensityAt(mainSegment.getApexIndex()) > mightBeCorrelated.getIntensityAt(otherSegment.getApexIndex())) {
            return correlateBiggerToSmaller(main,mainSegment, mightBeCorrelated, otherSegment);
        } else return correlateBiggerToSmaller(mightBeCorrelated, otherSegment, main, mainSegment).invert();



    }

    private CorrelationGroup correlateBiggerToSmaller(ChromatographicPeak large, ChromatographicPeak.Segment largeSegment, ChromatographicPeak small, ChromatographicPeak.Segment smallSegment) {
        final TDoubleArrayList a = new TDoubleArrayList(), b = new TDoubleArrayList();

        // find index that is above 25% intensity of main peak
        final Range<Integer> t25 = smallSegment.calculateFWHM(0.15d);


        for (int i = t25.lowerEndpoint(); i < t25.upperEndpoint(); ++i) {
            int j = large.findScanNumber(small.getScanNumberAt(i));
            if (j >= 0) a.add(large.getIntensityAt(j));
            else a.add(0d);
            b.add(small.getIntensityAt(i));
        }
        final double correlation = pearson(a,b,a.size());
        return new CorrelationGroup(large,small,largeSegment,smallSegment,t25.upperEndpoint()-t25.lowerEndpoint(), correlation);
    }


    private double pearson(TDoubleArrayList a, TDoubleArrayList b, int n) {

        double meanA=0d;
        double meanB=0d;
        for (int i=0; i < n; ++i) {
            meanA += a.getQuick(i);
            meanB += b.getQuick(i);
        }
        meanA /= n;
        meanB /= n;
        double va=0d, vb=0d, vab=0d;
        for (int i=0; i < n; ++i) {
            final double x = a.getQuick(i)-meanA;
            final double y = b.getQuick(i)-meanB;
            va += x*x;
            vb += y*y;
            vab += x*y;
        }
        return vab / Math.sqrt(va*vb);
    }

}