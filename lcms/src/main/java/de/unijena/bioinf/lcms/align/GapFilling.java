package de.unijena.bioinf.lcms.align;

import com.google.common.collect.Range;
import de.unijena.bioinf.lcms.CorrelatedPeakDetector;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.lcms.MemoryFileStorage;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.peakshape.PeakShape;
import de.unijena.bioinf.lcms.quality.Quality;
import de.unijena.bioinf.model.lcms.ChromatographicPeak;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.GapFilledIon;
import de.unijena.bioinf.model.lcms.Scan;

import java.io.IOException;
import java.util.*;

public class GapFilling {

    public Cluster gapFilling(LCMSProccessingInstance instance, Cluster cluster, double rtError, double peakShapeError, boolean onlyGoodShapes) {

        for (ProcessedSample sample : instance.getSamples()) {
            if (sample.storage instanceof MemoryFileStorage)
                ((MemoryFileStorage) sample.storage).keepInMemory();
            final Set<ChromatographicPeak.Segment> segments = new HashSet<>();
            for (FragmentedIon ion : sample.ions)
                segments.add(ion.getSegment());
            for (FragmentedIon ion : sample.gapFilledIons)
                segments.add(ion.getSegment());
            for (FragmentedIon ion : sample.otherIons)
                segments.add(ion.getSegment());

            for (AlignedFeatures f : cluster.features) {
                // most abundant ion
                FragmentedIon mostAbundant = f.features.values().stream().filter(x->x.getClass().equals(FragmentedIon.class)).max(Comparator.comparingDouble(FragmentedIon::getIntensity)).get();
                if (onlyGoodShapes && mostAbundant.getPeakShape().getPeakShapeQuality().notBetterThan(Quality.BAD))
                    continue;
                if (onlyGoodShapes && mostAbundant.getIsotopes().size()<=1)
                    continue;
                if (f.features.containsKey(sample) && !(f.features.get(sample) instanceof GapFilledIon)) {
                    // no need for gap filling
                } else {
                    final double retentionTimeTolerance = rtError;

                    final double[] rets = f.features.entrySet().stream().filter(x->!(x instanceof GapFilledIon)).mapToDouble(e -> e.getKey().getRecalibratedRT(e.getValue().getRetentionTime())).toArray();
                    Arrays.sort(rets);
                    double lowest = rets[(int)(0.25*rets.length)] - retentionTimeTolerance;
                    double highest = rets[(int)(0.75*rets.length)] + retentionTimeTolerance;
                    final double middle = rets[(int)(rets.length*0.5)];
                    double avg = f.rt;
                    final Range<Double> tolerance = Range.closed(lowest,highest);
                    //System.out.println(String.valueOf(lowest/1000d) + " ... " + (highest/1000d) + " seconds tolerance");

                    ArrayList<Scan> scans = new ArrayList<>(sample.findScansByRecalibratedRT(Range.closed(lowest,highest)).values());
                    if (scans.isEmpty())
                        continue;

                    final HashSet<ChromatographicPeak> alreadyTried = new HashSet<>();
                    outerLoop:
                    for (final double tol : new double[]{4,2,1}) {
                        final double LOW = middle - (middle-lowest)/tol;
                        final double HIGH = middle + (highest-middle)/tol;
                        scans = new ArrayList<>(sample.findScansByRecalibratedRT(Range.closed(LOW,HIGH)).values());
                        if (scans.isEmpty())
                            continue;
                        final Optional<ChromatographicPeak> peak = sample.builder.detect(Range.closed(scans.get(0).getScanNumber(), scans.get(scans.size()-1).getScanNumber()), f.mass); // TODO: recalibrate mass, too?
                        if (peak.isPresent()) {
                            if (alreadyTried.contains(peak.get()))
                                continue ;
                            alreadyTried.add(peak.get());
                            for (ChromatographicPeak.Segment seg : peak.get().getSegments()) {
                                final double peakRt = sample.getRecalibratedRT(peak.get().getRetentionTimeAt(seg.getApexIndex()));
                                if (tolerance.contains(peakRt)) {
                                    if (segments.contains(seg))
                                        continue ; // we already know this ion
                                    final GapFilledIon pseudoIon = new GapFilledIon(peak.get(), seg, mostAbundant);
                                    // search for isotopes
                                    if (new CorrelatedPeakDetector().detectCorrelatedPeaks(sample, pseudoIon)) {

                                        if (onlyGoodShapes) {
                                            if (pseudoIon.getIsotopes().size()<=1)
                                                continue;
                                            final PeakShape shape = instance.fitPeakShape(sample, pseudoIon);
                                            if (shape.getPeakShapeQuality().notBetterThan(Quality.BAD))
                                                continue;
                                        }

                                        double avgError = 0d; int n=0;
                                        for (FragmentedIon ion : f.features.values()) {
                                            if (!(ion instanceof GapFilledIon)) {
                                                avgError += ion.comparePeakWidthSmallToLarge(pseudoIon);
                                                ++n;
                                            }
                                        }
                                        avgError /= n;

                                        if (avgError >= 4*peakShapeError) {
                                            System.err.println("REJECTED DUE TO PEAK SHAPE OF " + avgError);
                                            continue;
                                        }

                                        sample.gapFilledIons.add(pseudoIon);
                                        FragmentedIon ion = f.features.get(sample);
                                        if (ion==null) {
                                            f.features.put(sample, pseudoIon);
                                        } else {
                                            // we do not really know which one is correct. Hopefully, alignments will figure it out
                                            if ( Math.abs(ion.getRetentionTime() - middle) > Math.abs(pseudoIon.getRetentionTime() - middle)) {
                                                f.features.put(sample,pseudoIon);
                                            }
                                        }
                                        segments.add(seg);
                                        break outerLoop;
                                    }
                                }
                            }
                        }
                    }

                }
            }
            if (sample.storage instanceof MemoryFileStorage) {
                try {
                    ((MemoryFileStorage) sample.storage).backOnDisc();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return cluster;
    }

}
