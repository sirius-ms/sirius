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

package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;
import de.unijena.bioinf.passatutto.Decoy;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.FormulaResult;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.SiriusLocations.DECOYS;

public class PassatuttoSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, Decoy> {

    @Override
    public Decoy read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        return null; // not supported yet
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<Decoy> optDecoy) throws IOException {
        final Decoy decoy = optDecoy.orElseThrow(() -> new RuntimeException("No decoy data found to write for ID: " + id));
        writer.textFile(DECOYS.relFilePath(id), (bw) -> {
            if (decoy.getDecoyTree() != null) {
                new AnnotatedSpectrumWriter(AnnotatedSpectrumWriter.Fields.MZ, AnnotatedSpectrumWriter.Fields.REL_INTENSITY, AnnotatedSpectrumWriter.Fields.FORMULA, AnnotatedSpectrumWriter.Fields.ION).write(bw, decoy.getDecoyTree());
            } else {
                final SimpleSpectrum spec = decoy.getDecoySpectrum();
                bw.write(AnnotatedSpectrumWriter.Fields.MZ.name);
                bw.write('\t');
                bw.write(AnnotatedSpectrumWriter.Fields.REL_INTENSITY.name);
                bw.newLine();
                for (int k = 0; k < spec.size(); ++k) {
                    bw.write(String.valueOf(spec.getMzAt(k)));
                    bw.write('\t');
                    bw.write(String.valueOf(spec.getIntensityAt(k)));
                    bw.newLine();
                }
            }
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        writer.deleteIfExists(DECOYS.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        writer.deleteIfExists(DECOYS.relDir());
    }
}
