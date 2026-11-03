package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.file;
import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.files;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Judges another tool's DIA deconvolution against the same DDA measurement we judge our own against, by reading the
 * spectra it exported as MGF.
 * <p>
 * The point of going through {@link SpectrumAgreement} rather than writing a second comparison is that the two
 * numbers then differ only in the spectra, never in how they were scored: same reference run, same way of matching a
 * compound, same exclusion of the precursor region, same decoy control. Anything else would compare two measurements
 * of two different things.
 * <p>
 * Both tools must have been given the same input for the comparison to mean anything. That is not automatic here -
 * a tool that cannot read multiplexed DIA scans has to be given a file with those scans removed, and such a file has
 * no MS/MS in the m/z range that was acquired only in them. Point {@code SIRIUS_DIA_TEST_FILE} at the same files the
 * other tool was given, and the run appears in the table next to it.
 * <p>
 * Skipped unless the MGF and a DDA measurement are given:
 * <pre>
 * SIRIUS_MZMINE_MGF=/path/sirius.mgf SIRIUS_DDA_TEST_FILE=/path/DDA_1.mzML \
 *   SIRIUS_DIA_TEST_FILE=/path/fixed/SWATH_1.mzML,... \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*MzmineVersusDdaFileTest*'
 * </pre>
 */
public class MzmineVersusDdaFileTest {

    @Test
    public void compareAnotherToolAgainstDda() throws IOException {
        final Optional<Path> mgf = file("SIRIUS_MZMINE_MGF");
        final Optional<Path> ddaFile = file("SIRIUS_DDA_TEST_FILE");
        assumeTrue(mgf.isPresent() && ddaFile.isPresent(),
                "no MGF and DDA file given, set SIRIUS_MZMINE_MGF and SIRIUS_DDA_TEST_FILE");
        final double rtTolerance = SpectrumAgreement.DEFAULT_RT_TOLERANCE;

        final SpectrumAgreement.Run reference =
                SpectrumAgreement.Run.of(List.of(ddaFile.get()), "DDA reference", null, false);
        final SpectrumAgreement.Run other = read(mgf.get(), "MZmine");

        // our own runs on the same input, if it was given. More than one configuration can be compared at once,
        // and that is the only way to compare them: each configuration reports a slightly different set of
        // compounds, so scoring each on its own set measures the sets as much as the spectra.
        final List<Path> diaFiles = files("SIRIUS_DIA_TEST_FILE");
        final List<SpectrumAgreement.Run> runs = new ArrayList<>();
        runs.add(other);
        if (!diaFiles.isEmpty()) {
            runs.add(SpectrumAgreement.Run.of(diaFiles, "SIRIUS", processing -> {
            }, true));
            // the two reductions of the precursor set, separately and together: they are different claims and
            // measuring them at once cannot say which of them did what
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_COMPARE_GROUPING", "false"))) {
                runs.add(SpectrumAgreement.Run.of(diaFiles, "+ grouping", processing ->
                        DiaPipelineFileTest.deconvolutionOf(processing)
                                .setGroupIndistinguishablePrecursors(true), true));
                runs.add(SpectrumAgreement.Run.of(diaFiles, "+ both", processing ->
                        processing.setDiaAssignmentStrategy(new GroupedAssignmentStrategy()), true));
            }
            // the two probability cuts, which are thresholds on a share of the candidate set rather than on the
            // evidence for any one candidate, so a crowded window cannot clear them at all
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_COMPARE_CUTS", "false"))) {
                runs.add(SpectrumAgreement.Run.of(diaFiles, "no write cut", processing -> {
                    DiaPipelineFileTest.deconvolutionOf(processing).setMinProbabilityForSpectrum(0d);
                    DiaPipelineFileTest.refinementOf(processing).setMinProbabilityForSpectrum(0d);
                }, true));
                runs.add(SpectrumAgreement.Run.of(diaFiles, "no cut at all", processing -> {
                    DiaPipelineFileTest.deconvolutionOf(processing).setMinProbabilityForSpectrum(0d);
                    DiaPipelineFileTest.deconvolutionOf(processing).setMinProbability(0d);
                    DiaPipelineFileTest.refinementOf(processing).setMinProbabilityForSpectrum(0d);
                    DiaPipelineFileTest.refinementOf(processing).setMinProbability(0d);
                }, true));
            }
            // the candidate set bounded by relative evidence instead: no absolute cut, but a candidate has to be
            // within a factor of the best explanation of the fragment
            // grouping collapses the near ties that both dilute the share and ill-condition the factorisation, so
            // it is the one reduction of the candidate set that could make dropping the cuts safe
            // grouping, with the group speaking for every member above a share of its dominant one. Stage two is
            // off throughout: it rebuilds the assignment over the final features without grouping and rewrites
            // every spectrum, so with it on the peaks being compared would not be the grouped ones at all.
            // the two strategies as they would ship: both stages, grouped or not. This is the comparison that
            // decides between them, everything else having varied one piece at a time.
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_COMPARE_STRATEGIES", "false"))) {
                // the simplest of the three: fixed bars, no competition, no second stage. Swept over the precursor
                // bar, because that is the one number deciding how many compounds it attempts at all, and the
                // comparison against the other tool is only fair where the two attempt a similar number.
                for (double bar : new double[]{5e5}) {
                    runs.add(SpectrumAgreement.Run.of(diaFiles, String.format("uncontested %.0e", bar), processing -> {
                        final UncontestedAssignmentStrategy strategy = new UncontestedAssignmentStrategy();
                        strategy.setMinPrecursorIntensity(bar);
                        processing.setDiaAssignmentStrategy(strategy);
                    }, true));
                }
                for (double share : new double[]{0.5}) {
                    runs.add(SpectrumAgreement.Run.of(diaFiles, String.format("grouped %.1f", share), processing -> {
                        final GroupedAssignmentStrategy strategy = new GroupedAssignmentStrategy();
                        strategy.setMinReportedShareOfGroup(share);
                        processing.setDiaAssignmentStrategy(strategy);
                    }, true, true));
                }
            }
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_COMPARE_REPORTED", "false"))) {
                runs.add(SpectrumAgreement.Run.of(diaFiles, "stage 1 only", processing -> {
                }, false));
                for (double share : new double[]{0.3, 0.5, 0.7, 1.0}) {
                    runs.add(SpectrumAgreement.Run.of(diaFiles, String.format("grouped %.1f", share), processing -> {
                        DiaPipelineFileTest.deconvolutionOf(processing).setGroupIndistinguishablePrecursors(true);
                        DiaPipelineFileTest.deconvolutionOf(processing).setMinReportedShareOfGroup(share);
                    }, false));
                }
            }
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_COMPARE_GROUPED_CUTS", "false"))) {
                runs.add(SpectrumAgreement.Run.of(diaFiles, "grouped", processing ->
                        DiaPipelineFileTest.deconvolutionOf(processing)
                                .setGroupIndistinguishablePrecursors(true), true));
                runs.add(SpectrumAgreement.Run.of(diaFiles, "grouped, no cut", processing -> {
                    DiaPipelineFileTest.deconvolutionOf(processing).setGroupIndistinguishablePrecursors(true);
                    DiaPipelineFileTest.deconvolutionOf(processing).setMinProbability(0d);
                    DiaPipelineFileTest.deconvolutionOf(processing).setMinProbabilityForSpectrum(0d);
                    DiaPipelineFileTest.refinementOf(processing).setMinProbability(0d);
                    DiaPipelineFileTest.refinementOf(processing).setMinProbabilityForSpectrum(0d);
                }, true));
            }
            if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_COMPARE_RELATIVE", "false"))) {
                for (double floor : new double[]{0.1, 0.25}) {
                    runs.add(SpectrumAgreement.Run.of(diaFiles, String.format("relative %.2f", floor), processing -> {
                        DiaPipelineFileTest.deconvolutionOf(processing).setMinProbability(0d);
                        DiaPipelineFileTest.deconvolutionOf(processing).setMinProbabilityForSpectrum(0d);
                        DiaPipelineFileTest.deconvolutionOf(processing)
                                .setMinRelativeProbabilityForSpectrum(floor);
                        DiaPipelineFileTest.refinementOf(processing).setMinProbability(0d);
                        DiaPipelineFileTest.refinementOf(processing).setMinProbabilityForSpectrum(0d);
                        DiaPipelineFileTest.refinementOf(processing).setMinRelativeProbabilityForSpectrum(floor);
                    }, true));
                }
            }
        }

        System.out.printf("%n=== reference: %s, %d compounds with a merged MS/MS ===%n",
                ddaFile.get().getFileName(), reference.spectra.size());

        final List<SpectrumAgreement.Matching> matchings = new ArrayList<>();
        for (SpectrumAgreement.Run run : runs) matchings.add(SpectrumAgreement.match(run, reference, rtTolerance));

        // each run on the compounds it reports itself, which is not a comparison at all: a run that only attempts
        // the compounds it can handle looks better than one that also attempts the rest
        System.out.printf("%n=== each run on its own compounds (not comparable with each other) ===%n");
        System.out.println(SpectrumAgreement.header());
        for (SpectrumAgreement.Matching matching : matchings) {
            System.out.println(SpectrumAgreement.score(matching, reference, matching.compounds()).describe());
        }

        final IntSet all = new IntOpenHashSet();
        IntSet shared = null;
        for (SpectrumAgreement.Matching matching : matchings) {
            all.addAll(matching.compounds());
            if (shared == null) shared = new IntOpenHashSet(matching.compounds());
            else shared.retainAll(matching.compounds());
        }
        System.out.printf("%ncompounds: %d reported by every run, %d by at least one%n", shared.size(), all.size());

        // The comparison that separates coverage from quality: every reference compound the DIA acquisition could
        // have isolated at all, a missing spectrum counting as a cosine of zero, plus the same over a fixed budget of
        // the best answers so that reporting more compounds helps only where the extra ones are good.
        SpectrumAgreement.Run diaRun = null;
        for (SpectrumAgreement.Run run : runs) {
            if (run.isolationWindows != null) {
                diaRun = run;
                break;
            }
        }
        if (diaRun != null) {
            final IntSet reachable = SpectrumAgreement.reachable(reference, diaRun);
            System.out.printf("%n=== spectral reconstruction over the %d reference compounds the acquisition could "
                    + "isolate ===%n", reachable.size());
            for (SpectrumAgreement.Matching matching : matchings) {
                System.out.print(SpectrumAgreement.describeReconstruction(matching, reference, reachable,
                        SpectrumAgreement.DEFAULT_COSINE_BUDGET));
            }
            // and the same for the subsets a quality flag would keep, so the flag can be judged by whether what it
            // keeps still beats the alternative rather than by how well it correlates with anything
            for (int k = 0; k < runs.size(); ++k) {
                final double noise = runs.get(k).ms2NoiseLevel;
                if (!(noise > 0)) continue;
                final String name = matchings.get(k).name;
                System.out.printf("  the same run restricted to what the quality flag keeps:%n");
                for (var grades : List.of(
                        List.of(SpectrumAgreement.DiaGrade.GOOD),
                        List.of(SpectrumAgreement.DiaGrade.DECENT),
                        List.of(SpectrumAgreement.DiaGrade.GOOD, SpectrumAgreement.DiaGrade.DECENT),
                        List.of(SpectrumAgreement.DiaGrade.BAD),
                        List.of(SpectrumAgreement.DiaGrade.LOWEST))) {
                    final StringBuilder label = new StringBuilder(name);
                    for (var grade : grades) label.append(' ').append(grade);
                    System.out.print(SpectrumAgreement.describeReconstruction(
                            matchings.get(k).restrictedTo(label.toString(),
                                    spectrum -> grades.contains(SpectrumAgreement.DiaGrade.of(spectrum, noise))),
                            reference, reachable, SpectrumAgreement.DEFAULT_COSINE_BUDGET));
                }
            }
            // and which of the quantities a run knows about itself predict a spectrum that agrees with nothing
            System.out.printf("%n=== can a run tell which of its spectra are uninformative? ===%n");
            for (int k = 0; k < runs.size(); ++k) {
                if (runs.get(k).quality.isEmpty()) continue;
                System.out.print(SpectrumAgreement.describeQualitySignals(matchings.get(k), runs.get(k), reference,
                        reachable, rtTolerance));
            }
        }
        report("every run, on the compounds all of them report", matchings, reference, shared);
        // whether the spectrum being scored is the best one that run has for the compound
        System.out.printf("%n=== spectra competing for one compound, on the compounds all runs report ===%n");
        for (SpectrumAgreement.Run run : runs) {
            System.out.print(SpectrumAgreement.describeCompetition(run, reference, shared, rtTolerance));
        }
        // and whether the fragments missing from a compound's spectrum went to a co-eluting neighbour instead
        System.out.printf("%n=== did the missing fragments go to a neighbour in the same window? ===%n");
        for (double windowMz : new double[]{2d, 12.5d}) {
            for (SpectrumAgreement.Run run : runs) {
                System.out.print(SpectrumAgreement.describeNeighbourRecall(run, reference, shared, rtTolerance,
                        windowMz));
            }
        }
        report("every run, on the compounds any of them reports", matchings, reference, all);

        // and on every compound the reference has, which is the only denominator that charges a run for the
        // compounds it did not attempt at all. Restricting to what two runs both reported flatters whichever of
        // them reports less: it is never asked about the rest.
        final IntSet everything = new IntOpenHashSet();
        for (int index = 0; index < reference.spectra.size(); ++index) everything.add(index);
        report("every run, on every compound the reference has", matchings, reference, everything);

        // what a variant destroys rather than what it keeps: the compounds one run reports and another does not,
        // scored on the run that still has them. A reduction of the precursor set is free exactly if the spectra it
        // removes were no better than chance, and no aggregate over the surviving compounds can say whether they were.
        System.out.printf("%n=== the spectra each variant gives up, scored on the run that keeps them ===%n");
        for (SpectrumAgreement.Matching baseline : matchings) {
            for (SpectrumAgreement.Matching variant : matchings) {
                if (baseline == variant) continue;
                final IntSet lost = new IntOpenHashSet(baseline.compounds());
                lost.removeAll(variant.compounds());
                if (lost.size() < 20) continue;
                System.out.printf("  %s loses %d of %s's compounds:%n", variant.name, lost.size(), baseline.name);
                System.out.println(SpectrumAgreement.header());
                System.out.println(SpectrumAgreement.score(baseline, reference, lost).describe());
                System.out.printf("    intensity agreement per compound %.1f %%%n",
                        100 * SpectrumAgreement.score(baseline, reference, lost).meanConfirmedShare());
            }
        }

        // our own run is the second, the MGF being the first
        if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_DUMP_WORST", "false"))
                && matchings.size() > 1) {
            dumpWorst(matchings.get(1), reference, rtTolerance);
        }

        // and the compounds where the MGF agrees with the reference and we do not, which is where a trace level
        // debugging of one compound can see what happened to peaks that demonstrably were there
        if (Boolean.parseBoolean(System.getenv().getOrDefault("SIRIUS_DIA_DUMP_DIVERGENT", "false"))
                && matchings.size() > 1) {
            dumpDivergent(matchings.get(0), matchings.get(1), reference, shared);
        }

        assertTrue(other.spectra.size() > 0, "the MGF must contain MS2 spectra");
        final SpectrumAgreement.Agreement theirs =
                SpectrumAgreement.score(matchings.get(0), reference, matchings.get(0).compounds());
        assertTrue(theirs.matchedFeatures > 0, "and its compounds must match the reference");
        assertTrue(theirs.confirmedRate() > theirs.decoyRate(), "its agreement must beat chance");
    }

    /**
     * The compounds where most of our intensity sits on peaks the reference does not have, with the offending peaks
     * and what else was eluting in the same isolation window at the time.
     * <p>
     * Aggregate rates say a few very intense peaks per spectrum are wrong; they cannot say what those peaks are.
     */
    private static void dumpWorst(SpectrumAgreement.Matching mine, SpectrumAgreement.Run reference,
                                  double rtTolerance) {
        record Case(double unconfirmedIntensity, int index, SpectrumAgreement.FeatureSpectrum ours,
                    SpectrumAgreement.FeatureSpectrum match) {
        }
        final List<Case> cases = new ArrayList<>();
        for (int index : mine.compounds()) {
            final SpectrumAgreement.FeatureSpectrum ours = mine.byReference.get(index);
            final SpectrumAgreement.FeatureSpectrum match = reference.spectra.get(index);
            double total = 0d, unconfirmed = 0d;
            for (int k = 0; k < ours.size(); ++k) {
                total += ours.intensity[k];
                if (!match.contains(ours.mz[k])) unconfirmed += ours.intensity[k];
            }
            if (total > 0 && ours.size() >= 3) cases.add(new Case(unconfirmed / total, index, ours, match));
        }
        cases.sort((a, b) -> Double.compare(b.unconfirmedIntensity(), a.unconfirmedIntensity()));

        System.out.printf("%n=== the compounds whose intensity sits most on peaks the reference lacks ===%n");
        for (int rank = 0; rank < Math.min(20, cases.size()); ++rank) {
            final Case worst = cases.get(rank);
            final SpectrumAgreement.FeatureSpectrum ours = worst.ours(), match = worst.match();
            System.out.printf("%n%2d. m/z %.4f at %.1f s: %d peaks, %.0f %% of intensity unconfirmed "
                            + "(reference has %d peaks)%n", rank + 1, ours.precursorMz, ours.retentionTime,
                    ours.size(), 100 * worst.unconfirmedIntensity(), match.size());
            // our strongest peaks, and whether the reference has them
            final Integer[] byIntensity = new Integer[ours.size()];
            for (int k = 0; k < byIntensity.length; ++k) byIntensity[k] = k;
            java.util.Arrays.sort(byIntensity, (a, b) -> Double.compare(ours.intensity[b], ours.intensity[a]));
            for (int k = 0; k < Math.min(5, byIntensity.length); ++k) {
                final int peak = byIntensity[k];
                final double share = 100 * ours.intensity[peak] / ours.basePeakIntensity;
                System.out.printf("      m/z %9.4f  %5.1f %% of base  %s%s%n", ours.mz[peak], share,
                        match.contains(ours.mz[peak]) ? "confirmed" : "NOT in reference",
                        reference.hasPrecursorNear(ours.mz[peak], ours.retentionTime, rtTolerance)
                                ? "  (is another compound's precursor)" : "");
            }
            // and what else the reference saw in the same isolation window at the same time, which is what could
            // have produced those peaks instead
            final List<String> neighbours = reference.compoundsNear(ours.precursorMz, ours.retentionTime,
                    rtTolerance, 50d);
            System.out.printf("      co-eluting within 50 m/z: %s%n",
                    neighbours.isEmpty() ? "none the reference saw" : String.join(", ", neighbours));
        }
    }

    /**
     * The compounds the MGF gets right and we do not, with the fragment masses that make the difference.
     * <p>
     * These are the only cases where the raw data is known to hold the peak: another tool found it in the same file
     * and the DDA measurement of the same compound confirms it. Whatever happened to such a peak happened in our
     * pipeline, and a tracker pointed at one of these compounds can say where.
     */
    private static void dumpDivergent(SpectrumAgreement.Matching theirs, SpectrumAgreement.Matching ours,
                                      SpectrumAgreement.Run reference, IntSet compounds) {
        record Case(double advantage, int theirConfirmed, int weFound, SpectrumAgreement.FeatureSpectrum reference,
                    SpectrumAgreement.FeatureSpectrum them, SpectrumAgreement.FeatureSpectrum us) {
        }
        final List<Case> cases = new ArrayList<>();
        for (int index : compounds) {
            final SpectrumAgreement.FeatureSpectrum them = theirs.byReference.get(index);
            final SpectrumAgreement.FeatureSpectrum us = ours.byReference.get(index);
            final SpectrumAgreement.FeatureSpectrum match = reference.spectra.get(index);
            if (them == null || us == null) continue;
            // the peaks they and the reference agree on, and how many of those we have
            int confirmed = 0, weHave = 0;
            for (int k = 0; k < them.size(); ++k) {
                if (!match.contains(them.mz[k])) continue;
                ++confirmed;
                if (us.contains(them.mz[k])) ++weHave;
            }
            if (confirmed < 5) continue;
            cases.add(new Case(confirmed - weHave, confirmed, weHave, match, them, us));
        }
        cases.sort((a, b) -> Double.compare(b.advantage(), a.advantage()));

        System.out.printf("%n=== compounds the MGF gets right and we do not ===%n");
        for (int rank = 0; rank < Math.min(10, cases.size()); ++rank) {
            final Case worst = cases.get(rank);
            final SpectrumAgreement.FeatureSpectrum match = worst.reference(), them = worst.them(), us = worst.us();
            System.out.printf("%n%2d. m/z %.4f at %.1f s: reference %d peaks, MGF %d peaks of which %d confirmed, "
                            + "we have %d peaks and %d of those confirmed masses%n", rank + 1, match.precursorMz,
                    match.retentionTime, match.size(), them.size(), worst.theirConfirmed(), us.size(),
                    worst.weFound());
            System.out.printf("      our precursor %.4f at %.1f s%n", us.precursorMz, us.retentionTime);
            // the masses all three could have agreed on, strongest of theirs first
            final Integer[] byIntensity = new Integer[them.size()];
            for (int k = 0; k < byIntensity.length; ++k) byIntensity[k] = k;
            java.util.Arrays.sort(byIntensity, (a, b) -> Double.compare(them.intensity[b], them.intensity[a]));
            final StringBuilder missing = new StringBuilder();
            for (int k = 0; k < byIntensity.length; ++k) {
                final int peak = byIntensity[k];
                if (!match.contains(them.mz[peak]) || us.contains(them.mz[peak])) continue;
                if (missing.length() > 0) missing.append(", ");
                missing.append(String.format(java.util.Locale.US, "%.4f (%.0f %% of their base)", them.mz[peak],
                        100 * them.intensity[peak] / them.basePeakIntensity));
            }
            System.out.printf("      confirmed masses we lack: %s%n", missing);
        }
    }

    /** one block of the table: every run scored over the same set of reference compounds */
    private static void report(String title, List<SpectrumAgreement.Matching> matchings,
                               SpectrumAgreement.Run reference, IntSet compounds) {
        System.out.printf("%n=== %s ===%n", title);
        System.out.println(SpectrumAgreement.header());
        final List<SpectrumAgreement.Agreement> rows = new ArrayList<>();
        for (SpectrumAgreement.Matching matching : matchings) {
            rows.add(SpectrumAgreement.score(matching, reference, compounds));
        }
        for (SpectrumAgreement.Agreement row : rows) System.out.println(row.describe());
        System.out.println("  corrected for chance:");
        for (SpectrumAgreement.Agreement row : rows) System.out.println(row.describeNet());
        // how many compounds a run gets substantially right, by mass alone
        System.out.printf("  of the reference's strongest %d peaks, how many are among the run's strongest %d:%n",
                SpectrumAgreement.DEFAULT_TOP_PEAKS, SpectrumAgreement.DEFAULT_TOP_PEAKS);
        for (SpectrumAgreement.Matching matching : matchings) {
            System.out.print(SpectrumAgreement.describeTopPeaks(matching, reference, compounds,
                    SpectrumAgreement.DEFAULT_TOP_PEAKS));
        }
        // and whether a run can recognise its own good spectra without being shown the reference
        System.out.println("  can the run tell its good spectra apart by itself?");
        for (SpectrumAgreement.Matching matching : matchings) {
            System.out.print(SpectrumAgreement.describeQuality(matching, reference, compounds,
                    SpectrumAgreement.DEFAULT_TOP_PEAKS));
        }
        // the same weighting asked per compound, which says whether an intensity difference is systematic
        System.out.println("  intensity agreement per compound instead of over the pooled current:");
        for (SpectrumAgreement.Agreement row : rows) {
            System.out.printf("    %-15s %6.1f %%%n", row.name(), 100 * row.meanConfirmedShare());
        }
        System.out.printf("    %-15s %8s %8s %8s %8s %8s %8s%n", "compounds at", "0 %", "0-10 %", "10-25 %",
                "25-50 %", "50-75 %", "75+ %");
        for (SpectrumAgreement.Agreement row : rows) System.out.println(row.describeShareDistribution());
        // whether a run gives two co-eluting precursors of different mass the same spectrum, and whether the
        // reference thinks they should have it
        System.out.println("  co-eluting pairs of different precursor mass sharing a spectrum:");
        for (SpectrumAgreement.Matching matching : matchings) {
            System.out.print(SpectrumAgreement.describeDuplication(matching, reference, compounds,
                    SpectrumAgreement.DEFAULT_RT_TOLERANCE, 12.5d));
        }
        // and how the peaks are confirmed as a function of how many of the run's own spectra carry the same mass
        System.out.println("  by how many of the run's own spectra carry the mass:");
        for (SpectrumAgreement.Matching matching : matchings) {
            System.out.print(SpectrumAgreement.describeUbiquity(matching, reference, compounds));
        }
    }

    /**
     * Reads the MS2 spectra of an MGF, one per feature.
     * <p>
     * Only {@code MSLEVEL=2} blocks are taken. An export of this kind also carries a correlated pseudo MS1 spectrum
     * per feature, which is an isotope and adduct pattern rather than a fragmentation spectrum and has nothing to do
     * with what is being compared here.
     */
    static SpectrumAgreement.Run read(Path path, String name) throws IOException {
        final List<SpectrumAgreement.FeatureSpectrum> spectra = new ArrayList<>();
        int blocks = 0, ms2Blocks = 0;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            boolean inside = false;
            int msLevel = 0;
            double precursorMz = Double.NaN, retentionTime = Double.NaN;
            SimpleMutableSpectrum peaks = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.equals("BEGIN IONS")) {
                    inside = true;
                    msLevel = 0;
                    precursorMz = Double.NaN;
                    retentionTime = Double.NaN;
                    peaks = new SimpleMutableSpectrum();
                    ++blocks;
                } else if (line.equals("END IONS")) {
                    inside = false;
                    if (msLevel == 2 && Double.isFinite(precursorMz) && Double.isFinite(retentionTime)) {
                        ++ms2Blocks;
                        final SpectrumAgreement.FeatureSpectrum spectrum = SpectrumAgreement.FeatureSpectrum.of(
                                precursorMz, retentionTime, new SimpleSpectrum(peaks));
                        if (spectrum.size() > 0) spectra.add(spectrum);
                    }
                    peaks = null;
                } else if (inside) {
                    final int equals = line.indexOf('=');
                    if (equals > 0 && !Character.isDigit(line.charAt(0))) {
                        final String key = line.substring(0, equals);
                        final String value = line.substring(equals + 1).trim();
                        switch (key) {
                            case "MSLEVEL" -> msLevel = Integer.parseInt(value);
                            // the m/z of the precursor, optionally followed by its intensity
                            case "PEPMASS" -> precursorMz = Double.parseDouble(value.split("\\s+")[0]);
                            case "RTINSECONDS" -> retentionTime = Double.parseDouble(value);
                            default -> {
                            }
                        }
                    } else {
                        final String[] columns = line.split("\\s+");
                        if (columns.length >= 2) {
                            peaks.addPeak(Double.parseDouble(columns[0]), Double.parseDouble(columns[1]));
                        }
                    }
                }
            }
        }
        System.out.printf("%s: %d blocks in %s, %d of them MS2, %d with fragment peaks below the precursor%n",
                name, blocks, path.getFileName(), ms2Blocks, spectra.size());
        return new SpectrumAgreement.Run(name, ms2Blocks, spectra);
    }
}
