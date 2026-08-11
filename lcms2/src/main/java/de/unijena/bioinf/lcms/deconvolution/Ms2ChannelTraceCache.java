package de.unijena.bioinf.lcms.deconvolution;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.lcms.trace.ContiguousTrace;
import de.unijena.bioinf.lcms.trace.Ms2TraceStorage;
import de.unijena.bioinf.lcms.trace.ProcessedSample;
import de.unijena.bioinf.lcms.traceextractor.TraceCachingStrategy;
import lombok.Getter;

import java.util.Optional;

/**
 * Stores the fragment traces of one {@link DiaChannel} and hands out the one that already covers a peak, so that
 * the same peak is never picked into two different traces.
 * <p>
 * The spectrum ids this cache is asked about are indices into the channel's own retention time axis, which is what
 * {@link Ms2ChannelTracePicker} works with. Everything below this class keeps the traces in retention time, so
 * there is only one place where scan points and retention times meet.
 */
public class Ms2ChannelTraceCache implements TraceCachingStrategy.Cache {

    @Getter
    private final DiaChannel channel;

    private final Ms2TraceStorage.ChannelTraces traces;

    private final Deviation allowedMassDeviation;

    public Ms2ChannelTraceCache(ProcessedSample sample, DiaChannel channel) {
        this(sample, channel, sample.getStorage().getStatistics().getMs1MassDeviationWithinTraces());
    }

    /**
     * @param allowedMassDeviation must be the same deviation the picker uses, otherwise a peak could be considered
     *                             part of an existing trace by one and not by the other
     */
    public Ms2ChannelTraceCache(ProcessedSample sample, DiaChannel channel, Deviation allowedMassDeviation) {
        this.channel = channel;
        this.allowedMassDeviation = allowedMassDeviation;
        this.traces = sample.getStorage().getMs2TraceStorage().forChannel(channel.getChannelId(), channel.getAxis());
    }

    @Override
    public ContiguousTrace addTraceToCache(ContiguousTrace trace) {
        return traces.add(trace);
    }

    @Override
    public Optional<ContiguousTrace> getTraceFromCache(int channelIndex, double mz) {
        return traces.find(channelIndex, mz, allowedMassDeviation);
    }

    /**
     * @return number of fragment traces picked in this channel so far
     */
    public int numberOfTraces() {
        return traces.size();
    }
}
