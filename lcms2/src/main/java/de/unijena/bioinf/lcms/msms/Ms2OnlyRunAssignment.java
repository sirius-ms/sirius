package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.lcms.quality.CheckMs2Quality;
import de.unijena.bioinf.lcms.quality.QualityAssessment;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.DefaultQualityCategory;
import de.unijena.bioinf.ms.persistence.model.core.QualityReport;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.SampleStatistics;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.storage.StorageUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * What becomes of the fragmentation spectra of a run that has no MS1 scans, see {@link ProcessedSample#isMs2Only()}.
 * <p>
 * Such a run cannot be preprocessed as a chromatogram - there is no trace to pick, nothing to align and nothing to
 * merge - but its spectra were still measured on the same experiment and mostly name the precursor they were taken
 * from. So each of them is offered to the features the other runs produced: a spectrum whose precursor m/z and
 * retention time land on a feature belongs to that feature, and one that lands on none is kept as a compound of its
 * own, with no LC/MS information attached, provided its spectrum is worth keeping at all.
 * <p>
 * The matching and the grouping here are pure computation on numbers, so that both can be tested without a project.
 * Which spectra go in and where the resulting features go is the caller's business.
 */
public class Ms2OnlyRunAssignment {

    /**
     * How far apart in retention time two things may be and still be treated as the same elution, in seconds.
     * <p>
     * Used twice: a feature accepts an MS/MS this far outside its own retention time interval, and two leftover
     * spectra of the same precursor this far apart are the same compound measured twice. A fixed number rather than
     * one derived from the experiment, because the run this measures against has no chromatography of its own to
     * derive anything from - its retention times come from a different measurement and only agree with the features'
     * as well as the two gradients do.
     */
    public static final double RETENTION_TIME_TOLERANCE = 10d;

    /**
     * How much the precursor m/z of an MS/MS may differ from the mass of the feature it is assigned to. Wider than
     * the mass accuracy of the instrument on purpose: this is an isolation target, which is often a rounded number
     * or the mass of a neighbouring isotope peak rather than a measurement of the precursor.
     */
    @Getter @Setter private Deviation allowedMassDeviation = new Deviation(20, 0.01);

    /**
     * The quality a leftover spectrum has to reach before it becomes a feature of its own. Everything below it is
     * discarded: a feature without any LC/MS information is worth having only for the spectrum it carries, so a
     * spectrum that says nothing leaves nothing behind.
     */
    @Getter @Setter private DataQuality minimumQualityForOwnFeature = DataQuality.LOWEST;

    /** a feature of the merged experiment, reduced to what an MS/MS can be matched against */
    public record FeatureTarget(long alignedFeatureId, double mass, double retentionTimeStart,
                                double retentionTimeEnd, double apexRetentionTime) {

        public static FeatureTarget of(AlignedFeatures feature) {
            final RetentionTime rt = feature.getRetentionTime();
            final boolean interval = rt != null && rt.isInterval();
            final double start = interval ? rt.getStartTime() : Double.NaN;
            final double end = interval ? rt.getEndTime() : Double.NaN;
            final double apex = rt == null ? Double.NaN : rt.getMiddleTime();
            return new FeatureTarget(feature.getAlignedFeatureId(), feature.getAverageMass(), start, end, apex);
        }
    }

    /** one fragmentation spectrum of a run without MS1 scans, together with the run it was measured in */
    public record Fragmentation(ProcessedSample sample, Ms2SpectrumHeader header, SimpleSpectrum peaks) {

        public double precursorMz() {
            return header.getPrecursorMz();
        }

        public double retentionTime() {
            return header.getRetentionTime();
        }
    }

    /**
     * @param assigned          which spectra go to which already existing feature
     * @param unassignedGroups  the remaining spectra, grouped by precursor and elution, one group per feature to be
     *                          created. Only groups that passed the quality bar are in here.
     * @param discardedSpectra  how many spectra were dropped as noise, or as too poor to be worth a feature
     * @param withoutPrecursor  how many spectra could not be looked at at all, for not naming a precursor
     */
    public record Result(Long2ObjectMap<List<Fragmentation>> assigned, List<List<Fragmentation>> unassignedGroups,
                         int discardedSpectra, int withoutPrecursor) {

        public int assignedSpectra() {
            return assigned.values().stream().mapToInt(List::size).sum();
        }

        public String describe() {
            return String.format(Locale.US, "%d MS/MS spectra of runs without MS1 scans were assigned to %d existing "
                            + "features, %d became %d features of their own, %d were discarded as noise and %d named "
                            + "no precursor.",
                    assignedSpectra(), assigned.size(),
                    unassignedGroups.stream().mapToInt(List::size).sum(), unassignedGroups.size(),
                    discardedSpectra, withoutPrecursor);
        }
    }

    /**
     * The order the spectra are looked at in, and the order they end up in within a feature. Neither the runs nor
     * the features arrive in a fixed order - both are produced by parallel jobs - so everything that iterates over
     * spectra sorts them by where they were measured first, or two runs on the same input would differ.
     */
    private static final Comparator<Fragmentation> BY_MEASUREMENT =
            Comparator.<Fragmentation>comparingInt(f -> f.sample().getUid())
                    .thenComparingInt(f -> f.header().getUid());

    /**
     * @param features every feature of the experiment, in any order
     * @return where each spectrum belongs
     */
    public Result assign(List<Fragmentation> spectra, List<FeatureTarget> features) {
        final FeatureTarget[] byMass = features.toArray(FeatureTarget[]::new);
        Arrays.sort(byMass, Comparator.comparingDouble(FeatureTarget::mass)
                .thenComparingLong(FeatureTarget::alignedFeatureId));
        final double[] masses = new double[byMass.length];
        for (int k = 0; k < byMass.length; ++k) masses[k] = byMass[k].mass();

        final List<Fragmentation> ordered = new ArrayList<>(spectra);
        ordered.sort(BY_MEASUREMENT);
        final Long2ObjectMap<List<Fragmentation>> assigned = new Long2ObjectOpenHashMap<>();
        final List<Fragmentation> leftovers = new ArrayList<>();
        int withoutPrecursor = 0, discarded = 0;
        for (Fragmentation spectrum : ordered) {
            if (!(spectrum.precursorMz() > 0)) {
                ++withoutPrecursor;
                continue;
            }
            if (!carriesSignal(spectrum)) {
                ++discarded;
                continue;
            }
            final FeatureTarget target = bestMatch(spectrum, byMass, masses);
            if (target == null) leftovers.add(spectrum);
            else assigned.computeIfAbsent(target.alignedFeatureId(), k -> new ArrayList<>()).add(spectrum);
        }

        final List<List<Fragmentation>> groups = new ArrayList<>();
        for (List<Fragmentation> group : group(leftovers)) {
            if (isWorthAFeatureOfItsOwn(group)) groups.add(group);
            else discarded += group.size();
        }
        return new Result(assigned, groups, discarded, withoutPrecursor);
    }

    /**
     * Whether a spectrum holds anything at all, by exactly the bar the data dependent path applies to every MS/MS
     * before it is attached to a trace. A spectrum that would have been dropped there has no business being
     * attached to a feature here, and even less business becoming one.
     */
    private static boolean carriesSignal(Fragmentation spectrum) {
        if (spectrum.peaks().isEmpty()) return false;
        final CheckMs2Quality.PeakStats stats =
                CheckMs2Quality.peaksWithCommonMassDeltas(spectrum.precursorMz(), spectrum.peaks())
                        .mergeMax(CheckMs2Quality.signalPeaks(spectrum.peaks()));
        return stats.numberOfPeaks() >= 3 || stats.ratioOfIntensity() >= 0.25;
    }

    /**
     * The feature an MS/MS was most likely taken from: of everything within the mass tolerance and eluting at the
     * right time, the one whose apex is closest to when the spectrum was measured.
     */
    private FeatureTarget bestMatch(Fragmentation spectrum, FeatureTarget[] byMass, double[] masses) {
        final double mz = spectrum.precursorMz();
        final double tolerance = allowedMassDeviation.absoluteFor(mz);
        int index = Arrays.binarySearch(masses, mz - tolerance);
        if (index < 0) index = -(index + 1);
        // binarySearch may land anywhere within a run of equal masses, so walk back to the first candidate
        while (index > 0 && masses[index - 1] >= mz - tolerance) --index;
        FeatureTarget best = null;
        double bestDistance = Double.POSITIVE_INFINITY, bestMassError = Double.POSITIVE_INFINITY;
        for (int k = index; k < byMass.length && masses[k] <= mz + tolerance; ++k) {
            final FeatureTarget candidate = byMass[k];
            if (!elutesWith(candidate, spectrum.retentionTime())) continue;
            final double distance = Math.abs(candidate.apexRetentionTime() - spectrum.retentionTime());
            final double massError = Math.abs(candidate.mass() - mz);
            // closest in retention time, then closest in mass; the array is sorted, so ties fall to the smaller
            // mass and, at equal mass, to the smaller feature id - the choice must not depend on iteration order
            if (distance < bestDistance || (distance == bestDistance && massError < bestMassError)) {
                best = candidate;
                bestDistance = distance;
                bestMassError = massError;
            }
        }
        return best;
    }

    private static boolean elutesWith(FeatureTarget feature, double retentionTime) {
        if (Double.isNaN(feature.retentionTimeStart())) return false;
        return retentionTime >= feature.retentionTimeStart() - RETENTION_TIME_TOLERANCE
                && retentionTime <= feature.retentionTimeEnd() + RETENTION_TIME_TOLERANCE;
    }

    /**
     * Collects repeated measurements of one precursor into one group, so that a precursor fragmented ten times does
     * not become ten features. Spectra belong together when their precursor masses agree within the tolerance and
     * their retention times are no further apart than {@link #RETENTION_TIME_TOLERANCE}.
     */
    List<List<Fragmentation>> group(List<Fragmentation> spectra) {
        final List<Fragmentation> sorted = new ArrayList<>(spectra);
        sorted.sort(Comparator.comparingDouble(Fragmentation::precursorMz)
                .thenComparingDouble(Fragmentation::retentionTime).thenComparing(BY_MEASUREMENT));
        final List<List<Fragmentation>> byMass = new ArrayList<>();
        for (Fragmentation spectrum : sorted) {
            // measured against the first member rather than the previous one, so that a dense series of precursors
            // cannot chain arbitrarily far away from where it started
            if (byMass.isEmpty() || !allowedMassDeviation.inErrorWindow(
                    byMass.get(byMass.size() - 1).get(0).precursorMz(), spectrum.precursorMz())) {
                byMass.add(new ArrayList<>());
            }
            byMass.get(byMass.size() - 1).add(spectrum);
        }
        final List<List<Fragmentation>> groups = new ArrayList<>();
        for (List<Fragmentation> massGroup : byMass) {
            massGroup.sort(Comparator.comparingDouble(Fragmentation::retentionTime).thenComparing(BY_MEASUREMENT));
            List<Fragmentation> current = null;
            for (Fragmentation spectrum : massGroup) {
                // here the previous member is the right reference: a compound eluting over a long time is one
                // compound, it is a gap in the measurements that separates two of them
                if (current == null || spectrum.retentionTime() - current.get(current.size() - 1).retentionTime()
                        > RETENTION_TIME_TOLERANCE) {
                    current = new ArrayList<>();
                    groups.add(current);
                }
                current.add(spectrum);
            }
        }
        return groups;
    }

    /**
     * Whether a group of leftover spectra says enough to be worth a feature that has nothing else in it. Judged by
     * the same check the preprocessing applies to every other feature's MS/MS, so that a spectrum is not held to a
     * different standard for having arrived through this path.
     */
    boolean isWorthAFeatureOfItsOwn(List<Fragmentation> group) {
        return qualityOf(group).getScore() >= minimumQualityForOwnFeature.getScore();
    }

    /** the MS/MS quality of the feature this group would become */
    DataQuality qualityOf(List<Fragmentation> group) {
        final AlignedFeatures feature = toFeature(group);
        final QualityReport report = new QualityReport();
        new CheckMs2Quality().addToReport(report, noiseLevelOf(group), feature, null);
        final QualityReport.Category category = report.getCategories().get(DefaultQualityCategory.MS2_QUALITY.name());
        if (category == null) return DataQuality.NOT_APPLICABLE;
        return QualityAssessment.scoreCategory(category);
    }

    /**
     * The MS2 quality check reads its noise level off the run. There is no merged run here, so the check is given
     * the loudest of the runs that contributed to the group - the strictest of the answers that are true of it.
     */
    private static MergedLCMSRun noiseLevelOf(List<Fragmentation> group) {
        double noise = 0;
        for (Fragmentation spectrum : group) {
            noise = Math.max(noise, spectrum.sample().getStorage().getStatistics().getMs2NoiseLevel());
        }
        final MergedLCMSRun run = MergedLCMSRun.builder().build();
        run.setSampleStats(new SampleStatistics(null, null, 0, 0, 0, 0, noise));
        return run;
    }

    /**
     * Adds the spectra to a feature that already exists, and rebuilds its merged spectrum over all of them - the
     * merged spectrum is what a reader and the fragmentation tree computation see, so it has to know about the
     * spectra that arrived last.
     */
    public void addTo(AlignedFeatures feature, List<Fragmentation> spectra) {
        MSData msData = feature.getMSData().orElse(null);
        if (msData == null) {
            msData = new MSData();
            feature.setMsData(msData);
        }
        // a fresh list: what comes out of the importer is a fixed size view of an array
        final List<MergedMSnSpectrum> msn = msData.getMsnSpectra() == null
                ? new ArrayList<>() : new ArrayList<>(msData.getMsnSpectra());
        for (Fragmentation spectrum : spectra) msn.add(toMergedMsn(spectrum));
        msData.setMsnSpectra(msn);
        msData.setMergedMSnSpectrum(mergeAndNormalize(feature.getAverageMass(), msn));
        feature.setHasMsMs(true);
    }

    /**
     * The feature a group of spectra becomes: its fragmentation spectra, the precursor mass and the retention time
     * they were measured at, and nothing else. There is no trace, no isotope pattern and no intensity, because no
     * MS1 scan of this compound was ever recorded.
     */
    public AlignedFeatures toFeature(List<Fragmentation> group) {
        final List<MergedMSnSpectrum> msn = new ArrayList<>(group.size());
        double massSum = 0d, minRt = Double.POSITIVE_INFINITY, maxRt = Double.NEGATIVE_INFINITY;
        double apexRt = Double.NaN, largestIonCount = -1d;
        for (Fragmentation spectrum : group) {
            final MergedMSnSpectrum merged = toMergedMsn(spectrum);
            msn.add(merged);
            massSum += spectrum.precursorMz();
            minRt = Math.min(minRt, spectrum.retentionTime());
            maxRt = Math.max(maxRt, spectrum.retentionTime());
            if (merged.getTotalIonCount() != null && merged.getTotalIonCount() > largestIonCount) {
                largestIonCount = merged.getTotalIonCount();
                apexRt = spectrum.retentionTime();
            }
        }
        final double averageMass = massSum / group.size();
        final MSData msData = new MSData();
        msData.setMsnSpectra(msn);
        msData.setMergedMSnSpectrum(mergeAndNormalize(averageMass, msn));

        final AlignedFeatures feature = new AlignedFeatures();
        feature.setDetectedAdducts(new DetectedAdducts());
        feature.setMsData(msData);
        feature.setAverageMass(averageMass);
        feature.setApexMass(averageMass);
        feature.setCharge((byte) group.get(0).sample().getPolarity());
        // the retention time is a measurement even when nothing else about the elution is: it is when the
        // fragmentation happened. Given as an interval so that it is usable for anything that groups by elution.
        feature.setRetentionTime(new RetentionTime(minRt, maxRt, Double.isNaN(apexRt) ? (minRt + maxRt) / 2 : apexRt));
        feature.setHasMs1(false);
        feature.setHasMsMs(true);
        return feature;
    }

    private static MergedMSnSpectrum toMergedMsn(Fragmentation spectrum) {
        final Ms2SpectrumHeader header = spectrum.header();
        double totalIonCount = 0d;
        for (int k = 0; k < spectrum.peaks().size(); ++k) totalIonCount += spectrum.peaks().getIntensityAt(k);
        return new MergedMSnSpectrum(
                header.getMsLevel(),
                spectrum.sample().getPolarity(),
                header.getEnergy().orElse(CollisionEnergy.none()),
                header.getPrecursorMz(),
                header.getIsolationWindow().map(w -> new IsolationWindow[]{w}).orElse(new IsolationWindow[0]),
                new long[]{spectrum.sample().getRun().getRunId()},
                new int[][]{{header.getScanId()}},
                // there is no MS1 scan this fragmentation points at, and no merged axis it could be projected onto.
                // The arrays still have one entry per sample, because readers walk them alongside the sample ids
                new int[][]{{}},
                new int[][]{{}},
                new double[]{header.getPrecursorMz()},
                // chimeric pollution is measured in the MS1 scan of the isolation window, which this run does not have
                null,
                totalIonCount,
                false,
                StorageUtils.cleanMsnDataInLCMSProcessingForImport(spectrum.peaks()));
    }

    public static SimpleSpectrum mergeAndNormalize(double ionMass, List<MergedMSnSpectrum> msn) {
        return de.unijena.bioinf.lcms.projectspace.PickFeaturesAndImportToSirius
                .mergeAndNormalizeMsnSpectra(ionMass, msn.stream().map(MergedMSnSpectrum::toMs2Spectrum).toList());
    }
}
