package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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

    /**
     * How many of a spectrum's strongest peaks are checked separately.
     * <p>
     * The intensity weighted agreement can only differ from the per peak agreement if the two disagree about which
     * peaks are large, so this asks the question directly: of the peaks a reader would notice first, how many belong.
     */
    static final int STRONGEST_PEAKS = 5;

    private SpectrumAgreement() {
    }

    /** the header of the table {@link Agreement#describe} prints a row of */
    static String header() {
        return String.format("%-15s %8s %8s %8s %11s %10s %10s %8s %8s %8s %8s %8s", "", "features", "matched", "peaks",
                "confirmed", "by decoy", "weighted", "recall", "cosine", "rank", "top5", "top5int", "intact");
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

        /**
         * The same matching with only the spectra a predicate keeps.
         * <p>
         * Used to ask what a run would be worth if a quality flag were believed: the compounds whose spectrum the
         * flag rejects become compounds the run has no spectrum for, which is exactly how they would be read. The
         * feature count stays the run's, since filtering spectra does not unfind features.
         */
        Matching restrictedTo(String name, java.util.function.Predicate<FeatureSpectrum> keep) {
            final Matching restricted = new Matching(name, featureCount);
            for (Int2ObjectMap.Entry<FeatureSpectrum> entry : byReference.int2ObjectEntrySet()) {
                if (keep.test(entry.getValue())) restricted.byReference.put(entry.getIntKey(), entry.getValue());
            }
            return restricted;
        }
    }

    /**
     * The quality grades {@code CheckMs2Quality} gives a data independent spectrum, by the ion current it carries
     * relative to the run's MS2 noise level.
     * <p>
     * Duplicated from the check rather than called into it, because the check reports into a whole feature quality
     * report and this needs the grade of one spectrum. The boundaries have to stay in step with
     * {@code CheckMs2Quality.DIA_ION_COUNT_OVER_NOISE}.
     */
    enum DiaGrade {
        LOWEST, BAD, DECENT, GOOD, UNKNOWN;

        static DiaGrade of(FeatureSpectrum spectrum, double ms2NoiseLevel) {
            if (!Double.isFinite(spectrum.totalIonCount) || !(ms2NoiseLevel > 0)) return UNKNOWN;
            final double overNoise = spectrum.totalIonCount / ms2NoiseLevel;
            if (overNoise < 5d) return LOWEST;
            if (overNoise < 20d) return BAD;
            if (overNoise < 150d) return DECENT;
            return GOOD;
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
            agreement.addIntensityAgreement(ours, match);
            agreement.addConfirmedShare(ours, match);
            agreement.addStrongestPeaks(ours, match);
            agreement.addSurvivingPrecursors(ours, match, reference);
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
     * Whether the fragments a run failed to report for a compound went to one of its neighbours instead.
     * <p>
     * Assigning a shared fragment to the wrong one of two co-eluting precursors leaves the loser with a thin spectrum
     * of whatever the winner did not take, and no measure taken on the loser's own spectrum can distinguish that from
     * fragments that were never traced at all. The difference is whether the peaks are somewhere else in the same
     * isolation window. So recall is asked twice: of the spectrum matched to the compound, and of every spectrum the
     * run has for a precursor within one window of it that elutes at the same time.
     * <p>
     * The union of many spectra contains more masses and therefore finds more of anything, so the same two questions
     * are asked of an unrelated compound's fragments. The comparison is between the two gaps, not the two recalls.
     */
    static String describeNeighbourRecall(Run run, Run reference, IntSet compounds, double rtTolerance,
                                          double windowMz) {
        // the run's spectra by precursor mass, so a neighbourhood is a range
        final List<FeatureSpectrum> byMass = new ArrayList<>(run.spectra);
        byMass.sort(Comparator.comparingDouble(s -> s.precursorMz));
        long referencePeaks = 0, foundMatched = 0, foundNear = 0;
        long decoyPeaks = 0, decoyMatched = 0, decoyNear = 0;
        int compoundsCounted = 0, neighbours = 0;
        for (int index : compounds) {
            final FeatureSpectrum match = reference.spectra.get(index);
            final FeatureSpectrum decoy = reference.decoy(match);
            // everything the run has in this isolation window at this time, the matched spectrum included
            final List<FeatureSpectrum> here = new ArrayList<>();
            for (FeatureSpectrum ours : byMass) {
                if (ours.precursorMz < match.precursorMz - windowMz) continue;
                if (ours.precursorMz > match.precursorMz + windowMz) break;
                if (Math.abs(ours.retentionTime - match.retentionTime) <= rtTolerance) here.add(ours);
            }
            if (here.isEmpty()) continue;
            FeatureSpectrum matched = null;
            double closest = Double.POSITIVE_INFINITY;
            for (FeatureSpectrum ours : here) {
                final double allowed = PRECURSOR_DEVIATION.absoluteFor(match.precursorMz);
                if (Math.abs(ours.precursorMz - match.precursorMz) > allowed) continue;
                final double distance = Math.abs(ours.retentionTime - match.retentionTime);
                if (distance < closest) {
                    closest = distance;
                    matched = ours;
                }
            }
            if (matched == null) continue;
            ++compoundsCounted;
            neighbours += here.size();
            for (int k = 0; k < match.size(); ++k) {
                if (match.intensity[k] < MIN_RELATIVE_INTENSITY_FOR_RECALL * match.basePeakIntensity) continue;
                ++referencePeaks;
                if (matched.contains(match.mz[k])) ++foundMatched;
                for (FeatureSpectrum ours : here) {
                    if (ours.contains(match.mz[k])) {
                        ++foundNear;
                        break;
                    }
                }
            }
            if (decoy == null) continue;
            for (int k = 0; k < decoy.size(); ++k) {
                if (decoy.intensity[k] < MIN_RELATIVE_INTENSITY_FOR_RECALL * decoy.basePeakIntensity) continue;
                ++decoyPeaks;
                if (matched.contains(decoy.mz[k])) ++decoyMatched;
                for (FeatureSpectrum ours : here) {
                    if (ours.contains(decoy.mz[k])) {
                        ++decoyNear;
                        break;
                    }
                }
            }
        }
        final double matchedRate = referencePeaks == 0 ? 0d : 100d * foundMatched / referencePeaks;
        final double nearRate = referencePeaks == 0 ? 0d : 100d * foundNear / referencePeaks;
        final double matchedDecoy = decoyPeaks == 0 ? 0d : 100d * decoyMatched / decoyPeaks;
        final double nearDecoy = decoyPeaks == 0 ? 0d : 100d * decoyNear / decoyPeaks;
        return String.format("%-15s %4d compounds, %4.1f spectra within %.0f m/z: recall %5.1f %% matched, "
                        + "%5.1f %% anywhere near; by decoy %4.1f %% and %4.1f %%; net %5.1f %% against %5.1f %%%n",
                run.name, compoundsCounted, neighbours / (double) Math.max(1, compoundsCounted), windowMz,
                matchedRate, nearRate, matchedDecoy, nearDecoy,
                matchedRate - matchedDecoy, nearRate - nearDecoy);
    }

    /** how many of each spectrum's strongest peaks the top peak agreement compares */
    static final int DEFAULT_TOP_PEAKS = 12;

    /** the shares of the reference's strongest peaks a spectrum is counted as having reached */
    private static final double[] TOP_PEAK_STEPS = {0.25, 0.5, 0.75, 1.0};

    /**
     * How many compounds a run gets substantially right, judged only by which masses its strongest peaks are.
     * <p>
     * Every other measure here is either a rate over all peaks, which says nothing about how the peaks are
     * distributed over compounds, or weighted by intensity, which is the least reliable quantity in a DIA experiment:
     * a fragment's height depends on how its window was shared, and two spectra listing the same masses can differ
     * several fold in any one of them. Taking the strongest peaks of <em>both</em> spectra and asking only whether
     * the masses agree removes intensity from the comparison except as an ordering, which is the part of it that
     * survives.
     * <p>
     * Counted per compound and reported as a distribution rather than a mean, because "half the compounds are three
     * quarters right" and "every compound is a third right" are different results with the same average, and only
     * the first is a usable identification.
     * <p>
     * The decoy is the same question asked of an unrelated compound of similar precursor mass. Twelve masses against
     * twelve have a real chance of meeting, and without the control a quarter of them agreeing would look like a
     * result.
     */
    static String describeTopPeaks(Matching matching, Run reference, IntSet compounds, int topPeaks) {
        final int[] reached = new int[TOP_PEAK_STEPS.length];
        final int[] reachedByDecoy = new int[TOP_PEAK_STEPS.length];
        double shareSum = 0d, decoyShareSum = 0d;
        int spectra = 0, decoys = 0;
        for (int index : compounds) {
            final FeatureSpectrum ours = matching.byReference.get(index);
            if (ours == null || ours.size() == 0) continue;
            final FeatureSpectrum match = reference.spectra.get(index);
            if (match.size() == 0) continue;
            ++spectra;
            final double share = topPeakOverlap(ours, match, topPeaks);
            shareSum += share;
            for (int step = 0; step < TOP_PEAK_STEPS.length; ++step) {
                if (share >= TOP_PEAK_STEPS[step] - 1e-9) ++reached[step];
            }
            final FeatureSpectrum decoy = reference.decoy(ours);
            if (decoy == null || decoy.size() == 0) continue;
            ++decoys;
            final double decoyShare = topPeakOverlap(ours, decoy, topPeaks);
            decoyShareSum += decoyShare;
            for (int step = 0; step < TOP_PEAK_STEPS.length; ++step) {
                if (decoyShare >= TOP_PEAK_STEPS[step] - 1e-9) ++reachedByDecoy[step];
            }
        }
        if (spectra == 0) return String.format("%-15s no spectra%n", matching.name);

        final StringBuilder out = new StringBuilder(String.format("%-17s %8d spectra, mean %5.1f %% (decoy %4.1f %%)",
                matching.name, spectra, 100 * shareSum / spectra,
                decoys == 0 ? Double.NaN : 100 * decoyShareSum / decoys));
        for (int step = 0; step < TOP_PEAK_STEPS.length; ++step) {
            out.append(String.format("   >=%3.0f %%: %5d (%4.1f %%, decoy %4.1f %%)", 100 * TOP_PEAK_STEPS[step],
                    reached[step], 100d * reached[step] / spectra,
                    decoys == 0 ? Double.NaN : 100d * reachedByDecoy[step] / decoys));
        }
        return out.append('\n').toString();
    }

    /**
     * @return the fraction of the reference's strongest peaks that are among the run's strongest, both lists cut to
     * the same length. Divided by how many the reference has to offer, so a reference spectrum with fewer than
     * {@code topPeaks} peaks is not counted as partly missed for peaks it never had.
     */
    private static double topPeakOverlap(FeatureSpectrum ours, FeatureSpectrum match, int topPeaks) {
        final double[] mine = strongestMasses(ours, topPeaks);
        final double[] theirs = strongestMasses(match, topPeaks);
        if (theirs.length == 0) return 0d;
        int shared = 0;
        for (double mass : theirs) {
            for (double candidate : mine) {
                if (PEAK_DEVIATION.inErrorWindow(mass, candidate)) {
                    ++shared;
                    break;
                }
            }
        }
        return shared / (double) theirs.length;
    }

    /** the masses of the strongest peaks, at most the given number of them */
    private static double[] strongestMasses(FeatureSpectrum spectrum, int topPeaks) {
        final Integer[] byIntensity = new Integer[spectrum.size()];
        for (int k = 0; k < byIntensity.length; ++k) byIntensity[k] = k;
        java.util.Arrays.sort(byIntensity, (a, b) -> Double.compare(spectrum.intensity[b], spectrum.intensity[a]));
        final int n = Math.min(topPeaks, byIntensity.length);
        final double[] masses = new double[n];
        for (int k = 0; k < n; ++k) masses[k] = spectrum.mz[byIntensity[k]];
        return masses;
    }

    /**
     * Whether a run can tell its good spectra from its bad ones without being shown the answer.
     * <p>
     * A run that reports twice as many compounds as another and gets a quarter of them substantially right is only
     * useful if the right quarter can be recognised. If it can, the extra spectra cost a reader nothing - they arrive
     * ranked, and the ones below the line can be ignored or filtered. If it cannot, they are noise indistinguishable
     * from data, and reporting them is worse than not reporting them.
     * <p>
     * So the same top peak agreement, split by two quantities a run knows about itself: how many peaks it put in the
     * spectrum, and how intense the precursor was. The second is what another tool's minimum feature intensity is a
     * crude form of, and if the agreement rises steeply with it then that threshold is not arbitrary after all.
     */
    static String describeQuality(Matching matching, Run reference, IntSet compounds, int topPeaks) {
        final int[] peakBounds = {1, 5, 10, 20, 40};
        final double[] intensityBounds = {0, 1e4, 1e5, 1e6, 1e7};
        final int[] byPeaksCount = new int[peakBounds.length], byPeaksGood = new int[peakBounds.length];
        // and, for the same bins, whether the peaks a spectrum does have are right at all. A thin spectrum cannot
        // cover much of the reference's top twelve however good it is - two peaks reach a sixth of it at best - so
        // the coverage above says as much about the arithmetic as about the spectrum. This does not.
        final long[] peaksIn = new long[peakBounds.length], confirmedIn = new long[peakBounds.length];
        final long[] decoyPeaksIn = new long[peakBounds.length], decoyConfirmedIn = new long[peakBounds.length];
        final int[] byIntensityCount = new int[intensityBounds.length];
        final int[] byIntensityGood = new int[intensityBounds.length];
        int withoutIntensity = 0;
        for (int index : compounds) {
            final FeatureSpectrum ours = matching.byReference.get(index);
            if (ours == null || ours.size() == 0) continue;
            final FeatureSpectrum match = reference.spectra.get(index);
            if (match.size() == 0) continue;
            final boolean good = topPeakOverlap(ours, match, topPeaks) >= 0.25 - 1e-9;

            int bin = 0;
            while (bin + 1 < peakBounds.length && ours.size() >= peakBounds[bin + 1]) ++bin;
            ++byPeaksCount[bin];
            if (good) ++byPeaksGood[bin];
            final FeatureSpectrum decoy = reference.decoy(ours);
            for (int k = 0; k < ours.size(); ++k) {
                ++peaksIn[bin];
                if (match.contains(ours.mz[k])) ++confirmedIn[bin];
                if (decoy != null) {
                    ++decoyPeaksIn[bin];
                    if (decoy.contains(ours.mz[k])) ++decoyConfirmedIn[bin];
                }
            }

            if (Double.isNaN(ours.precursorIntensity)) {
                ++withoutIntensity;
                continue;
            }
            int intensityBin = 0;
            while (intensityBin + 1 < intensityBounds.length
                    && ours.precursorIntensity >= intensityBounds[intensityBin + 1]) ++intensityBin;
            ++byIntensityCount[intensityBin];
            if (good) ++byIntensityGood[intensityBin];
        }

        final StringBuilder out = new StringBuilder(String.format("%-17s %-14s %8s %10s %12s %10s %9s %8s%n",
                matching.name, "condition", "spectra", "reaching", "25 % of top", "peaks", "confirmed", "decoy"));
        for (int bin = 0; bin < peakBounds.length; ++bin) {
            if (byPeaksCount[bin] == 0) continue;
            out.append(String.format("%-17s %-14s %8d %10d %11.1f %% %10d %7.1f %% %6.1f %%%n", "",
                    bin + 1 == peakBounds.length ? peakBounds[bin] + "+ peaks"
                            : peakBounds[bin] + "-" + (peakBounds[bin + 1] - 1) + " peaks",
                    byPeaksCount[bin], byPeaksGood[bin], 100d * byPeaksGood[bin] / byPeaksCount[bin],
                    peaksIn[bin], peaksIn[bin] == 0 ? 0d : 100d * confirmedIn[bin] / peaksIn[bin],
                    decoyPeaksIn[bin] == 0 ? 0d : 100d * decoyConfirmedIn[bin] / decoyPeaksIn[bin]));
        }
        if (withoutIntensity > 0) {
            out.append(String.format("%-17s %d spectra without a precursor intensity to bin by%n", "",
                    withoutIntensity));
        }
        for (int bin = 0; bin < intensityBounds.length; ++bin) {
            if (byIntensityCount[bin] == 0) continue;
            out.append(String.format("%-17s %-14s %8d %10d %9.1f %%%n", "",
                    bin + 1 == intensityBounds.length ? String.format("%.0e+", intensityBounds[bin])
                            : String.format("%.0e-%.0e", intensityBounds[bin], intensityBounds[bin + 1]),
                    byIntensityCount[bin], byIntensityGood[bin],
                    100d * byIntensityGood[bin] / byIntensityCount[bin]));
        }
        return out.toString();
    }

    /** a spectrum agreeing with the reference less than this is treated as uninformative */
    static final double NONSENSE_COSINE = 0.2;

    /** a spectrum confirming less than this share of its own peaks is treated as uninformative */
    static final double NONSENSE_CONFIRMED = 0.2;

    /** the signals a run knows about its own spectra, in the order the report prints them */
    private static final String[] QUALITY_SIGNALS = {"ions / noise", "base peak / noise", "claimed share",
            "ions / precursor", "precursor intensity", "peak count", "mean log p-value",
            "persisted TIC / noise", "best 5 log p sum"};

    /**
     * Which of the quantities a run knows about a spectrum predict that the spectrum is uninformative.
     * <p>
     * The point of a quality flag is that it can be computed without the answer, so every signal here is measured on
     * the run's own data and none of them looks at the reference. The reference supplies only the label. Each signal
     * is reported in quartiles of its own distribution rather than against a threshold, so signals on different
     * scales are comparable and no bound has to be invented before knowing whether the signal is worth anything.
     * <p>
     * Two labels, because they fail differently. The <b>cosine</b> against the reference spectrum is what a user
     * ultimately cares about, but it is taken over the union of both peak lists, so the reference's unmatched peaks
     * enter as zeros and a thin spectrum is capped low however right its few peaks are - which makes any signal
     * correlated with peak count look better than it is. The <b>confirmed share</b>, the fraction of the run's own
     * peaks that the reference has, is neutral with respect to size: three peaks all confirmed scores one, exactly
     * as forty all confirmed would. A signal that leads under both is really a signal.
     * <p>
     * The records are per sample and per channel while the labels are per compound, so they are first aggregated to
     * the feature: currents sum, because a compound measured in three samples really did produce three times the
     * ions, and the noise level averages. That is a choice rather than a fact, and worth revisiting if a signal looks
     * good only after it.
     */
    static String describeQualitySignals(Matching matching, Run run, Run reference, IntSet reachable,
                                         double rtTolerance) {
        if (run.quality.isEmpty()) return String.format("%-19s no quality records%n", matching.name);
        final List<de.unijena.bioinf.lcms.utils.Tracker.DiaSpectrumQuality> byMass = new ArrayList<>(run.quality);
        byMass.sort(Comparator.comparingDouble(
                de.unijena.bioinf.lcms.utils.Tracker.DiaSpectrumQuality::precursorMz));
        final double[] masses = new double[byMass.size()];
        for (int k = 0; k < masses.length; ++k) masses[k] = byMass.get(k).precursorMz();

        final List<double[]> signals = new ArrayList<>();
        final DoubleArrayList cosines = new DoubleArrayList(), confirmedShares = new DoubleArrayList();
        final DoubleArrayList decoyShares = new DoubleArrayList();
        for (int index : reachable) {
            final FeatureSpectrum ours = matching.byReference.get(index);
            final FeatureSpectrum match = reference.spectra.get(index);
            if (ours == null || ours.size() == 0 || match.size() == 0) continue;
            double attributed = 0d, measured = 0d, base = 0d, noise = 0d, precursor = 0d, logP = 0d, bestLogP = 0d;
            int records = 0, withLogP = 0, withBest = 0;
            final double allowed = PRECURSOR_DEVIATION.absoluteFor(ours.precursorMz);
            for (int k = lowerBound(masses, ours.precursorMz - allowed);
                 k < byMass.size() && byMass.get(k).precursorMz() <= ours.precursorMz + allowed; ++k) {
                final var record = byMass.get(k);
                if (Math.abs(record.retentionTime() - ours.retentionTime) > rtTolerance) continue;
                attributed += record.attributedIonCount();
                measured += record.measuredIonCount();
                base = Math.max(base, record.basePeak());
                noise += record.noiseLevel();
                precursor = Math.max(precursor, record.precursorIntensity());
                if (Double.isFinite(record.meanLogPValue())) {
                    logP += record.meanLogPValue();
                    ++withLogP;
                }
                if (Double.isFinite(record.bestLogPValueSum())) {
                    bestLogP += record.bestLogPValueSum();
                    ++withBest;
                }
                ++records;
            }
            if (records == 0) continue;
            noise /= records;
            signals.add(new double[]{
                    noise > 0 ? attributed / noise : Double.NaN,
                    noise > 0 ? base / noise : Double.NaN,
                    measured > 0 ? attributed / measured : Double.NaN,
                    precursor > 0 ? attributed / precursor : Double.NaN,
                    precursor > 0 ? precursor : Double.NaN,
                    ours.size(),
                    // negated, so that every signal points the same way: larger is better
                    withLogP == 0 ? Double.NaN : -logP / withLogP,
                    // exactly what the quality check can read from a written project
                    Double.isFinite(ours.totalIonCount) && run.ms2NoiseLevel > 0
                            ? ours.totalIonCount / run.ms2NoiseLevel : Double.NaN,
                    // negated as well, so larger is better throughout
                    withBest == 0 ? Double.NaN : -bestLogP / withBest});
            final double cosine = spectralCosine(ours, match);
            cosines.add(Double.isFinite(cosine) ? cosine : 0d);
            int confirmed = 0, byChance = 0;
            final FeatureSpectrum decoy = reference.decoy(ours);
            for (int k = 0; k < ours.size(); ++k) {
                if (match.contains(ours.mz[k])) ++confirmed;
                if (decoy != null && decoy.contains(ours.mz[k])) ++byChance;
            }
            confirmedShares.add(confirmed / (double) ours.size());
            decoyShares.add(decoy == null ? Double.NaN : byChance / (double) ours.size());
        }
        if (signals.isEmpty()) return String.format("%-19s no records matched a compound%n", matching.name);

        final StringBuilder out = new StringBuilder(String.format("%n%s: %d spectra with quality records%n",
                matching.name, signals.size()));
        out.append(quartileTable(signals, cosines.toDoubleArray(), decoyShares.toDoubleArray(),
                "cosine against the reference spectrum", NONSENSE_COSINE, "cos"));
        out.append(quartileTable(signals, confirmedShares.toDoubleArray(), decoyShares.toDoubleArray(),
                "share of our own peaks the reference confirms", NONSENSE_CONFIRMED, "conf"));
        return out.toString();
    }

    /**
     * One block: every signal against one label, in quartiles of the signal.
     *
     * @param label      per spectrum, the quantity being predicted
     * @param decoy      per spectrum, the same quantity against an unrelated compound, for the chance baseline
     * @param bad        below this the spectrum counts as uninformative
     */
    private static String quartileTable(List<double[]> signals, double[] label, double[] decoy, String labelName,
                                        double bad, String shortName) {
        double overall = 0d, chance = 0d;
        int badCount = 0, chanceCount = 0;
        for (int k = 0; k < label.length; ++k) {
            overall += label[k];
            if (label[k] < bad) ++badCount;
            if (Double.isFinite(decoy[k])) {
                chance += decoy[k];
                ++chanceCount;
            }
        }
        final StringBuilder out = new StringBuilder(String.format(
                "  label: %s. %.1f %% of spectra are below %.2f; mean %.3f, mean against an unrelated compound "
                        + "%.3f%n  Q1 is the quarter of spectra with the LOWEST value of that row's signal, Q4 the "
                        + "highest; each row sorted on its own%n", labelName, 100d * badCount / label.length, bad,
                overall / label.length, chanceCount == 0 ? Double.NaN : chance / chanceCount));
        for (int signal = 0; signal < QUALITY_SIGNALS.length; ++signal) {
            final int which = signal;
            final IntArrayList usable = new IntArrayList();
            for (int k = 0; k < signals.size(); ++k) if (Double.isFinite(signals.get(k)[which])) usable.add(k);
            if (usable.size() < 8) continue;
            final Integer[] order = new Integer[usable.size()];
            for (int k = 0; k < order.length; ++k) order[k] = usable.getInt(k);
            java.util.Arrays.sort(order, Comparator.comparingDouble(k -> signals.get(k)[which]));
            out.append(String.format("    %-20s", QUALITY_SIGNALS[signal]));
            for (int quartile = 0; quartile < 4; ++quartile) {
                final int from = quartile * order.length / 4, to = (quartile + 1) * order.length / 4;
                int badHere = 0;
                double mean = 0d;
                for (int k = from; k < to; ++k) {
                    if (label[order[k]] < bad) ++badHere;
                    mean += label[order[k]];
                }
                out.append(String.format("  Q%d(<%.3g) %5.1f %% bad, %s %.3f", quartile + 1,
                        to > from ? signals.get(order[to - 1])[which] : Double.NaN,
                        to > from ? 100d * badHere / (to - from) : Double.NaN, shortName,
                        to > from ? mean / (to - from) : Double.NaN));
            }
            out.append('\n');
        }
        return out.toString();
    }

    /** index of the first mass at or above the bound */
    private static int lowerBound(double[] sortedMasses, double bound) {
        int low = 0, high = sortedMasses.length;
        while (low < high) {
            final int mid = (low + high) >>> 1;
            if (sortedMasses[mid] < bound) low = mid + 1;
            else high = mid;
        }
        return low;
    }

    /** how many of the best matches the fixed budget metric averages over */
    static final int DEFAULT_COSINE_BUDGET = 500;

    /**
     * The spectral cosine between a run's spectrum and the reference's, over the union of their peaks.
     * <p>
     * Peaks either side has and the other does not enter as zeros, so a spectrum is penalised both for missing the
     * reference's fragments and for inventing its own. The same computation {@link Agreement#addIntensityAgreement}
     * accumulates, extracted so that it can be asked about one pair.
     */
    static double spectralCosine(FeatureSpectrum ours, FeatureSpectrum match) {
        final DoubleArrayList mine = new DoubleArrayList(), theirs = new DoubleArrayList();
        final boolean[] used = new boolean[match.size()];
        for (int k = 0; k < ours.size(); ++k) {
            final int at = match.indexOf(ours.mz[k]);
            mine.add(ours.intensity[k]);
            if (at >= 0 && !used[at]) {
                used[at] = true;
                theirs.add(match.intensity[at]);
            } else {
                theirs.add(0d);
            }
        }
        for (int k = 0; k < match.size(); ++k) {
            if (used[k]) continue;
            mine.add(0d);
            theirs.add(match.intensity[k]);
        }
        return Agreement.cosineOf(mine.toDoubleArray(), theirs.toDoubleArray());
    }

    /**
     * The reference compounds a DIA acquisition could have found at all.
     * <p>
     * A compound whose precursor mass falls in none of the isolation windows was never fragmented in the DIA run, so
     * no method could have produced a spectrum for it and charging one for missing it measures the acquisition rather
     * than the method.
     */
    static IntSet reachable(Run reference, Run dia) {
        final IntSet eligible = new IntOpenHashSet();
        for (int index = 0; index < reference.spectra.size(); ++index) {
            if (dia.couldIsolate(reference.spectra.get(index).precursorMz)) eligible.add(index);
        }
        return eligible;
    }

    /**
     * How well a run reconstructs the reference's spectra, over a fixed set of compounds and a fixed budget of
     * answers.
     * <p>
     * Two numbers, because a single one cannot separate coverage from quality:
     * <ul>
     *     <li><b>over all reachable compounds</b>, with a compound the run has no spectrum for counted as a cosine of
     *     zero. This charges a method for everything it did not attempt, so producing many mediocre spectra helps
     *     only to the extent that they are actually similar.</li>
     *     <li><b>over the best {@code budget} answers</b>, the rest of the budget counted as zero. Every method gets
     *     the same number of slots, so one that reports more compounds has more chances to fill them - but only with
     *     matches that are good, since a poor one contributes almost nothing to the average.</li>
     * </ul>
     * A decoy column for each, from the same spectra scored against an unrelated compound, because a cosine of a tenth
     * is roughly what two unrelated spectra of similar mass produce.
     */
    static String describeReconstruction(Matching matching, Run reference, IntSet reachable, int budget) {
        final DoubleArrayList cosines = new DoubleArrayList(), decoyCosines = new DoubleArrayList();
        for (int index : reachable) {
            final FeatureSpectrum ours = matching.byReference.get(index);
            final FeatureSpectrum match = reference.spectra.get(index);
            if (ours == null || ours.size() == 0 || match.size() == 0) {
                cosines.add(0d);
                continue;
            }
            final double cosine = spectralCosine(ours, match);
            cosines.add(Double.isFinite(cosine) ? cosine : 0d);
            final FeatureSpectrum decoy = reference.decoy(ours);
            if (decoy != null && decoy.size() > 0) {
                final double against = spectralCosine(ours, decoy);
                decoyCosines.add(Double.isFinite(against) ? against : 0d);
            }
        }
        final double[] sorted = cosines.toDoubleArray();
        java.util.Arrays.sort(sorted);
        double all = 0d, best = 0d;
        for (double cosine : sorted) all += cosine;
        for (int k = 0; k < Math.min(budget, sorted.length); ++k) best += sorted[sorted.length - 1 - k];
        final double[] decoySorted = decoyCosines.toDoubleArray();
        java.util.Arrays.sort(decoySorted);
        double decoyBest = 0d;
        for (int k = 0; k < Math.min(budget, decoySorted.length); ++k) {
            decoyBest += decoySorted[decoySorted.length - 1 - k];
        }
        int above = 0;
        for (double cosine : sorted) if (cosine >= 0.5) ++above;
        return String.format("%-19s %7d matched of %d reachable   mean %6.4f   best %d mean %6.4f "
                        + "(decoy %6.4f)   cosine >= 0.5: %5d%n", matching.name,
                matching.compounds().size(), reachable.size(), sorted.length == 0 ? 0d : all / sorted.length,
                budget, best / budget, decoyBest / budget, above);
    }

    /** how many spectra of one run may compete for the same reference compound before the bin is called that */
    private static final int[] COMPETITION_BINS = {1, 2, 3, 6};

    /**
     * How many spectra of a run land on the same reference compound, and whether the one being scored is the best of
     * them.
     * <p>
     * A run that reports several features per compound has several spectra within the matching window, and the
     * harness scores the one closest in retention time. If the compound's fragments were spread over those features,
     * the scored spectrum is thin and wrong while a sibling holds the answer, and a difference between two runs is
     * then about how many features they make of one elution rather than about their assignments. The best of the
     * candidates is an oracle and not a result; it is here to say whether the answer is in the output at all. The
     * union is what merging the features would give, without merging duplicate peaks, so its peak count is an upper
     * bound.
     */
    static String describeCompetition(Run run, Run reference, IntSet compounds, double rtTolerance) {
        final Int2ObjectMap<List<FeatureSpectrum>> candidates = new Int2ObjectOpenHashMap<>();
        for (FeatureSpectrum ours : run.spectra) {
            final int index = reference.bestMatchIndex(ours, rtTolerance);
            if (index >= 0 && compounds.contains(index)) {
                candidates.computeIfAbsent(index, key -> new ArrayList<>()).add(ours);
            }
        }

        final int bins = COMPETITION_BINS.length;
        final int[] compoundsIn = new int[bins];
        final double[] scoredShare = new double[bins], bestShare = new double[bins], unionShare = new double[bins];
        final double[] scoredPeaks = new double[bins];
        for (int index : compounds) {
            final List<FeatureSpectrum> here = candidates.get(index);
            if (here == null || here.isEmpty()) continue;
            final FeatureSpectrum match = reference.spectra.get(index);
            int bin = 0;
            while (bin + 1 < bins && here.size() >= COMPETITION_BINS[bin + 1]) ++bin;
            // the one the harness scores, and the best and the union of all of them
            double closestDistance = Double.POSITIVE_INFINITY, scored = 0d, best = 0d;
            double unionTotal = 0d, unionConfirmed = 0d, peaks = 0d;
            for (FeatureSpectrum ours : here) {
                double total = 0d, confirmed = 0d;
                for (int k = 0; k < ours.size(); ++k) {
                    total += ours.intensity[k];
                    if (match.contains(ours.mz[k])) confirmed += ours.intensity[k];
                }
                if (total <= 0) continue;
                unionTotal += total;
                unionConfirmed += confirmed;
                best = Math.max(best, confirmed / total);
                final double distance = Math.abs(ours.retentionTime - match.retentionTime);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    scored = confirmed / total;
                    peaks = ours.size();
                }
            }
            if (unionTotal <= 0) continue;
            ++compoundsIn[bin];
            scoredShare[bin] += scored;
            bestShare[bin] += best;
            unionShare[bin] += unionConfirmed / unionTotal;
            scoredPeaks[bin] += peaks;
        }

        final StringBuilder out = new StringBuilder(String.format("%-15s %10s %10s %9s %9s %9s%n",
                run.name, "candidates", "compounds", "scored", "best", "union"));
        for (int bin = 0; bin < bins; ++bin) {
            if (compoundsIn[bin] == 0) continue;
            final String range = bin + 1 == bins ? COMPETITION_BINS[bin] + "+"
                    : COMPETITION_BINS[bin] == COMPETITION_BINS[bin + 1] - 1 ? String.valueOf(COMPETITION_BINS[bin])
                    : COMPETITION_BINS[bin] + "-" + (COMPETITION_BINS[bin + 1] - 1);
            out.append(String.format("%-15s %10s %10d %7.1f %% %7.1f %% %7.1f %%   (%.1f peaks scored)%n", "", range,
                    compoundsIn[bin], 100 * scoredShare[bin] / compoundsIn[bin],
                    100 * bestShare[bin] / compoundsIn[bin], 100 * unionShare[bin] / compoundsIn[bin],
                    scoredPeaks[bin] / compoundsIn[bin]));
        }
        return out.toString();
    }

    /**
     * How often a run gives two co-eluting precursors of different mass the same spectrum, and whether the reference
     * agrees that they should have it.
     * <p>
     * Two precursors whose chromatograms cannot be told apart offer every fragment exactly the same evidence, so any
     * rule that judges a pair on its own evidence must give them the same peaks. The result is two features with
     * different precursor masses and one spectrum, which for a formula annotation is worse than no spectrum: it is a
     * confident wrong answer rather than a missing one. No agreement measure can see this, because a duplicate on a
     * precursor mass the reference never reported is scored against nothing.
     * <p>
     * The reference decides whether a duplicate is a fabrication or chemistry. In-source fragments, isomers and
     * homologues genuinely do co-elute and genuinely do share most of their fragments, and there the reference
     * spectra of the two compounds are themselves near-identical. So the pairs are binned by how similar
     * <em>our</em> two spectra are, and each bin reports how similar the <em>reference's</em> two spectra are. A bin
     * where we agree and the reference does not is duplication we invented.
     *
     * @param windowMz how far apart two precursors may be and still have been co-isolated
     */
    static String describeDuplication(Matching matching, Run reference, IntSet compounds, double rtTolerance,
                                      double windowMz) {
        // our spectra for the compounds in question, ordered by precursor mass so a window is a range
        final List<int[]> indexed = new ArrayList<>();
        final List<FeatureSpectrum> ours = new ArrayList<>();
        for (int index : compounds) {
            final FeatureSpectrum spectrum = matching.byReference.get(index);
            if (spectrum != null && spectrum.size() > 0) {
                ours.add(spectrum);
                indexed.add(new int[]{index});
            }
        }
        final Integer[] order = new Integer[ours.size()];
        for (int k = 0; k < order.length; ++k) order[k] = k;
        java.util.Arrays.sort(order, Comparator.comparingDouble(k -> ours.get(k).precursorMz));

        final double[] bounds = {0.9, 0.7, 0d};
        final int[] pairs = new int[bounds.length];
        final double[] referenceCosineSum = new double[bounds.length];
        final int[] referenceAgrees = new int[bounds.length];
        for (int a = 0; a < order.length; ++a) {
            final FeatureSpectrum left = ours.get(order[a]);
            for (int b = a + 1; b < order.length; ++b) {
                final FeatureSpectrum right = ours.get(order[b]);
                if (right.precursorMz - left.precursorMz > windowMz) break;
                // a different compound, co-eluting, close enough in mass to have shared an isolation window
                if (Math.abs(right.precursorMz - left.precursorMz)
                        <= PRECURSOR_DEVIATION.absoluteFor(left.precursorMz)) continue;
                if (Math.abs(right.retentionTime - left.retentionTime) > rtTolerance) continue;
                final double mine = cosine(left, right);
                if (!Double.isFinite(mine)) continue;
                int bin = bounds.length - 1;
                for (int k = 0; k < bounds.length; ++k) {
                    if (mine >= bounds[k]) {
                        bin = k;
                        break;
                    }
                }
                ++pairs[bin];
                final double theirs = cosine(reference.spectra.get(indexed.get(order[a])[0]),
                        reference.spectra.get(indexed.get(order[b])[0]));
                if (Double.isFinite(theirs)) {
                    referenceCosineSum[bin] += theirs;
                    if (theirs >= 0.9) ++referenceAgrees[bin];
                }
            }
        }

        final StringBuilder out = new StringBuilder(String.format("%-15s %14s %8s %12s %14s%n", matching.name,
                "our cosine", "pairs", "their cosine", "theirs >= 0.9"));
        final String[] labels = {"0.9 and up", "0.7 to 0.9", "below 0.7"};
        for (int bin = 0; bin < bounds.length; ++bin) {
            if (pairs[bin] == 0) continue;
            out.append(String.format("%-15s %14s %8d %12.3f %11.1f %%%n", "", labels[bin], pairs[bin],
                    referenceCosineSum[bin] / pairs[bin], 100d * referenceAgrees[bin] / pairs[bin]));
        }
        return out.toString();
    }

    /** plain intensity cosine over the peaks the two spectra share, which is all a duplication check needs */
    private static double cosine(FeatureSpectrum left, FeatureSpectrum right) {
        double dot = 0d, normLeft = 0d, normRight = 0d;
        for (int k = 0; k < left.size(); ++k) {
            normLeft += left.intensity[k] * left.intensity[k];
            final int at = right.indexOf(left.mz[k]);
            if (at >= 0) dot += left.intensity[k] * right.intensity[at];
        }
        for (int k = 0; k < right.size(); ++k) normRight += right.intensity[k] * right.intensity[k];
        return normLeft > 0 && normRight > 0 ? dot / Math.sqrt(normLeft * normRight) : Double.NaN;
    }

    /** lower bounds of the per compound intensity agreement bins */
    private static final double[] CONFIRMED_SHARE_BINS = {0d, 1e-9, 0.1, 0.25, 0.5, 0.75};

    /** how many of a run's own spectra a mass has to appear in before the bin is called that */
    private static final int[] UBIQUITY_BINS = {1, 2, 4, 11, 31, 101};

    /**
     * How a run's peaks are confirmed as a function of how many of its <em>own</em> spectra carry the same mass.
     * <p>
     * A background mass - a contaminant eluting across the whole run, or electronic noise - is available to every
     * precursor in every isolation window, so if such masses are what a run misassigns, they show up as masses it
     * reports for very many compounds at once. A real fragment is shared by a few related compounds at most. This
     * needs no reference to compute, only the run's own spectra, and the reference then says which bins are right.
     * <p>
     * Counted over the given compounds only, so two runs are comparable: a run's own bin for a mass depends on how
     * many spectra it reports, and restricting both to the same compounds removes that.
     */
    static String describeUbiquity(Matching matching, Run reference, IntSet compounds) {
        // every peak mass of the run, sorted, so the number of spectra carrying a mass is a range count
        int total = 0;
        for (int index : compounds) {
            final FeatureSpectrum ours = matching.byReference.get(index);
            if (ours != null) total += ours.size();
        }
        final double[] allMasses = new double[total];
        int at = 0;
        for (int index : compounds) {
            final FeatureSpectrum ours = matching.byReference.get(index);
            if (ours == null) continue;
            System.arraycopy(ours.mz, 0, allMasses, at, ours.size());
            at += ours.size();
        }
        java.util.Arrays.sort(allMasses);

        final int bins = UBIQUITY_BINS.length;
        final int[] peaksIn = new int[bins], confirmedIn = new int[bins];
        final double[] shareIn = new double[bins], confirmedShareIn = new double[bins];
        double allShare = 0d;
        for (int index : compounds) {
            final FeatureSpectrum ours = matching.byReference.get(index);
            if (ours == null) continue;
            final FeatureSpectrum match = reference.spectra.get(index);
            double spectrumTotal = 0d;
            for (int k = 0; k < ours.size(); ++k) spectrumTotal += ours.intensity[k];
            if (spectrumTotal <= 0) continue;
            for (int k = 0; k < ours.size(); ++k) {
                final int ubiquity = countNear(allMasses, ours.mz[k]);
                int bin = 0;
                while (bin + 1 < bins && ubiquity >= UBIQUITY_BINS[bin + 1]) ++bin;
                // each compound contributes one unit of intensity, as in meanConfirmedShare
                final double share = ours.intensity[k] / spectrumTotal;
                ++peaksIn[bin];
                shareIn[bin] += share;
                allShare += share;
                if (match.contains(ours.mz[k])) {
                    ++confirmedIn[bin];
                    confirmedShareIn[bin] += share;
                }
            }
        }

        final StringBuilder out = new StringBuilder(String.format("%-15s %12s %8s %10s %10s%n",
                matching.name, "in spectra", "peaks", "of current", "confirmed"));
        for (int bin = 0; bin < bins; ++bin) {
            if (peaksIn[bin] == 0) continue;
            final String range = bin + 1 == bins ? UBIQUITY_BINS[bin] + "+"
                    : UBIQUITY_BINS[bin] == UBIQUITY_BINS[bin + 1] - 1 ? String.valueOf(UBIQUITY_BINS[bin])
                    : UBIQUITY_BINS[bin] + "-" + (UBIQUITY_BINS[bin + 1] - 1);
            out.append(String.format("%-15s %12s %8d %8.1f %% %8.1f %%%n", "", range, peaksIn[bin],
                    allShare == 0 ? 0d : 100 * shareIn[bin] / allShare,
                    100 * confirmedIn[bin] / (double) peaksIn[bin]));
        }
        return out.toString();
    }

    /** how many of the sorted masses are the same mass as the query, within {@link #PEAK_DEVIATION} */
    private static int countNear(double[] sortedMasses, double queryMz) {
        final double allowed = PEAK_DEVIATION.absoluteFor(queryMz);
        return upperBound(sortedMasses, queryMz + allowed) - upperBound(sortedMasses, queryMz - allowed);
    }

    /** index of the first mass strictly above the bound */
    private static int upperBound(double[] sortedMasses, double bound) {
        int low = 0, high = sortedMasses.length;
        while (low < high) {
            final int mid = (low + high) >>> 1;
            if (sortedMasses[mid] <= bound) low = mid + 1;
            else high = mid;
        }
        return low;
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
        /** the precursor's own apex intensity, NaN where the source does not report one */
        double precursorIntensity = Double.NaN;
        /** summed over the feature's MSn spectra, as {@code CheckMs2Quality} would read it; NaN if not recorded */
        double totalIonCount = Double.NaN;
        /** true if any of the feature's MSn spectra was acquired data independently */
        boolean dataIndependent;

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
            final FeatureSpectrum spectrum = of(feature.getAverageMass(),
                    feature.getRetentionTime().getRetentionTimeInSeconds(),
                    feature.getMSData().orElseThrow().getMergedMSnSpectrum());
            spectrum.precursorIntensity = feature.getApexIntensity() == null ? Double.NaN
                    : feature.getApexIntensity();
            final var msn = feature.getMSData().orElseThrow().getMsnSpectra();
            if (msn != null && !msn.isEmpty()) {
                double total = 0d;
                boolean any = false;
                for (var spec : msn) {
                    if (spec.getTotalIonCount() != null) {
                        total += spec.getTotalIonCount();
                        any = true;
                    }
                    spectrum.dataIndependent |= Boolean.TRUE.equals(spec.getDataIndependent());
                }
                if (any) spectrum.totalIonCount = total;
            }
            return spectrum;
        }

        int size() {
            return mz.length;
        }

        /** whether some peak of this spectrum has the given mass, within {@link #PEAK_DEVIATION} */
        boolean contains(double queryMz) {
            return indexOf(queryMz) >= 0;
        }

        /** @return the index of a peak of this mass, or -1; the intensity is what the weighting needs */
        int indexOf(double queryMz) {
            final double allowed = PEAK_DEVIATION.absoluteFor(queryMz);
            int low = 0, high = mz.length - 1;
            while (low <= high) {
                final int mid = (low + high) >>> 1;
                if (mz[mid] < queryMz - allowed) low = mid + 1;
                else if (mz[mid] > queryMz + allowed) high = mid - 1;
                else return mid;
            }
            return -1;
        }
    }

    /** the spectra of one run, searchable by precursor mass and retention time */
    static final class Run {
        final String name;
        final int featureCount;
        final List<FeatureSpectrum> spectra;
        /**
         * The isolation windows this run's acquisition used, as alternating lower and upper bounds, or null where the
         * acquisition is unknown - a DDA measurement, or spectra read from a file.
         */
        double[] isolationWindows;
        /** what each written spectrum was made of, when the run was asked to collect it */
        List<de.unijena.bioinf.lcms.utils.Tracker.DiaSpectrumQuality> quality = List.of();
        /** the MS2 noise level the quality checks divide by, so a diagnostic computes exactly what they will */
        double ms2NoiseLevel = Double.NaN;

        Run(String name, int featureCount, List<FeatureSpectrum> spectra) {
            this.name = name;
            this.featureCount = featureCount;
            this.spectra = spectra;
            this.spectra.sort(Comparator.comparingDouble(s -> s.precursorMz));
        }

        /**
         * Whether this acquisition could have isolated the given mass at all.
         * <p>
         * A compound outside every isolation window was never fragmented, so no method could have found it and no
         * comparison should charge one for missing it. True when the windows are unknown, since then nothing can be
         * excluded on this ground.
         */
        boolean couldIsolate(double precursorMz) {
            if (isolationWindows == null) return true;
            for (int k = 0; k + 1 < isolationWindows.length; k += 2) {
                if (precursorMz >= isolationWindows[k] && precursorMz <= isolationWindows[k + 1]) return true;
            }
            return false;
        }

        /**
         * Runs the files through the pipeline and keeps only the spectra. The features themselves are unreachable
         * when this returns, which is what makes several runs in one test JVM possible.
         */
        static Run of(List<Path> files, String name, Consumer<LCMSProcessing> configure, boolean refine)
                throws IOException {
            return of(files, name, configure, refine, false);
        }

        /**
         * @param collectQuality installs a tracker that records what every written spectrum was made of. Replaces any
         *                       tracker {@code configure} set, so the two cannot be asked for at once.
         */
        static Run of(List<Path> files, String name, Consumer<LCMSProcessing> configure, boolean refine,
                      boolean collectQuality) throws IOException {
            final QualityCollector collector = collectQuality ? new QualityCollector() : null;
            final Consumer<LCMSProcessing> configured = collectQuality
                    ? processing -> {
                        if (configure != null) configure.accept(processing);
                        processing.setTracker(collector);
                    }
                    : configure;
            final Run run = build(files, name, configured, refine);
            if (collector != null) run.quality = collector.records;
            return run;
        }

        /** collects {@link de.unijena.bioinf.lcms.utils.Tracker.DiaSpectrumQuality} and nothing else */
        private static final class QualityCollector extends de.unijena.bioinf.lcms.utils.Tracker.NOOP {
            private final List<de.unijena.bioinf.lcms.utils.Tracker.DiaSpectrumQuality> records = new ArrayList<>();

            @Override
            public boolean tracksDia() {
                return true;
            }

            @Override
            public void diaSpectrumQuality(de.unijena.bioinf.lcms.trace.ProcessedSample sample, String channel,
                                           DiaSpectrumQuality quality) {
                records.add(quality);
            }
        }

        private static Run build(List<Path> files, String name, Consumer<LCMSProcessing> configure, boolean refine)
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
            final Run result = new Run(name, run.features.size(), spectra);
            result.isolationWindows = isolationWindowsOf(run);
            result.ms2NoiseLevel = run.ms2NoiseLevel;
            return result;
        }

        /** the windows every channel of the reconstructed acquisition covers, or null if there was no DIA scheme */
        private static double[] isolationWindowsOf(PipelineRun run) {
            final DoubleArrayList bounds = new DoubleArrayList();
            for (DiaReport report : run.diaReports) {
                if (report == null || report.index() == null) continue;
                for (DiaChannel channel : report.index().getChannels()) {
                    for (int w = 0; w < channel.numberOfWindows(); ++w) {
                        bounds.add(channel.windowLowerBound(w));
                        bounds.add(channel.windowUpperBound(w));
                    }
                }
            }
            return bounds.isEmpty() ? null : bounds.toDoubleArray();
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

        /** the compounds of this run that elute at the same time within the given mass range, for a dump */
        List<String> compoundsNear(double precursorMz, double retentionTime, double rtTolerance, double mzRange) {
            final List<String> found = new ArrayList<>();
            for (int k = lowerBound(precursorMz - mzRange);
                 k < spectra.size() && spectra.get(k).precursorMz <= precursorMz + mzRange; ++k) {
                final FeatureSpectrum other = spectra.get(k);
                if (Math.abs(other.precursorMz - precursorMz) < 1e-6) continue;
                if (Math.abs(other.retentionTime - retentionTime) > rtTolerance) continue;
                found.add(String.format(java.util.Locale.US, "%.4f (base %.0f)", other.precursorMz,
                        other.basePeakIntensity));
            }
            return found;
        }

        /** whether some compound of this run has this precursor mass and elutes within the tolerance */
        boolean hasPrecursorNear(double precursorMz, double retentionTime, double rtTolerance) {
            final double allowed = PRECURSOR_DEVIATION.absoluteFor(precursorMz);
            for (int k = lowerBound(precursorMz - allowed);
                 k < spectra.size() && spectra.get(k).precursorMz <= precursorMz + allowed; ++k) {
                if (Math.abs(spectra.get(k).retentionTime - retentionTime) <= rtTolerance) return true;
            }
            return false;
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
        /** per compound, so a spectrum with many peaks does not outvote the rest */
        private double cosineSum, rankSum;
        private int cosineCount, rankCount;
        /** of the strongest peaks of each spectrum, how many the reference confirms */
        int strongestPeaks, strongestConfirmed;
        /** how much of a spectrum's intensity its strongest peaks carry, which is what the weighting is sensitive to */
        private double concentrationSum;
        private int concentrationCount;
        /** the intensity weighted agreement asked per compound instead of over the pooled ion current */
        private double confirmedShareSum;
        private int confirmedShareCount;
        private final int[] spectraWithShare = new int[CONFIRMED_SHARE_BINS.length];
        private final long[] peaksWithShare = new long[CONFIRMED_SHARE_BINS.length];
        int strongestUnconfirmed, strongestUnconfirmedArePrecursors;

        /**
         * How much our intensities agree with the reference's, which is a different question from which peaks we
         * found. Two measures, because they fail differently: the cosine says whether the intensity <em>weighted</em>
         * spectra match at all, and the rank correlation over the peaks both spectra have says whether our ordering
         * of them is right even where the magnitudes are not.
         */
        void addIntensityAgreement(FeatureSpectrum ours, FeatureSpectrum reference) {
            final DoubleArrayList mine = new DoubleArrayList(), theirs = new DoubleArrayList();
            final DoubleArrayList sharedMine = new DoubleArrayList(), sharedTheirs = new DoubleArrayList();
            final boolean[] used = new boolean[reference.size()];
            for (int k = 0; k < ours.size(); ++k) {
                final int match = reference.indexOf(ours.mz[k]);
                mine.add(ours.intensity[k]);
                if (match >= 0 && !used[match]) {
                    used[match] = true;
                    theirs.add(reference.intensity[match]);
                    sharedMine.add(ours.intensity[k]);
                    sharedTheirs.add(reference.intensity[match]);
                } else {
                    theirs.add(0d);
                }
            }
            // the reference's peaks we do not have count against the cosine as well
            for (int k = 0; k < reference.size(); ++k) {
                if (used[k]) continue;
                mine.add(0d);
                theirs.add(reference.intensity[k]);
            }
            final double cosine = cosineOf(mine.toDoubleArray(), theirs.toDoubleArray());
            if (Double.isFinite(cosine)) {
                cosineSum += cosine;
                ++cosineCount;
            }
            if (sharedMine.size() >= 3) {
                final double rank = de.unijena.bioinf.ChemistryBase.math.Statistics.spearman(
                        sharedMine.toDoubleArray(), sharedTheirs.toDoubleArray(), 0d);
                if (Double.isFinite(rank)) {
                    rankSum += rank;
                    ++rankCount;
                }
            }
        }

        /** the strongest peaks of our spectrum, and whether the reference has them */
        void addStrongestPeaks(FeatureSpectrum ours, FeatureSpectrum reference) {
            final Integer[] byIntensity = new Integer[ours.size()];
            for (int k = 0; k < byIntensity.length; ++k) byIntensity[k] = k;
            java.util.Arrays.sort(byIntensity, (a, b) -> Double.compare(ours.intensity[b], ours.intensity[a]));
            double top = 0d, all = 0d;
            for (int k = 0; k < byIntensity.length; ++k) {
                all += ours.intensity[byIntensity[k]];
                if (k < STRONGEST_PEAKS) {
                    top += ours.intensity[byIntensity[k]];
                    ++strongestPeaks;
                    if (reference.contains(ours.mz[byIntensity[k]])) ++strongestConfirmed;
                }
            }
            if (all > 0) {
                concentrationSum += top / all;
                ++concentrationCount;
            }
        }

        /**
         * The same as the intensity weighted agreement, but with every compound weighted equally.
         * <p>
         * The weighted agreement sums raw intensities over all compounds, so it answers "of all the ion current this
         * run reports, how much is confirmed" and the most intense compounds decide it. Two runs also read their
         * intensities off different scales. Normalising each spectrum by its own total first asks the same question
         * per compound and then averages, which is the form that says whether a difference is systematic or sits in
         * a handful of very large spectra.
         */
        void addConfirmedShare(FeatureSpectrum ours, FeatureSpectrum match) {
            double total = 0d, confirmedHere = 0d;
            for (int k = 0; k < ours.size(); ++k) {
                total += ours.intensity[k];
                if (match.contains(ours.mz[k])) confirmedHere += ours.intensity[k];
            }
            if (total > 0) {
                final double share = confirmedHere / total;
                confirmedShareSum += share;
                ++confirmedShareCount;
                int bin = 0;
                while (bin + 1 < CONFIRMED_SHARE_BINS.length && share >= CONFIRMED_SHARE_BINS[bin + 1]) ++bin;
                ++spectraWithShare[bin];
                peaksWithShare[bin] += ours.size();
            }
        }

        /**
         * How the per compound intensity agreement is distributed, with the average peak count of each bin.
         * <p>
         * A mean of forty per cent is two very different situations. Either most spectra are partly right, and what
         * is missing is a better estimate of the intensities, or a large minority are entirely wrong and the rest are
         * good, and then the work is to recognise the wrong ones rather than to weigh their peaks more carefully.
         */
        String describeShareDistribution() {
            final StringBuilder out = new StringBuilder(String.format("    %-15s", name));
            for (int bin = 0; bin < CONFIRMED_SHARE_BINS.length; ++bin) {
                out.append(String.format(" %6.1f %%", 100 * spectraWithShare[bin] / (double) confirmedShareCount));
            }
            out.append("\n").append(String.format("    %-15s", "  peaks each"));
            for (int bin = 0; bin < CONFIRMED_SHARE_BINS.length; ++bin) {
                out.append(String.format(" %8.1f", spectraWithShare[bin] == 0 ? Double.NaN
                        : peaksWithShare[bin] / (double) spectraWithShare[bin]));
            }
            return out.toString();
        }

        double meanConfirmedShare() {
            return confirmedShareCount == 0 ? Double.NaN : confirmedShareSum / confirmedShareCount;
        }

        String name() {
            return name;
        }

        /**
         * Of the strongest peaks the reference does <em>not</em> confirm, how many are the surviving precursor ion of
         * another compound that elutes at the same time.
         * <p>
         * A DIA scan holds the intact ions of everything co-isolated with the compound, not only its fragments, and
         * the most intense of those is often more intense than any real fragment. Only the compound's own precursor
         * region is excluded, so a neighbour's intact ion is free to become the base peak of its spectrum - which
         * would explain an intensity weighted agreement far below the per peak one.
         */
        void addSurvivingPrecursors(FeatureSpectrum ours, FeatureSpectrum match, Run reference) {
            final Integer[] byIntensity = new Integer[ours.size()];
            for (int k = 0; k < byIntensity.length; ++k) byIntensity[k] = k;
            java.util.Arrays.sort(byIntensity, (a, b) -> Double.compare(ours.intensity[b], ours.intensity[a]));
            for (int k = 0; k < Math.min(STRONGEST_PEAKS, byIntensity.length); ++k) {
                final int peak = byIntensity[k];
                if (match.contains(ours.mz[peak])) continue;
                ++strongestUnconfirmed;
                // is this mass the precursor of some other compound eluting here? the reference knows every
                // compound it saw, so a mass matching one of their precursors is an intact ion, not a fragment
                if (reference.hasPrecursorNear(ours.mz[peak], ours.retentionTime, DEFAULT_RT_TOLERANCE)) {
                    ++strongestUnconfirmedArePrecursors;
                }
            }
        }

        double survivingPrecursorRate() {
            return strongestUnconfirmed == 0 ? 0d
                    : strongestUnconfirmedArePrecursors / (double) strongestUnconfirmed;
        }

        /**
         * The share of a spectrum's intensity carried by its strongest peaks, averaged over compounds. The intensity
         * weighted agreement cannot be high unless the intensity is concentrated somewhere, so this says whether a
         * difference in that agreement is about which peaks are large or about how unequal the peaks are at all.
         */
        double meanConcentration() {
            return concentrationCount == 0 ? Double.NaN : concentrationSum / concentrationCount;
        }

        double strongestRate() {
            return strongestPeaks == 0 ? 0d : strongestConfirmed / (double) strongestPeaks;
        }

        static double cosineOf(double[] left, double[] right) {
            double dot = 0d, normLeft = 0d, normRight = 0d;
            for (int k = 0; k < left.length; ++k) {
                dot += left[k] * right[k];
                normLeft += left[k] * left[k];
                normRight += right[k] * right[k];
            }
            return normLeft > 0 && normRight > 0 ? dot / Math.sqrt(normLeft * normRight) : Double.NaN;
        }

        double meanCosine() {
            return cosineCount == 0 ? Double.NaN : cosineSum / cosineCount;
        }

        double meanRankCorrelation() {
            return rankCount == 0 ? Double.NaN : rankSum / rankCount;
        }

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
            return String.format("%-15s %8d %8d %8d %9.1f %% %8.1f %% %8.1f %% %6.1f %% %8.3f %8.3f %7.1f %% %7.1f %% %7.1f %%", name, features,
                    matchedFeatures, peaks, 100 * confirmedRate(), 100 * decoyRate(),
                    intensity == 0 ? 0d : 100 * confirmedIntensity / intensity, 100 * recallRate(),
                    meanCosine(), meanRankCorrelation(), 100 * strongestRate(), 100 * meanConcentration(), 100 * survivingPrecursorRate());
        }

        /** the same, with both rates corrected for what they would be by chance */
        String describeNet() {
            return String.format("%-15s %8d %8d %8d %9.1f %% %8.1f %% %8.1f %% %6.1f %% %8.3f %8.3f %7.1f %% %7.1f %% %7.1f %%", name, features,
                    matchedFeatures, peaks, 100 * (confirmedRate() - decoyRate()), 100 * decoyRate(),
                    intensity == 0 ? 0d : 100 * confirmedIntensity / intensity,
                    100 * (recallRate() - decoyRecallRate()), meanCosine(), meanRankCorrelation(),
                    100 * strongestRate(), 100 * meanConcentration(), 100 * survivingPrecursorRate());
        }
    }
}
