package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.spectrum.SpectrumStorage;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.trace.segmentation.TraceSegmentationStrategy;
import de.unijena.bioinf.lcms.traceextractor.AbstractTracePicker;
import de.unijena.bioinf.lcms.traceextractor.TraceCachingStrategy;
import de.unijena.bioinf.lcms.traceextractor.TracePicker;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import lombok.Getter;

/**
 * Picks fragment chromatograms within one {@link DiaChannel}, the same way {@link TracePicker} picks mass traces
 * within a sample.
 * <p>
 * All indices this picker takes and returns are indices into the channel's own retention time axis, not MS1 scan
 * indices and not MS2 spectrum uids. That is the whole point of the channel: because a channel contains only the
 * scans of one isolation window at one collision energy, consecutive indices are consecutive measurements of the
 * same m/z range, and stepping from one to the next is as simple as it is for MS1 scans. A fragment trace can
 * therefore never accidentally continue into the scans of a different window.
 * <p>
 * Note that a channel is sampled much more coarsely than the MS1 axis - once per cycle of all windows, so a few
 * seconds rather than a few hundred milliseconds. Peak widths must therefore always be reasoned about in
 * retention time, never in numbers of scan points.
 */
public class Ms2ChannelTracePicker extends AbstractTracePicker {

    @Getter
    private final DiaChannel channel;

    private final SpectrumStorage spectrumStorage;

    private final float noiseLevel;

    private final float expectedPeakWidth;

    /**
     * @param noiseLevel noise level of this channel, see {@link DiaChannelNoise}
     * @param cache      decides how picked traces are stored and how we avoid picking the same peak twice
     */
    public Ms2ChannelTracePicker(ProcessedSample sample, DiaChannel channel, float noiseLevel,
                                 TraceCachingStrategy.Cache cache, TraceSegmentationStrategy segmentationStrategy) {
        super(cache, segmentationStrategy);
        this.channel = channel;
        this.spectrumStorage = sample.getStorage().getSpectrumStorage();
        this.noiseLevel = noiseLevel;
        // The fragments are measured by the same analyzer as the MS1 peaks, so we expect the m/z of a fragment to
        // be about as reproducible from scan to scan as the m/z of a precursor. Callers can widen this.
        setAllowedMassDeviation(sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces());
        // Usually not known yet at this point of the pipeline, exactly as for MS1 traces, in which case the
        // segmentation simply does not apply its peak width rules.
        this.expectedPeakWidth = sample.getStorage().getStatistics().getExpectedPeakWidth().orElse(0d).floatValue();
    }

    /**
     * Whether to verify that a spectrum handed out really is sorted by mass.
     * <p>
     * {@link SimpleSpectrum} declares itself an {@code OrderedSpectrum}, and {@code Spectrums} therefore reaches every
     * peak in it by binary search - but nothing checks the claim, and the datatype rebuilds a stored spectrum with the
     * constructor that neither copies nor orders. A spectrum that is not sorted would make a binary search miss peaks
     * that a linear scan finds, which is what one run did while another did not.
     */
    private static final boolean VERIFY_ORDER =
            Boolean.getBoolean("de.unijena.bioinf.sirius.dia.verifySpectrumOrder");

    @Override
    public SimpleSpectrum getSpectrum(int channelIndex) {
        final SimpleSpectrum spectrum = spectrumStorage.getMs2Spectrum(channel.ms2UidAt(channelIndex));
        if (VERIFY_ORDER && spectrum != null) {
            for (int k = 1; k < spectrum.size(); ++k) {
                if (spectrum.getMzAt(k - 1) > spectrum.getMzAt(k)) {
                    org.slf4j.LoggerFactory.getLogger(Ms2ChannelTracePicker.class).error(String.format(
                            java.util.Locale.US, "UNSORTED ms2 spectrum uid=%d channelIndex=%d size=%d at %d: "
                                    + "%.9f > %.9f", channel.ms2UidAt(channelIndex), channelIndex, spectrum.size(),
                            k, spectrum.getMzAt(k - 1), spectrum.getMzAt(k)));
                    break;
                }
            }
        }
        return spectrum;
    }

    @Override
    public IntIterator iterRightFrom(int channelIndex) {
        final int n = channel.numberOfScans();
        return new IntIterator() {
            private int index = channelIndex + 1;

            @Override
            public int nextInt() {
                return index++;
            }

            @Override
            public boolean hasNext() {
                return index < n;
            }
        };
    }

    @Override
    public IntIterator iterLeftFrom(int channelIndex) {
        return new IntIterator() {
            private int index = channelIndex - 1;

            @Override
            public int nextInt() {
                return index--;
            }

            @Override
            public boolean hasNext() {
                return index >= 0;
            }
        };
    }

    @Override
    public float getNoiseLevelFor(ContiguousTrace trace) {
        return noiseLevel;
    }

    @Override
    public float getExpectedPeakWidth(ContiguousTrace trace) {
        return expectedPeakWidth;
    }

    @Override
    public ContiguousTrace makeTrace(IntArrayList channelIndizes, DoubleArrayList masses, FloatArrayList intensities) {
        final int from = channelIndizes.getInt(0);
        final int to = channelIndizes.getInt(channelIndizes.size() - 1);
        // A trace has to be a dense array over consecutive indices. Our iterators only ever step by one, so this
        // holds by construction - but if it ever stops holding, the trace would silently be misaligned in m/z and
        // intensity, which is far worse than failing here.
        if (to - from + 1 != channelIndizes.size()) {
            throw new IllegalStateException("fragment trace is not contiguous: " + channelIndizes.size()
                    + " scan points spanning indices " + from + " to " + to);
        }
        return new ContiguousTrace(channel.getAxis(), from, to, masses.toDoubleArray(), intensities.toFloatArray());
    }
}
