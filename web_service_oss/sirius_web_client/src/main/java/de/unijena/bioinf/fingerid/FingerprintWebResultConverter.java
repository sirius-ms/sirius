

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

import de.unijena.bioinf.ChemistryBase.fp.AbstractFingerprint;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerprintJobOutput;

import java.io.IOException;

public class FingerprintWebResultConverter implements IOFunctions.IOFunction<FingerprintJobOutput, FingerprintResult> {

    protected final MaskedFingerprintVersion version;

    public FingerprintWebResultConverter(MaskedFingerprintVersion version) {
        this.version = version;
    }

    @Override
    public FingerprintResult apply(FingerprintJobOutput fingerprintJobOutput) throws IOException {
        if (fingerprintJobOutput != null && fingerprintJobOutput.fingerprint != null) {
            ProbabilityFingerprint prediction = ProbabilityFingerprint.fromProbabilityArrayBinary(version, fingerprintJobOutput.fingerprint);
            double[] iokrVerctor = fingerprintJobOutput.iokrVector != null
                    ? AbstractFingerprint.convertToDoubles(fingerprintJobOutput.iokrVector) : null;
            //todo add IOKR if needed
            return new FingerprintResult(prediction);
        }
        return null;
    }
}
