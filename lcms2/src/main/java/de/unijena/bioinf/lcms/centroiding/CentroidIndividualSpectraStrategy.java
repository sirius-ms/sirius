package de.unijena.bioinf.lcms.centroiding;

import de.unijena.bioinf.ChemistryBase.algorithm.Quickselect;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.noise.NoiseStatistics;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;
import java.util.BitSet;

public class CentroidIndividualSpectraStrategy implements CentroidingStrategy
{
    @Override
    public void centroidMsScan(Scan scan) {
        SimpleSpectrum s = scan.getPeaks();
        if (s.size()<3) return;
        double noiseLevel = Quickselect.quickselectInplace(Spectrums.copyIntensities(s), 0, s.size(), (int)(s.size()*0.05))*3d;//Quickselect.quickselectInplace(Spectrums.copyIntensities(s), 0, s.size(), (int)(s.size()*0.9))/10d;
        SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        double lastDelta = Double.POSITIVE_INFINITY;
        for (int j=1; j < Math.min(s.size(), 20); ++j ) {
            lastDelta = Math.min(lastDelta, s.getMzAt(j)-s.getMzAt(j-1));
        }
        int from=0;
        for (int i=1; i < s.size(); ++i) {
            final double delta = s.getMzAt(i)-s.getMzAt(i-1);
            if (delta > 0.1 || delta > 2.5*lastDelta) {
               PersistenceHomologyPeakDetector persistenceHomologyPeakDetector = new PersistenceHomologyPeakDetector(s, from, i);
               while (persistenceHomologyPeakDetector.addNextPeak(noiseLevel, buffer, true)) {

               }
               from = i;
            } else {
                lastDelta=delta;
            }
        }
        if (from < s.size()-1) {
            PersistenceHomologyPeakDetector persistenceHomologyPeakDetector = new PersistenceHomologyPeakDetector(s, from, s.size());
            while (persistenceHomologyPeakDetector.addNextPeak(noiseLevel, buffer, true)) {

            }
        }
        scan.setCentroided(true);
        scan.setPeaks(new SimpleSpectrum(buffer));
    }

    @Override
    public void centroidMsMsScan(MSMSScan scan) {
        SimpleSpectrum s = scan.getPeaks();
        if (s.size()<3) return;
        final double noiseLevel;
        /*
        if (scan.getPeaks().size() >= 100){
            noiseLevel = Quickselect.quickselectInplace(Spectrums.copyIntensities(s), 0, s.size(), (int)Math.min(s.size()-100, (s.size()*0.05)))*3d;
        } else noiseLevel=0;
         */
        noiseLevel = Quickselect.quickselectInplace(Spectrums.copyIntensities(s), 0, s.size(), Math.max(0,(int)Math.min(s.size()-100, (s.size()*0.05))))*3d;
        SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        double lastDelta = Double.POSITIVE_INFINITY;
        for (int j=1; j < Math.min(s.size(), 20); ++j ) {
            lastDelta = Math.min(lastDelta, s.getMzAt(j)-s.getMzAt(j-1));
        }
        int from=0;
        for (int i=1; i < s.size(); ++i) {
            final double delta = s.getMzAt(i)-s.getMzAt(i-1);
            if (delta > 0.1 || delta > 2.5*lastDelta) {
                PersistenceHomologyPeakDetector persistenceHomologyPeakDetector = new PersistenceHomologyPeakDetector(s, from, i);
                while (persistenceHomologyPeakDetector.addNextPeak(noiseLevel, buffer, true)) {

                }
                from = i;
            } else {
                lastDelta=delta;
            }
        }
        if (from < s.size()-1) {
            PersistenceHomologyPeakDetector persistenceHomologyPeakDetector = new PersistenceHomologyPeakDetector(s, from, s.size());
            while (persistenceHomologyPeakDetector.addNextPeak(noiseLevel, buffer, true)) {

            }
        }
        scan.setCentroided(true);
        scan.setPeaks(new SimpleSpectrum(buffer));
    }

    private static class PersistenceHomologyPeakDetector {
        private SimpleSpectrum spectrum;
        private int from, to;
        private Integer[] sortedPeaks;
        private BitSet usedPeaks;
        int current;
        DoubleArrayList apexes = new DoubleArrayList();

        private static boolean USE_DYNAMIC_FILTER=true;


        public PersistenceHomologyPeakDetector(SimpleSpectrum spectrum, int from, int to) {
            this.spectrum = spectrum;
            this.from = from;
            this.to = to;
            sortedPeaks = new Integer[to-from];
            for (int k=0; k < sortedPeaks.length; ++k) sortedPeaks[k] = k;
            Arrays.sort(sortedPeaks, (i,j)->-Double.compare(spectrum.getIntensityAt(i+from), spectrum.getIntensityAt(j+from)));
            usedPeaks = new BitSet(sortedPeaks.length);
            current=0;
        }

        public boolean addNextPeak(double minimumIntensity, SimpleMutableSpectrum buffer, boolean sumInt) {
            SimpleMutableSpectrum maybePeaks = new SimpleMutableSpectrum();
            while (current < sortedPeaks.length){
                int nextPeak = sortedPeaks[current++] + from;
                double pkint = spectrum.getIntensityAt(nextPeak);
                if (pkint < minimumIntensity) return false;
                if (usedPeaks.get(nextPeak-from)) continue;
                // extend left and right
                int a, b;
                double l = pkint;
                double lowestValley = pkint;
                for (a = nextPeak - 1; a >= from; --a) {
                    double i = spectrum.getIntensityAt(a);
                    lowestValley = Math.min(i,lowestValley);
                    if (i > l) {
                        if (a-1 >= from) {
                            double ip = spectrum.getIntensityAt(a-1);
                            if (ip > l || l/i < 0.9) {
                                break;
                            }
                        } else break;
                    }
                    l = i;
                }
                ++a;
                l = pkint;
                double leftEdge = spectrum.getIntensityAt(a);
                for (b = nextPeak + 1; b < to; ++b) {
                    double i = spectrum.getIntensityAt(b);
                    lowestValley = Math.min(i,lowestValley);
                    if (i > l) {
                        if (b+1 < to) {
                            double ip = spectrum.getIntensityAt(b+1);
                            if (ip > l || l/i < 0.9) {
                                break;
                            }
                        } else break;
                    }
                    l = i;
                }
                --b;

                /*
                if (b-a < 1) { // at least two peaks
                    if (allowForPairs && pkint > 2*minimumIntensity) {
                        // take the larger peak
                        buffer.addPeak(spectrum.getMzAt(nextPeak), pkint);
                        usedPeaks.set(a);usedPeaks.set(b);
                        continue;
                    } else continue;
                };

                 */

                // centroid
                double intsum = 0d;
                double mzavg = 0d;
                for (int j = a; j <= b; ++j) {
                    usedPeaks.set(j-from);
                }

                double persistence = lowestValley/pkint;
                final boolean notAGoodPeak = (persistence > 0.75);

                // centroiding
                {
                    final float[] deltaLeft = new float[nextPeak-a];
                    final float[] deltaRight = new float[b-nextPeak];
                    for (int j=a; j < nextPeak; ++j) {
                        deltaLeft[j-a] = (float)(spectrum.getIntensityAt(j)/spectrum.getIntensityAt(nextPeak));
                    }
                    for (int j=nextPeak+1; j <= b; ++j) {
                        deltaRight[j-(nextPeak+1)] = (float)(spectrum.getIntensityAt(j)/spectrum.getIntensityAt(nextPeak));
                    }
                    int bestI=0, bestJ=0;double mindiff=Double.POSITIVE_INFINITY;
                    if (USE_DYNAMIC_FILTER) {
                        for (int i = 0; i < deltaLeft.length; ++i) {
                            if (deltaLeft[i] < 0.33 || deltaLeft[i] > 0.66) continue;
                            for (int j = 0; j < deltaRight.length; ++j) {
                                if (deltaRight[j] < 0.33 || deltaRight[j] > 0.66) continue;
                                double diff = Math.abs(deltaLeft[i] - deltaRight[j]);
                                if (diff < mindiff) {
                                    mindiff = diff;
                                    bestI = i;
                                    bestJ = j;
                                }
                            }
                        }
                    } else {
                        // use fwhm
                    }
                    if (Double.isInfinite(mindiff)) {
                        // just use fwhm
                        for (bestI=deltaLeft.length-1; bestI >=0 ; --bestI) {
                            if (deltaLeft[bestI]<0.5) break;
                        }
                        ++bestI;

                        for (bestJ=0; bestJ < deltaRight.length; ++bestJ) {
                            if (deltaRight[bestJ]<0.5) break;
                        }
                        --bestJ;
                    }
                    // finally: centroid
                    for (int i=bestI+a; i <= bestJ+(nextPeak+1); ++i) {
                        final double intens = spectrum.getIntensityAt(i)/pkint;
                        intsum += intens;
                        mzavg += spectrum.getMzAt(i)*intens;
                    }
                }

                mzavg /= intsum;
                final double peakIntensity = (sumInt ? pkint*intsum : pkint);
                if (notAGoodPeak) {
                    // check the distance to closest apex
                    double dist = Double.POSITIVE_INFINITY;
                    for (double d : apexes) dist = Math.min(dist, Math.abs(mzavg-d));
                    if (dist >= 0.02) {
                        buffer.addPeak(mzavg, peakIntensity);
                        apexes.add(mzavg);
                    } else {
                        continue;
                    }
                } else {
                    buffer.addPeak(mzavg, peakIntensity);
                }
                apexes.add(mzavg);
                return true;
            }
            return false;
        }
    }
}
