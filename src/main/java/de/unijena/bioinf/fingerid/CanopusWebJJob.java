package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.webapi.WebJJob;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import org.jetbrains.annotations.NotNull;

public class CanopusWebJJob extends WebJJob<CanopusWebJJob, CanopusResult, CanopusJobOutput> {

    protected final MaskedFingerprintVersion version;
    protected ProbabilityFingerprint compoundClasses = null;

    public CanopusWebJJob(@NotNull JobId jobId, de.unijena.bioinf.ms.rest.model.JobState serverState, MaskedFingerprintVersion version, long submissionTime) {
        super(jobId, serverState, submissionTime);
        this.version = version;
    }

    @Override
    protected CanopusResult makeResult() {
        return new CanopusResult(compoundClasses);
    }

    @Override
    protected synchronized CanopusWebJJob updateTyped(@NotNull JobUpdate<CanopusJobOutput> update) {
        if (updateState(update)) {
            if (update.data != null) {
                if (update.data.compoundClasses != null)
                    compoundClasses = ProbabilityFingerprint.fromProbabilityArrayBinary(version, update.data.compoundClasses);
            }
        }

        checkForTimeout();
        evaluateState();
        return this;
    }
}
