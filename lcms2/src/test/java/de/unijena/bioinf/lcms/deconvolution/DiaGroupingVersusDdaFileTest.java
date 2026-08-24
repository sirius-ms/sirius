package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.utils.Tracker;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.file;
import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.files;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * What the grouping strategy would cost, measured against the compounds a DDA measurement of the same sample found.
 * <p>
 * Grouping gives the fragments of a set of inseparable precursors to its most intense member and no spectrum to the
 * rest, which is what a data dependent acquisition does when it picks one precursor out of an isolation window. The
 * question is not whether that is defensible - it is - but what it costs, and that is decided entirely by which
 * member is the most intense one. A group whose only real compound is its representative costs nothing. A group where
 * the compound the reference found sits second by intensity loses its spectrum altogether.
 * <p>
 * So for every group: does it contain a compound the reference measured, is that compound the most intense member,
 * and if not, where does it rank and how far below the top is it. Asked twice, once against every reference compound
 * and once against only those another tool also reported, because the second set is the one the tool comparison has
 * been using and it is a different, easier population.
 * <pre>
 * SIRIUS_DDA_TEST_FILE=/path/DDA_1.mzML SIRIUS_DIA_TEST_FILE=/path/fixed/SWATH_1.mzML \
 *   [SIRIUS_MZMINE_MGF=/path/sirius.mgf] \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*DiaGroupingVersusDdaFileTest*'
 * </pre>
 */
public class DiaGroupingVersusDdaFileTest {

    /** collects every group the deconvolution formed, which is all this evaluation needs from the pipeline */
    private static final class GroupCollector extends Tracker.NOOP {
        private final List<Tracker.DiaPrecursorGroup> groups = new ArrayList<>();

        @Override
        public boolean tracksDia() {
            return true;
        }

        /** the statistics the thresholds were read from, which is what decides whether a group is believable */
        private String statistics;

        @Override
        public void diaPrecursorGroup(ProcessedSample sample, Tracker.DiaPrecursorGroup group) {
            groups.add(group);
            if (statistics == null && sample != null) {
                statistics = sample.getStorage().getStatistics().getTraceCorrelations().describe();
            }
        }
    }

    @Test
    public void groupingAgainstTheReferenceCompounds() throws IOException {
        final Optional<Path> ddaFile = file("SIRIUS_DDA_TEST_FILE");
        final List<Path> diaFiles = files("SIRIUS_DIA_TEST_FILE");
        assumeTrue(ddaFile.isPresent() && !diaFiles.isEmpty(),
                "set SIRIUS_DDA_TEST_FILE and SIRIUS_DIA_TEST_FILE");
        final double rtTolerance = SpectrumAgreement.DEFAULT_RT_TOLERANCE;

        final SpectrumAgreement.Run reference =
                SpectrumAgreement.Run.of(List.of(ddaFile.get()), "DDA reference", null, false);

        // the compounds another tool also reported, which is the subset the comparison against it has been using
        IntSet mzmineSubset = null;
        final Optional<Path> mgf = file("SIRIUS_MZMINE_MGF");
        if (mgf.isPresent()) {
            final SpectrumAgreement.Run other = MzmineVersusDdaFileTest.read(mgf.get(), "MZmine");
            mzmineSubset = new IntOpenHashSet(SpectrumAgreement.match(other, reference, rtTolerance).compounds());
        }

        // grouping on, the weak precursor filter off: they are different claims and this is about the grouping
        final GroupCollector collector = new GroupCollector();
        PipelineRun.run(diaFiles, "grouped", processing -> {
            processing.setTracker(collector);
            DiaPipelineFileTest.deconvolutionOf(processing).setGroupIndistinguishablePrecursors(true);
        }, true);

        assertTrue(!collector.groups.isEmpty(), "the run has to produce precursor groups");
        // the learned cutoffs the grouping actually used, per condition
        System.out.printf("%n=== what this sample says a genuine co-elution correlates at ===%n%s%n",
                collector.statistics);
        System.out.printf("%n=== %d precursor groups, against %d reference compounds%s ===%n",
                collector.groups.size(), reference.spectra.size(),
                mzmineSubset == null ? "" : " (" + mzmineSubset.size() + " of them also reported by the MGF)");
        report(collector.groups, reference, null, rtTolerance, "every reference compound");
        // the same run with grouping off, to ask whether the members of a group get the same peaks anyway
        final SpectrumAgreement.Run ungrouped = SpectrumAgreement.Run.of(diaFiles, "ungrouped", processing -> {
        }, true);
        reportSharedPeaks(collector.groups, ungrouped, rtTolerance, 12.5d, 8);
        if (mzmineSubset != null) {
            report(collector.groups, reference, mzmineSubset, rtTolerance, "only the compounds the MGF reports too");
        }
    }

    /**
     * @param restrictTo reference compounds to consider, or null for all of them
     */
    private static void report(List<Tracker.DiaPrecursorGroup> groups, SpectrumAgreement.Run reference,
                               IntSet restrictTo, double rtTolerance, String title) {
        int groupsWithCompound = 0, ambiguousWithCompound = 0, compoundIsTop = 0;
        final Int2IntMap rankHistogram = new Int2IntOpenHashMap();
        final List<Double> ratiosWhenNotTop = new ArrayList<>();
        // the best rank a compound reaches anywhere, since a spectrum from any channel or sample is enough for it
        final Int2IntMap bestRankOfCompound = new Int2IntOpenHashMap();

        for (Tracker.DiaPrecursorGroup group : groups) {
            // members sorted by intensity, so a rank can be read off directly
            final Integer[] byIntensity = new Integer[group.memberMz().length];
            for (int k = 0; k < byIntensity.length; ++k) byIntensity[k] = k;
            java.util.Arrays.sort(byIntensity,
                    (a, b) -> Double.compare(group.memberIntensity()[b], group.memberIntensity()[a]));
            final double topIntensity = group.memberIntensity()[byIntensity[0]];

            boolean hasCompound = false;
            for (int rank = 0; rank < byIntensity.length; ++rank) {
                final int member = byIntensity[rank];
                final int compound = matchingCompound(reference, group.memberMz()[member],
                        group.memberRetentionTime()[member], rtTolerance, restrictTo);
                if (compound < 0) continue;
                hasCompound = true;
                rankHistogram.mergeInt(Math.min(rank + 1, 5), 1, Integer::sum);
                if (rank == 0) {
                    ++compoundIsTop;
                } else if (topIntensity > 0) {
                    ratiosWhenNotTop.add(group.memberIntensity()[member] / topIntensity);
                }
                final int previous = bestRankOfCompound.getOrDefault(compound, Integer.MAX_VALUE);
                bestRankOfCompound.put(compound, Math.min(previous, rank + 1));
            }
            if (hasCompound) {
                ++groupsWithCompound;
                if (byIntensity.length > 1) ++ambiguousWithCompound;
            }
        }

        System.out.printf("%n--- %s ---%n", title);
        System.out.printf("  groups containing a reference compound: %d, of which %d have more than one member%n",
                groupsWithCompound, ambiguousWithCompound);
        System.out.printf("  the compound is the most intense member: %d occurrences%n", compoundIsTop);
        final StringBuilder ranks = new StringBuilder();
        for (int rank = 1; rank <= 5; ++rank) {
            ranks.append(String.format(Locale.US, "  rank %s: %d", rank == 5 ? "5+" : String.valueOf(rank),
                    rankHistogram.getOrDefault(rank, 0)));
        }
        System.out.printf("  rank of the compound within its group by intensity:%s%n", ranks);
        if (!ratiosWhenNotTop.isEmpty()) {
            final double[] sorted = new double[ratiosWhenNotTop.size()];
            for (int k = 0; k < sorted.length; ++k) sorted[k] = ratiosWhenNotTop.get(k);
            java.util.Arrays.sort(sorted);
            System.out.printf("  when not the most intense, its intensity relative to the top member: "
                            + "median %.3f, quartiles %.3f and %.3f, worst %.5f%n",
                    sorted[sorted.length / 2], sorted[sorted.length / 4], sorted[3 * sorted.length / 4], sorted[0]);
        }
        // and the number that matters: how many compounds would keep a spectrum at all
        int compoundsKept = 0;
        for (int rank : bestRankOfCompound.values()) if (rank == 1) ++compoundsKept;
        System.out.printf("  reference compounds matched to any group: %d, of which %d are the most intense member "
                        + "somewhere and would keep a spectrum (%.1f %%); %d would lose theirs%n",
                bestRankOfCompound.size(), compoundsKept,
                100d * compoundsKept / Math.max(1, bestRankOfCompound.size()),
                bestRankOfCompound.size() - compoundsKept);
    }

    /**
     * Whether the precursors the grouping calls inseparable actually receive the same fragments when it is off.
     * <p>
     * The grouping rests on a claim that can be checked: that correlating every fragment against every precursor is
     * work that changes nothing, because precursors which correlate with each other above the cutoff cannot be told
     * apart by any fragment either. If the ungrouped run gives two members of one group different peaks, that claim is
     * false and the two approaches are not two ways of computing the same thing.
     * <p>
     * Compared by the identity of the strongest peaks rather than by a cosine. Two spectra with the same peaks and
     * different intensities are the same answer as far as a formula annotation is concerned, and a cosine calls them
     * different - which matters here because the intensity split is fitted per fragment and therefore carries noise
     * that a cosine mistakes for separation.
     * <p>
     * The control is the pairs that co-elute but were <em>not</em> grouped. If those share their peaks just as often,
     * the grouping is not identifying anything the assignment would not have handled.
     */
    private static void reportSharedPeaks(List<Tracker.DiaPrecursorGroup> groups, SpectrumAgreement.Run ungrouped,
                                          double rtTolerance, double windowMz, int topPeaks) {
        // every pair of aligned spectra that one group holds both members of
        final java.util.Set<Long> groupedPairs = new java.util.HashSet<>();
        for (Tracker.DiaPrecursorGroup group : groups) {
            if (group.memberMz().length < 2) continue;
            final int[] spectra = new int[group.memberMz().length];
            for (int k = 0; k < spectra.length; ++k) {
                spectra[k] = ungrouped.bestMatchIndex(new SpectrumAgreement.FeatureSpectrum(group.memberMz()[k],
                        group.memberRetentionTime()[k], new double[0], new double[0]), rtTolerance);
            }
            for (int a = 0; a < spectra.length; ++a) {
                for (int b = a + 1; b < spectra.length; ++b) {
                    if (spectra[a] < 0 || spectra[b] < 0 || spectra[a] == spectra[b]) continue;
                    groupedPairs.add(key(Math.min(spectra[a], spectra[b]), Math.max(spectra[a], spectra[b])));
                }
            }
        }

        // and the same question over every co-eluting pair, split by whether a group holds it
        final int[][] overlapHistogram = new int[2][topPeaks + 1];
        final int[] pairs = new int[2];
        final List<SpectrumAgreement.FeatureSpectrum> spectra = ungrouped.spectra;
        for (int a = 0; a < spectra.size(); ++a) {
            final SpectrumAgreement.FeatureSpectrum left = spectra.get(a);
            if (left.size() == 0) continue;
            for (int b = a + 1; b < spectra.size(); ++b) {
                final SpectrumAgreement.FeatureSpectrum right = spectra.get(b);
                if (right.precursorMz - left.precursorMz > windowMz) break;
                if (right.size() == 0) continue;
                if (Math.abs(right.precursorMz - left.precursorMz)
                        <= SpectrumAgreement.PRECURSOR_DEVIATION.absoluteFor(left.precursorMz)) continue;
                if (Math.abs(right.retentionTime - left.retentionTime) > rtTolerance) continue;
                final int which = groupedPairs.contains(key(a, b)) ? 0 : 1;
                ++pairs[which];
                ++overlapHistogram[which][sharedStrongestPeaks(left, right, topPeaks)];
            }
        }

        System.out.printf("%n--- do the members of one group get the same peaks when grouping is off? ---%n");
        System.out.printf("  comparing the identity of the %d strongest peaks, over co-eluting pairs within "
                + "%.1f m/z%n", topPeaks, windowMz);
        final String[] labels = {"one group holds both", "not grouped together"};
        for (int which = 0; which < 2; ++which) {
            if (pairs[which] == 0) continue;
            double weighted = 0d;
            int atLeastMost = 0;
            final StringBuilder histogram = new StringBuilder();
            for (int shared = 0; shared <= topPeaks; ++shared) {
                weighted += shared * overlapHistogram[which][shared];
                if (shared >= (topPeaks * 3) / 4) atLeastMost += overlapHistogram[which][shared];
                histogram.append(String.format(Locale.US, " %d:%d", shared, overlapHistogram[which][shared]));
            }
            System.out.printf("  %-22s %6d pairs, mean shared %.2f of %d, at least %d shared: %.1f %%%n",
                    labels[which], pairs[which], weighted / pairs[which], topPeaks, (topPeaks * 3) / 4,
                    100d * atLeastMost / pairs[which]);
            System.out.printf("    %s%n", histogram);
        }
    }

    /** how many of the strongest peaks of one spectrum the other also has, by mass */
    private static int sharedStrongestPeaks(SpectrumAgreement.FeatureSpectrum left,
                                            SpectrumAgreement.FeatureSpectrum right, int topPeaks) {
        final Integer[] byIntensity = new Integer[left.size()];
        for (int k = 0; k < byIntensity.length; ++k) byIntensity[k] = k;
        java.util.Arrays.sort(byIntensity, (a, b) -> Double.compare(left.intensity[b], left.intensity[a]));
        final Integer[] theirs = new Integer[right.size()];
        for (int k = 0; k < theirs.length; ++k) theirs[k] = k;
        java.util.Arrays.sort(theirs, (a, b) -> Double.compare(right.intensity[b], right.intensity[a]));
        final int mine = Math.min(topPeaks, byIntensity.length);
        final int hers = Math.min(topPeaks, theirs.length);
        int shared = 0;
        for (int k = 0; k < mine; ++k) {
            for (int j = 0; j < hers; ++j) {
                if (SpectrumAgreement.PEAK_DEVIATION.inErrorWindow(left.mz[byIntensity[k]],
                        right.mz[theirs[j]])) {
                    ++shared;
                    break;
                }
            }
        }
        return shared;
    }

    private static long key(int left, int right) {
        return ((long) left << 32) | (right & 0xffffffffL);
    }

    /** the reference compound at this mass and time, or -1; restricted to a subset if one was given */
    private static int matchingCompound(SpectrumAgreement.Run reference, double mz, double retentionTime,
                                        double rtTolerance, IntSet restrictTo) {
        final SpectrumAgreement.FeatureSpectrum query =
                new SpectrumAgreement.FeatureSpectrum(mz, retentionTime, new double[0], new double[0]);
        final int index = reference.bestMatchIndex(query, rtTolerance);
        if (index < 0) return -1;
        return restrictTo == null || restrictTo.contains(index) ? index : -1;
    }
}
