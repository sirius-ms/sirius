package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * How well one set of MS/MS spectra agrees with another, matched by compound.
 * <p>
 * Shared by every comparison against a DDA measurement so that the numbers are comparable by construction rather
 * than by inspection: our own deconvolution at either stage, and the output of another tool read from an MGF, all
 * go through this same scoring. Two directions are measured for each pair of runs:
 * <ul>
 *     <li><b>confirmed</b> - of the peaks the run produced, how many the reference spectrum of the same compound
 *     also has. This is precision.</li>
 *     <li><b>recall</b> - of the reference's fragment peaks, how many the run found.</li>
 * </ul>
 * Both come with a <b>decoy</b> rate: the same lookup against the reference spectrum of a different compound of
 * similar precursor mass eluting far away. A fragment mass matches some peak of some spectrum by chance often
 * enough that neither rate can be read without it.
 * <p>
 * Peaks within {@link #PRECURSOR_REGION} of the precursor are excluded throughout. They are the surviving precursor
 * ion and its isotopes, present in every spectrum of that precursor by construction, so counting them would report
 * agreement that is not evidence of anything.
 */
final class SpectrumAgreement {

    /** for matching a fragment peak of one spectrum against the peaks of another */
    static final Deviation PEAK_DEVIATION = new Deviation(10, 0.002);

    /** for matching a feature of one run against the features of another */
    static final Deviation PRECURSOR_DEVIATION = new Deviation(10, 0.002);

    /** the same compound in two runs of the same sample on the same column, in seconds */
    static final double DEFAULT_RT_TOLERANCE = 20d;

    /** a decoy has to be far enough away that it cannot be the same compound, in seconds */
    static final double MIN_DECOY_RT_DISTANCE = 120d;

    /** the surviving precursor ion and its isotopes, which every spectrum of that precursor has anyway */
    static final double PRECURSOR_REGION = 1.5;

    /** below this share of the base peak a reference peak is not reliably a fragment, so recall is not asked about it */
    static final double MIN_RELATIVE_INTENSITY_FOR_RECALL = 0.01;

    private SpectrumAgreement() {
    }

    /** the header of the table {@link Agreement#describe} prints a row of */
    static String header() {
        return String.format("%-15s %8s %8s %8s %11s %10s %10s %8s", "", "features", "matched", "peaks",
                "confirmed", "by decoy", "weighted", "recall");
    }

    /**
     * Which reference compound each of a run's spectra belongs to.
     * <p>
     * Keyed by the index of the reference spectrum, which is what lets two runs be scored on the same set of
     * compounds. Without that, each run is measured on the compounds it happens to report, and a tool that only
     * attempts the easy ones would look better than one that also attempts the hard ones - the two numbers would
     * not be comparable at all.
     */
    static final class Matching {
        final String name;
        final int featureCount;
        final Int2ObjectMap<FeatureSpectrum> byReference = new Int2ObjectOpenHashMap<>();
        /** spectra of this run that matched a reference compound another of its spectra already claimed */
        int duplicates;

        private Matching(String name, int featureCount) {
            this.name = name;
            this.featureCount = featureCount;
        }

        IntSet compounds() {
            return byReference.keySet();
        }
    }

    /** matches every spectrum of the run to a reference compound, keeping the closest in retention time */
    static Matching match(Run run, Run reference, double rtTolerance) {
        final Matching matching = new Matching(run.name, run.featureCount);
        for (FeatureSpectrum ours : run.spectra) {
            final int index = reference.bestMatchIndex(ours, rtTolerance);
            if (index < 0) continue;
            final FeatureSpectrum previous = matching.byReference.get(index);
            if (previous != null) {
                ++matching.duplicates;
                final double reference_rt = reference.spectra.get(index).retentionTime;
                if (Math.abs(previous.retentionTime - reference_rt) <= Math.abs(ours.retentionTime - reference_rt)) {
                    continue;
                }
            }
            matching.byReference.put(index, ours);
        }
        return matching;
    }

    /** how well one run's fragment peaks agree with the reference run's spectra of the same compounds */
    static Agreement score(Run run, Run reference, double rtTolerance) {
        final Matching matching = match(run, reference, rtTolerance);
        return score(matching, reference, matching.compounds());
    }

    /**
     * The same, restricted to a given set of reference compounds.
     * <p>
     * A compound in the set that this run has no spectrum for still counts towards recall, with none of its
     * fragments found - that is what makes recall comparable over the union of two runs' compounds. It contributes
     * nothing to precision, which is a statement about the peaks a run did produce.
     */
    static Agreement score(Matching matching, Run reference, IntSet compounds) {
        final Agreement agreement = new Agreement(matching.name, matching.featureCount);
        for (int index : compounds) {
            final FeatureSpectrum match = reference.spectra.get(index);
            final FeatureSpectrum ours = matching.byReference.get(index);
            if (ours == null) {
                for (int k = 0; k < match.size(); ++k) {
                    if (match.intensity[k] < MIN_RELATIVE_INTENSITY_FOR_RECALL * match.basePeakIntensity) continue;
                    ++agreement.referencePeaks;
                }
                continue;
            }
            final FeatureSpectrum decoy = reference.decoy(ours);
            ++agreement.matchedFeatures;

            for (int k = 0; k < ours.size(); ++k) {
                final double mz = ours.mz[k];
                final double intensity = ours.intensity[k];
                ++agreement.peaks;
                agreement.intensity += intensity;
                if (match.contains(mz)) {
                    ++agreement.confirmed;
                    agreement.confirmedIntensity += intensity;
                }
                if (decoy != null) {
                    ++agreement.decoyPeaks;
                    if (decoy.contains(mz)) ++agreement.decoyConfirmed;
                }
            }
            // and the other direction: which of the reference fragments did we find at all
            for (int k = 0; k < match.size(); ++k) {
                if (match.intensity[k] < MIN_RELATIVE_INTENSITY_FOR_RECALL * match.basePeakIntensity) continue;
                ++agreement.referencePeaks;
                if (ours.contains(match.mz[k])) ++agreement.recovered;
            }
            // and what recall would be by chance: the same question asked of an unrelated compound's fragments.
            // Note that this has to search OUR spectrum for the decoy's peaks, not the decoy for the reference's -
            // the latter compares two reference spectra with each other and says nothing about the run being scored.
            if (decoy != null) {
                for (int k = 0; k < decoy.size(); ++k) {
                    if (decoy.intensity[k] < MIN_RELATIVE_INTENSITY_FOR_RECALL * decoy.basePeakIntensity) continue;
                    ++agreement.decoyReferencePeaks;
                    if (ours.contains(decoy.mz[k])) ++agreement.decoyRecovered;
                }
            }
        }
        return agreement;
    }

    /**
     * The fragment peaks of one feature: everything a comparison needs and nothing else, so that several whole runs
     * fit in memory at once. Masses are ascending, and peaks in the precursor region are already gone.
     */
    static final class FeatureSpectrum {
        final double precursorMz;
        final double retentionTime;
        final double[] mz;
        final double[] intensity;
        final double basePeakIntensity;

        FeatureSpectrum(double precursorMz, double retentionTime, double[] mz, double[] intensity) {
            this.precursorMz = precursorMz;
            this.retentionTime = retentionTime;
            this.mz = mz;
            this.intensity = intensity;
            double base = 0d;
            for (double i : intensity) base = Math.max(base, i);
            this.basePeakIntensity = base;
        }

        /** @param peaks mass ordered peaks; anything at or above the precursor region is dropped */
        static FeatureSpectrum of(double precursorMz, double retentionTime, SimpleSpectrum peaks) {
            final DoubleArrayList masses = new DoubleArrayList(peaks.size());
            final DoubleArrayList intensities = new DoubleArrayList(peaks.size());
            for (int k = 0; k < peaks.size(); ++k) {
                if (peaks.getMzAt(k) > precursorMz - PRECURSOR_REGION) continue;
                masses.add(peaks.getMzAt(k));
                intensities.add(peaks.getIntensityAt(k));
            }
            return new FeatureSpectrum(precursorMz, retentionTime, masses.toDoubleArray(),
                    intensities.toDoubleArray());
        }

        static FeatureSpectrum of(AlignedFeatures feature) {
            return of(feature.getAverageMass(), feature.getRetentionTime().getRetentionTimeInSeconds(),
                    feature.getMSData().orElseThrow().getMergedMSnSpectrum());
        }

        int size() {
            return mz.length;
        }

        /** whether some peak of this spectrum has the given mass, within {@link #PEAK_DEVIATION} */
        boolean contains(double queryMz) {
            final double allowed = PEAK_DEVIATION.absoluteFor(queryMz);
            int low = 0, high = mz.length - 1;
            while (low <= high) {
                final int mid = (low + high) >>> 1;
                if (mz[mid] < queryMz - allowed) low = mid + 1;
                else if (mz[mid] > queryMz + allowed) high = mid - 1;
                else return true;
            }
            return false;
        }
    }

    /** the spectra of one run, searchable by precursor mass and retention time */
    static final class Run {
        final String name;
        final int featureCount;
        final List<FeatureSpectrum> spectra;

        Run(String name, int featureCount, List<FeatureSpectrum> spectra) {
            this.name = name;
            this.featureCount = featureCount;
            this.spectra = spectra;
            this.spectra.sort(Comparator.comparingDouble(s -> s.precursorMz));
        }

        /**
         * Runs the files through the pipeline and keeps only the spectra. The features themselves are unreachable
         * when this returns, which is what makes several runs in one test JVM possible.
         */
        static Run of(List<Path> files, String name, Consumer<LCMSProcessing> configure, boolean refine)
                throws IOException {
            final PipelineRun run = PipelineRun.run(files, name, configure, refine);
            if (run.refinement != null) System.out.print(run.refinement);
            final List<FeatureSpectrum> spectra = new ArrayList<>();
            for (AlignedFeatures feature : run.featuresWithMsMs()) {
                final FeatureSpectrum spectrum = FeatureSpectrum.of(feature);
                if (spectrum.size() > 0) spectra.add(spectrum);
            }
            System.out.printf("%s: %d features, %d with fragment peaks, %.1f s%n", name, run.features.size(),
                    spectra.size(), run.processingSeconds + run.alignmentSeconds);
            return new Run(name, run.features.size(), spectra);
        }

        /** the mass matching spectrum closest in retention time, or null if none is within the tolerance */
        FeatureSpectrum bestMatch(FeatureSpectrum query, double rtTolerance) {
            final int index = bestMatchIndex(query, rtTolerance);
            return index < 0 ? null : spectra.get(index);
        }

        /** the same, as an index into {@link #spectra}, or -1; the index is how a compound is identified */
        int bestMatchIndex(FeatureSpectrum query, double rtTolerance) {
            final double allowed = PRECURSOR_DEVIATION.absoluteFor(query.precursorMz);
            int best = -1;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int k = lowerBound(query.precursorMz - allowed);
                 k < spectra.size() && spectra.get(k).precursorMz <= query.precursorMz + allowed; ++k) {
                final double distance = Math.abs(spectra.get(k).retentionTime - query.retentionTime);
                if (distance <= rtTolerance && distance < bestDistance) {
                    bestDistance = distance;
                    best = k;
                }
            }
            return best;
        }

        /**
         * A spectrum that cannot be the queried compound but covers a comparable mass range: the closest precursor
         * mass among the compounds eluting far away. Scoring against it measures how often a fragment mass finds a
         * peak by chance.
         */
        FeatureSpectrum decoy(FeatureSpectrum query) {
            // walking outwards from the query mass finds the closest eligible precursor without scanning the rest
            int left = lowerBound(query.precursorMz) - 1, right = left + 1;
            while (left >= 0 || right < spectra.size()) {
                final boolean takeLeft = right >= spectra.size() || (left >= 0
                        && query.precursorMz - spectra.get(left).precursorMz
                        <= spectra.get(right).precursorMz - query.precursorMz);
                final FeatureSpectrum candidate = takeLeft ? spectra.get(left--) : spectra.get(right++);
                if (Math.abs(candidate.retentionTime - query.retentionTime) >= MIN_DECOY_RT_DISTANCE) {
                    return candidate;
                }
            }
            return null;
        }

        /** index of the first spectrum whose precursor mass is not below the given one */
        private int lowerBound(double precursorMz) {
            int low = 0, high = spectra.size();
            while (low < high) {
                final int mid = (low + high) >>> 1;
                if (spectra.get(mid).precursorMz < precursorMz) low = mid + 1;
                else high = mid;
            }
            return low;
        }
    }

    static final class Agreement {
        private final String name;
        private final int features;
        int matchedFeatures, peaks, confirmed, decoyPeaks, decoyConfirmed;
        int referencePeaks, recovered, decoyReferencePeaks, decoyRecovered;
        double intensity, confirmedIntensity;

        Agreement(String name, int features) {
            this.name = name;
            this.features = features;
        }

        double confirmedRate() {
            return peaks == 0 ? 0d : confirmed / (double) peaks;
        }

        double decoyRate() {
            return decoyPeaks == 0 ? 0d : decoyConfirmed / (double) decoyPeaks;
        }

        double recallRate() {
            return referencePeaks == 0 ? 0d : recovered / (double) referencePeaks;
        }

        double decoyRecallRate() {
            return decoyReferencePeaks == 0 ? 0d : decoyRecovered / (double) decoyReferencePeaks;
        }

        String describe() {
            return String.format("%-15s %8d %8d %8d %9.1f %% %8.1f %% %8.1f %% %6.1f %%", name, features,
                    matchedFeatures, peaks, 100 * confirmedRate(), 100 * decoyRate(),
                    intensity == 0 ? 0d : 100 * confirmedIntensity / intensity, 100 * recallRate());
        }

        /** the same, with both rates corrected for what they would be by chance */
        String describeNet() {
            return String.format("%-15s %8d %8d %8d %9.1f %% %8.1f %% %8.1f %% %6.1f %%", name, features,
                    matchedFeatures, peaks, 100 * (confirmedRate() - decoyRate()), 100 * decoyRate(),
                    intensity == 0 ? 0d : 100 * confirmedIntensity / intensity,
                    100 * (recallRate() - decoyRecallRate()));
        }
    }
}
