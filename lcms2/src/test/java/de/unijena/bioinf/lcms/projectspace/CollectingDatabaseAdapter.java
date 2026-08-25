package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.MergedTrace;
import de.unijena.bioinf.ms.persistence.model.core.trace.SourceTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/** keeps the features instead of persisting them, and hands out the ids the importer expects back */
public class CollectingDatabaseAdapter implements SiriusDatabaseAdapter {
    private final AtomicLong ids = new AtomicLong(1);
    public final List<AlignedFeatures> features = new ArrayList<>();
    public final List<LCMSRun> runs = new ArrayList<>();
    public MergedLCMSRun mergedRun;

    @Override
    public synchronized void importRun(LCMSRun run) {
        run.setRunId(ids.getAndIncrement());
        runs.add(run);
    }

    @Override
    public void updateRun(LCMSRun run) {
    }

    @Override
    public void importMergedRun(MergedLCMSRun mergedRun) {
        mergedRun.setRunId(ids.getAndIncrement());
        this.mergedRun = mergedRun;
    }

    @Override
    public void updateMergedRun(MergedLCMSRun mergedRun) {
    }

    @Override
    public void removeMergedRun(MergedLCMSRun run) {
    }

    @Override
    public void importScan(Scan scan) {
    }

    @Override
    public void importMSMSScan(MSMSScan scan) {
    }

    @Override
    public synchronized void importTrace(AbstractTrace trace) {
        // the importer needs an id back, and which kind of trace it is decides where it is stored
        final long id = ids.getAndIncrement();
        if (trace instanceof MergedTrace merged) merged.setMergedTraceId(id);
        else if (trace instanceof SourceTrace source) source.setSourceTraceId(id);
    }

    @Override
    public void removeSourceTrace(long id) {
    }

    @Override
    public void removeMergedTrace(long id) {
    }

    @Override
    public synchronized boolean importAlignedFeature(AlignedFeatures alignedFeatures) {
        alignedFeatures.setAlignedFeatureId(ids.getAndIncrement());
        features.add(alignedFeatures);
        return true;
    }

    @Override
    public void importRetentionTimeAxis(RetentionTimeAxis axis, boolean update) {
    }

    @Override
    public synchronized Stream<AlignedFeatures> getImportedFeatureStream(long runId) {
        return new ArrayList<>(features).stream();
    }
}
