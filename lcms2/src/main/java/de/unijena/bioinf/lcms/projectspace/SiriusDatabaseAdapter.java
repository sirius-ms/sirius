package de.unijena.bioinf.lcms.projectspace;

import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.run.LCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.MergedLCMSRun;
import de.unijena.bioinf.ms.persistence.model.core.run.RetentionTimeAxis;
import de.unijena.bioinf.ms.persistence.model.core.scan.MSMSScan;
import de.unijena.bioinf.ms.persistence.model.core.scan.Scan;
import de.unijena.bioinf.ms.persistence.model.core.trace.AbstractTrace;

import java.io.IOException;
import java.util.stream.Stream;

public interface SiriusDatabaseAdapter {

    void importRun(LCMSRun run) throws IOException;

    void updateRun(LCMSRun run) throws IOException;

    void importMergedRun(MergedLCMSRun mergedRun) throws IOException;

    void updateMergedRun(MergedLCMSRun mergedRun) throws IOException;

    void removeMergedRun(MergedLCMSRun run) throws IOException;

    void importScan(Scan scan) throws IOException;

    void importMSMSScan(MSMSScan scan) throws IOException;

    void importTrace(AbstractTrace trace) throws IOException;

    void removeSourceTrace(long id) throws IOException;

    void removeMergedTrace(long id) throws IOException;

    boolean importAlignedFeature(AlignedFeatures alignedFeatures) throws IOException;

    /**
     * Writes an already imported feature back, together with its MS data. Needed by everything that adds to a
     * feature after the fact - the fragmentation spectra of a run without MS1 scans are only matched against the
     * features once all of them exist.
     */
    void updateAlignedFeature(AlignedFeatures alignedFeatures) throws IOException;

    /**
     * Imports a feature that consists of nothing but its fragmentation spectra, together with the compound it forms
     * on its own. Such a feature never reaches the adduct network - it has no chromatographic peak to correlate with
     * anything - so this is the only place it can be given a compound.
     *
     * @return false if the feature was rejected, by the same rules that reject one coming from a trace
     */
    boolean importFeatureWithoutLCMS(AlignedFeatures alignedFeatures) throws IOException;

    void importRetentionTimeAxis(RetentionTimeAxis axis, boolean update) throws IOException;

    Stream<AlignedFeatures> getImportedFeatureStream(long runId) throws IOException;
}
