package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.LCMSProcessing;
import de.unijena.bioinf.lcms.projectspace.SiriusDatabaseAdapter;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MSData;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.file;
import static de.unijena.bioinf.lcms.deconvolution.DiaPipelineFileTest.files;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * How much of a DDA spectrum is present in the raw DIA scans at all - the upper limit on what any deconvolution of
 * this measurement could recover.
 * <p>
 * Everything else we measure compares one assignment against another, which cannot say whether a fragment we failed
 * to report was ever there to be found. This does: for a compound both measurements saw, it takes the DDA spectrum's
 * fragment masses and looks them up in the DIA scans that cover that compound's retention time in the isolation
 * window containing its precursor - no trace building, no assignment, just the centroids the instrument wrote.
 * <p>
 * The number is meaningless without its decoy. A DIA scan holds on the order of a hundred peaks and a compound is
 * covered by several of them, so a union over a retention time window is dense enough that a fair share of any mass
 * list will match it by chance. The same lookup is therefore also done in scans of the same windows taken far away in
 * retention time, where the compound is not eluting: whatever that finds is what density alone explains. The gap
 * between the two is the real ceiling.
 * <p>
 * Skipped unless both files are given:
 * <pre>
 * SIRIUS_DIA_TEST_FILE=/path/SWATH_1.mzML SIRIUS_DDA_TEST_FILE=/path/DDA_1.mzML \
 *   ./gradlew :lcms2:cleanTest :lcms2:test --tests '*DiaCeilingFileTest*'
 * </pre>
 */
public class DiaCeilingFileTest {

    /** for looking up a DDA fragment mass among the raw DIA centroids */
    private static final Deviation PEAK_DEVIATION = new Deviation(10, 0.002);

    /** below this share of the base peak a DDA peak is not reliably a fragment */
    private static final double MIN_RELATIVE_INTENSITY = 0.01;

    /** peaks this close to the precursor are the surviving precursor ion and its isotopes */
    private static final double PRECURSOR_REGION = 1.5;

    /** how far from the compound's apex the decoy scans are taken, in seconds */
    private static final double DECOY_RT_DISTANCE = 300d;

    /** the windows to try, in scan points of the channel to either side of the compound's apex */
    private static final int[] WINDOWS = {0, 1, 2, 4};

    @Test
    public void howMuchOfADdaSpectrumIsInTheRawDiaScans() throws IOException {
        final List<Path> diaFiles = files("SIRIUS_DIA_TEST_FILE");
        final Optional<Path> ddaFile = file("SIRIUS_DDA_TEST_FILE");
        assumeTrue(!diaFiles.isEmpty() && ddaFile.isPresent(),
                "no file pair given, set SIRIUS_DIA_TEST_FILE and SIRIUS_DDA_TEST_FILE");

        final PipelineRun dda = PipelineRun.run(ddaFile.get(), "DDA reference");
        System.out.printf("%n=== reference: %s, %d features with a merged MS/MS ===%n", ddaFile.get().getFileName(),
                dda.featuresWithMsMs().size());

        // the DIA file needs no deconvolution here, only its scans and its acquisition scheme
        final LCMSProcessing processing = new LCMSProcessing(new NoAdapter(), false);
        processing.setDeconvolutionDecision(sample -> false);
        try {
            final ProcessedSample sample = processing.processSample(diaFiles.get(0));
            final Optional<DiaScanIndex> index = DiaScanIndexBuilder.build(sample);
            assumeTrue(index.isPresent(), "no DIA acquisition scheme could be recovered");

            System.out.printf("%n%-12s %10s %12s %10s %10s %12s%n", "window", "compounds", "DDA peaks", "found",
                    "by decoy", "raw peaks");
            Ceiling widest = null;
            for (int window : WINDOWS) {
                final Ceiling ceiling = measure(dda.featuresWithMsMs(), sample, index.get(), window);
                System.out.println(ceiling.describe(window));
                widest = ceiling;
            }
            final int[] real = funnel(dda.featuresWithMsMs(), sample, index.get(), false);
            final int[] chance = funnel(dda.featuresWithMsMs(), sample, index.get(), true);
            System.out.printf("%n=== where the reference peaks are lost (within two scan points of the apex) ===%n");
            System.out.printf("%d reference fragment peaks%n", real[0]);
            final String[] labels = {"in the scans at all", "in 2 consecutive scans", "in 3 consecutive scans",
                    "in 3 consecutive scans and above the noise"};
            for (int k = 0; k < labels.length; ++k) {
                System.out.printf("  %-42s %6.1f %%   (%4.1f %% by decoy)%n", labels[k],
                        100d * real[k + 1] / real[0], 100d * chance[k + 1] / chance[0]);
            }
            assertTrue(widest != null && widest.ddaPeaks > 0, "there must be reference peaks to look for");
            // the compound's own scans have to hold more of its fragments than scans where it is not eluting
            assertTrue(widest.foundRate() > widest.decoyRate(), "co-eluting scans must beat distant ones");
        } finally {
            processing.closeStorages();
        }
    }

    /**
     * @param window how many scan points of the channel to include on either side of the compound's apex
     */
    private static Ceiling measure(List<AlignedFeatures> features, ProcessedSample sample, DiaScanIndex index,
                                   int window) {
        final Ceiling ceiling = new Ceiling();
        for (AlignedFeatures feature : features) {
            final MSData data = feature.getMSData().orElseThrow();
            final SimpleSpectrum reference = data.getMergedMSnSpectrum();
            if (reference == null || reference.isEmpty()) continue;
            final double precursorMz = feature.getAverageMass();
            final double retentionTime = feature.getRetentionTime().getRetentionTimeInSeconds();
            final List<DiaChannel> channels = index.channelsCovering(precursorMz);
            if (channels.isEmpty()) {
                ++ceiling.compoundsOutsideEveryWindow;
                continue;
            }

            final DoubleArrayList raw = peaksAround(sample, channels, retentionTime, window);
            final DoubleArrayList decoy = peaksAround(sample, channels, decoyRetentionTime(sample, retentionTime),
                    window);
            if (raw.isEmpty()) continue;
            ++ceiling.compounds;
            ceiling.rawPeaks.add(raw.size());

            double base = 0d;
            for (int k = 0; k < reference.size(); ++k) base = Math.max(base, reference.getIntensityAt(k));
            for (int k = 0; k < reference.size(); ++k) {
                final double mz = reference.getMzAt(k);
                if (mz > precursorMz - PRECURSOR_REGION) continue;
                if (reference.getIntensityAt(k) < MIN_RELATIVE_INTENSITY * base) continue;
                ++ceiling.ddaPeaks;
                ceiling.ddaIntensity += reference.getIntensityAt(k);
                if (contains(raw, mz)) {
                    ++ceiling.found;
                    ceiling.foundIntensity += reference.getIntensityAt(k);
                }
                if (!decoy.isEmpty()) {
                    ++ceiling.decoyPeaks;
                    if (contains(decoy, mz)) ++ceiling.decoyFound;
                }
            }
        }
        return ceiling;
    }

    /**
     * Where the fragments that are present but never reported are lost.
     * <p>
     * Being in the scans is not enough to become a peak of ours. A fragment has to be followable as a chromatogram -
     * the trace picker needs it in at least {@link DiaTraceCorrelation#MIN_POINTS} consecutive scan points - and it
     * has to be seeded above the channel's noise level. Splitting the reference peaks by those two conditions says
     * whether what we do not report was unreachable for the trace building or lost in the assignment afterwards.
     */
    private static int[] funnel(List<AlignedFeatures> features, ProcessedSample sample, DiaScanIndex index,
                               boolean decoy) {
        final int window = 2;
        int total = 0, aboveNoise = 0;
        // how many fragments reach a run of at least one, two, three consecutive scans
        final int[] runs = new int[4];
        final java.util.Map<Integer, Float> noiseOfChannel = new java.util.HashMap<>();
        for (AlignedFeatures feature : features) {
            final SimpleSpectrum reference = feature.getMSData().orElseThrow().getMergedMSnSpectrum();
            if (reference == null || reference.isEmpty()) continue;
            final double precursorMz = feature.getAverageMass();
            final List<DiaChannel> channels = index.channelsCovering(precursorMz);
            if (channels.isEmpty()) continue;
            final double own = feature.getRetentionTime().getRetentionTimeInSeconds();
            // the same lookup where the compound is not eluting, which is what density alone explains
            final double retentionTime = decoy ? decoyRetentionTime(sample, own) : own;

            double base = 0d;
            for (int k = 0; k < reference.size(); ++k) base = Math.max(base, reference.getIntensityAt(k));
            for (int k = 0; k < reference.size(); ++k) {
                final double mz = reference.getMzAt(k);
                if (mz > precursorMz - PRECURSOR_REGION) continue;
                if (reference.getIntensityAt(k) < MIN_RELATIVE_INTENSITY * base) continue;
                ++total;
                int bestRun = 0;
                double bestIntensity = 0d;
                float noise = 0f;
                for (DiaChannel channel : channels) {
                    final float channelNoise = noiseOfChannel.computeIfAbsent(channel.getChannelId(),
                            id -> DiaChannelNoise.estimate(sample, channel));
                    final int apex = channel.channelIndexForRetentionTime(retentionTime);
                    int run = 0;
                    for (int i = Math.max(0, apex - window);
                         i <= Math.min(channel.numberOfScans() - 1, apex + window); ++i) {
                        final double intensity = intensityAt(sample, channel, i, mz);
                        if (intensity > 0) {
                            ++run;
                            bestRun = Math.max(bestRun, run);
                            if (intensity > bestIntensity) {
                                bestIntensity = intensity;
                                noise = channelNoise;
                            }
                        } else {
                            run = 0;
                        }
                    }
                }
                if (bestIntensity <= 0) continue;
                for (int run = 1; run <= Math.min(3, bestRun); ++run) ++runs[run];
                if (bestRun >= DiaTraceCorrelation.MIN_POINTS && bestIntensity > noise) ++aboveNoise;
            }
        }
        return new int[]{total, runs[1], runs[2], runs[3], aboveNoise};
    }

    /** @return the intensity of the peak at this mass in that scan of the channel, or zero */
    private static double intensityAt(ProcessedSample sample, DiaChannel channel, int channelIndex, double mz) {
        final Ms2SpectrumHeader header = sample.getStorage().getSpectrumStorage()
                .ms2SpectrumHeader(channel.ms2UidAt(channelIndex));
        if (header == null || DiaPseudoSpectra.isDeconvoluted(header)) return 0d;
        final SimpleSpectrum spectrum = sample.getStorage().getSpectrumStorage().getMs2Spectrum(header.getUid());
        if (spectrum == null) return 0d;
        final int index = de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums.mostIntensivePeakWithin(spectrum, mz,
                PEAK_DEVIATION);
        return index < 0 ? 0d : spectrum.getIntensityAt(index);
    }

    /** a retention time far enough from the compound's own that it cannot be eluting there any more */
    private static double decoyRetentionTime(ProcessedSample sample, double retentionTime) {
        final double last = sample.getMapping().getRetentionTimeAt(sample.getMapping().length() - 1);
        return retentionTime + DECOY_RT_DISTANCE <= last ? retentionTime + DECOY_RT_DISTANCE
                : retentionTime - DECOY_RT_DISTANCE;
    }

    /**
     * Every centroid the given channels wrote within the window around this retention time, sorted by mass. Not
     * filtered by intensity and not traced: the question is what the instrument recorded, not what we can follow.
     */
    private static DoubleArrayList peaksAround(ProcessedSample sample, List<DiaChannel> channels,
                                               double retentionTime, int window) {
        final DoubleArrayList masses = new DoubleArrayList();
        for (DiaChannel channel : channels) {
            final int apex = channel.channelIndexForRetentionTime(retentionTime);
            for (int k = Math.max(0, apex - window); k <= Math.min(channel.numberOfScans() - 1, apex + window); ++k) {
                final Ms2SpectrumHeader header = sample.getStorage().getSpectrumStorage()
                        .ms2SpectrumHeader(channel.ms2UidAt(k));
                if (header == null || DiaPseudoSpectra.isDeconvoluted(header)) continue;
                final SimpleSpectrum spectrum = sample.getStorage().getSpectrumStorage().getMs2Spectrum(header.getUid());
                if (spectrum == null) continue;
                for (int p = 0; p < spectrum.size(); ++p) masses.add(spectrum.getMzAt(p));
            }
        }
        masses.sort(null);
        return masses;
    }

    private static boolean contains(DoubleArrayList sortedMasses, double queryMz) {
        final double allowed = PEAK_DEVIATION.absoluteFor(queryMz);
        int low = 0, high = sortedMasses.size() - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final double mz = sortedMasses.getDouble(mid);
            if (mz < queryMz - allowed) low = mid + 1;
            else if (mz > queryMz + allowed) high = mid - 1;
            else return true;
        }
        return false;
    }

    private static final class Ceiling {
        private int compounds, compoundsOutsideEveryWindow, ddaPeaks, found, decoyPeaks, decoyFound;
        private double ddaIntensity, foundIntensity;
        private final IntArrayList rawPeaks = new IntArrayList();

        double foundRate() {
            return ddaPeaks == 0 ? 0d : found / (double) ddaPeaks;
        }

        double decoyRate() {
            return decoyPeaks == 0 ? 0d : decoyFound / (double) decoyPeaks;
        }

        String describe(int window) {
            rawPeaks.sort(null);
            final int median = rawPeaks.isEmpty() ? 0 : rawPeaks.getInt(rawPeaks.size() / 2);
            return String.format(java.util.Locale.US, "%-12s %10d %12d %8.1f %% %8.1f %% %12d",
                    "apex +-" + window, compounds, ddaPeaks, 100 * foundRate(), 100 * decoyRate(), median);
        }
    }

    /** the DIA file is only read here, never imported */
    private static final class NoAdapter implements SiriusDatabaseAdapter {
        @Override public void importRun(de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun run) {
            run.setRunId(1L);
        }
        @Override public void updateRun(de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun run) {
        }
        @Override public void importMergedRun(de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun run) {
            run.setRunId(2L);
        }
        @Override public void updateMergedRun(de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun run) {
        }
        @Override public void removeMergedRun(de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun run) {
        }
        @Override public void importScan(de.unijena.bioinf.ms.persistence.model.core.scan.Scan scan) {
        }
        @Override public void importMSMSScan(de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan scan) {
        }
        @Override public void importTrace(de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace trace) {
        }
        @Override public void removeSourceTrace(long id) {
        }
        @Override public void removeMergedTrace(long id) {
        }
        @Override public boolean importAlignedFeature(AlignedFeatures alignedFeatures) {
            return true;
        }
        @Override public void importRetentionTimeAxis(
                de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis axis, boolean update) {
        }
        @Override public java.util.stream.Stream<AlignedFeatures> getImportedFeatureStream(long runId) {
            return java.util.stream.Stream.empty();
        }
    }
}
