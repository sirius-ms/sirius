/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusJobOutput;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class CanopusWebResultConverter implements IOFunctions.IOFunction<CanopusJobOutput, CanopusResult> {
    protected final MaskedFingerprintVersion classyfireVersion, npcVersion;

    public CanopusWebResultConverter(MaskedFingerprintVersion classyfireVersion, MaskedFingerprintVersion npcVersion) {
        this.classyfireVersion = classyfireVersion;
        this.npcVersion = npcVersion;
    }


    @Override
    public CanopusResult apply(@Nullable CanopusJobOutput canopusJobOutput) throws IOException {
        if (canopusJobOutput != null) {
            if (canopusJobOutput.compoundClasses != null) {
                ProbabilityFingerprint[] fps = readMultipleFingerprints(canopusJobOutput.compoundClasses);
                ProbabilityFingerprint compoundClasses = fps[0];
                ProbabilityFingerprint npcClasses = fps[1];
                return new CanopusResult(compoundClasses,npcClasses);
            }
        }
        return null;
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
