/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.rest.model.JobId;
import de.unijena.bioinf.ms.rest.model.JobUpdate;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import de.unijena.bioinf.webapi.WebJJob;
import org.jetbrains.annotations.NotNull;

public class CanopusWebJJob extends WebJJob<CanopusWebJJob, CanopusResult, CanopusJobOutput> {

    protected final MaskedFingerprintVersion classyfireVersion, npcVersion;
    protected ProbabilityFingerprint compoundClasses = null;
    protected ProbabilityFingerprint npcClasses = null;

    public CanopusWebJJob(@NotNull JobId jobId, de.unijena.bioinf.ms.rest.model.JobState serverState, MaskedFingerprintVersion classyfireVersion, MaskedFingerprintVersion npcVersion, long submissionTime) {
        super(jobId, serverState, submissionTime);
        this.classyfireVersion = classyfireVersion;
        this.npcVersion = npcVersion;
    }

    @Override
    protected CanopusResult makeResult() {
        return new CanopusResult(compoundClasses, npcClasses);
    }

    @Override
    protected synchronized CanopusWebJJob updateTyped(@NotNull JobUpdate<CanopusJobOutput> update) {
        if (updateState(update)) {
            if (update.data != null) {
                if (update.data.compoundClasses != null) {
                    ProbabilityFingerprint[] fps = readMultipleFingerprints(update.data.compoundClasses);
                    this.compoundClasses = fps[0];
                    this.npcClasses = fps[1];
                }
            }
        }

        checkForTimeout();
        evaluateState();
        return this;
    }

    private ProbabilityFingerprint[] readMultipleFingerprints(byte[] data) {
        byte[] buf1 = new byte[this.classyfireVersion.size() * 8];
        System.arraycopy(data, 0, buf1, 0, buf1.length);
        byte[] buf2 = new byte[this.npcVersion.size() * 8];
        System.arraycopy(data, buf1.length, buf2, 0, buf2.length);
        return new ProbabilityFingerprint[]{
                ProbabilityFingerprint.fromProbabilityArrayBinary(classyfireVersion, buf1),
                ProbabilityFingerprint.fromProbabilityArrayBinary(npcVersion, buf2)
        };
    }
}
