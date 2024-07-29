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

package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.fingerid.FingerprintResult;
import de.unijena.bioinf.ms.rest.model.fingerid.FingerIdData;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.FormulaResult;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.fingerid.FingerIdLocations.FINGERPRINTS;

public class FingerprintSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FingerprintResult> {

    @Override
    public FingerprintResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        if (!reader.exists(FINGERPRINTS.relFilePath(id)))
            return null;

        return reader.inDirectory(FINGERPRINTS.relDir(), () -> {
            final FingerIdData fingerIdData = reader.getProjectSpaceProperty(FingerIdDataProperty.class)
                    .map(p -> p.getByIonType(id.getIonType())).orElseThrow();

            final double[] probabilities = reader.doubleVector(FINGERPRINTS.fileName(id));
            return new FingerprintResult(new ProbabilityFingerprint(fingerIdData.getFingerprintVersion(), probabilities));
        });
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FingerprintResult> optPrint) throws IOException {
        final FingerprintResult fingerprintResult = optPrint.orElseThrow(() -> new IllegalArgumentException("Could not find finderprint to write for ID: " + id));
        writer.inDirectory(FINGERPRINTS.relDir(), () -> {
            writer.doubleVector(FINGERPRINTS.fileName(id), fingerprintResult.fingerprint.toProbabilityArray());
            return true;
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.deleteIfExists(FINGERPRINTS.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(FINGERPRINTS.relDir());
    }
}
