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

package de.unijena.bioinf.projectspace.canopus;

import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.NPCFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusData;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.CANOPUS;
import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.NPC;

public class CanopusSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, CanopusResult> {
    @Override
    public CanopusResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final String loc = CANOPUS.relFilePath(id);
        if (!reader.exists(loc)) return null;

        final CanopusData canopusData = reader.getProjectSpaceProperty(CanopusDataProperty.class)
                .map(p -> p.getByIonType(id.getIonType())).orElseThrow();

        final double[] probabilities = reader.doubleVector(loc);
        final ProbabilityFingerprint probabilityFingerprint = new ProbabilityFingerprint(canopusData.getFingerprintVersion(), probabilities);

        final Optional<ProbabilityFingerprint> npcFingerprint;
        final String npcLoc = NPC.relFilePath(id);
        if (reader.exists(npcLoc)) {
            npcFingerprint = Optional.of(new ProbabilityFingerprint(MaskedFingerprintVersion.allowAll(NPCFingerprintVersion.get()), reader.doubleVector(npcLoc)));
        } else npcFingerprint = Optional.empty();

        return new CanopusResult(probabilityFingerprint, npcFingerprint);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<CanopusResult> optCanopusResult) throws IOException {
        final CanopusResult canopusResult = optCanopusResult.orElseThrow(() -> new IllegalArgumentException("Could not find canopusResult to write for ID: " + id));

        writer.inDirectory(CANOPUS.relDir(), () -> {
            writer.doubleVector(CANOPUS.fileName(id), canopusResult.getCanopusFingerprint().toProbabilityArray());
            return true;
        });
        if (canopusResult.getNpcFingerprint().isPresent()) {
            writer.inDirectory(NPC.relDir(), () -> {
                writer.doubleVector(CANOPUS.fileName(id), canopusResult.getNpcFingerprint().get().toProbabilityArray());
                return true;
            });
        }
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.delete(CANOPUS.relFilePath(id));
        writer.delete(NPC.relFilePath(id));
    }
}
