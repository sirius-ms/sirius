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

import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.canopus.CanopusResult;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusCfData;
import de.unijena.bioinf.ms.rest.model.canopus.CanopusNpcData;
import de.unijena.bioinf.projectspace.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.CF;
import static de.unijena.bioinf.projectspace.canopus.CanopusLocations.NPC;

public class CanopusSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, CanopusResult> {
    @Override
    public CanopusResult read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        final String loc = CF.relFilePath(id);
        if (!reader.exists(loc)) return null;

        final CanopusCfData canopusCFData = reader.getProjectSpaceProperty(CanopusCfDataProperty.class)
                .map(p -> p.getByIonType(id.getIonType())).orElseThrow();

        final double[] cfProbabilities = reader.doubleVector(loc);
        final ProbabilityFingerprint probabilityFingerprint = new ProbabilityFingerprint(canopusCFData.getFingerprintVersion(), cfProbabilities);

       ProbabilityFingerprint npcFingerprint = null;
        final String npcLoc = NPC.relFilePath(id);
        if (reader.exists(npcLoc)) {
            final CanopusNpcData canopusNPCData = reader.getProjectSpaceProperty(CanopusNpcDataProperty.class)
                    .map(p -> p.getByIonType(id.getIonType())).orElse(null);

            if (canopusNPCData != null) {
                final double[] npcProbabilities = reader.doubleVector(npcLoc);
                npcFingerprint = new ProbabilityFingerprint(canopusNPCData.getFingerprintVersion(), npcProbabilities);
            }else {
                LoggerFactory.getLogger(getClass()).debug("Cannot write Canopus NPC summaries due to missing CANOPUS NPC model data."); //this is for backwards compatibility version
            }
        }


        return new CanopusResult(probabilityFingerprint, npcFingerprint);
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<CanopusResult> optCanopusResult) throws IOException {
        final CanopusResult canopusResult = optCanopusResult.orElseThrow(() -> new IllegalArgumentException("Could not find canopusResult to write for ID: " + id));

        writer.inDirectory(CF.relDir(), () -> {
            writer.doubleVector(CF.fileName(id), canopusResult.getCanopusFingerprint().toProbabilityArray());
            return true;
        });
        if (canopusResult.getNpcFingerprint().isPresent()) {
            writer.inDirectory(NPC.relDir(), () -> {
                writer.doubleVector(CF.fileName(id), canopusResult.getNpcFingerprint().get().toProbabilityArray());
                return true;
            });
        }
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.deleteIfExists(CF.relFilePath(id));
        writer.deleteIfExists(NPC.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(CF.relDir());
        writer.deleteIfExists(NPC.relDir());
    }
}
