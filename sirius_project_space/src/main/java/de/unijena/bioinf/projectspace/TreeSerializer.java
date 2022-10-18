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

package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonReader;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.AnnotatedSpectrumWriter;

import java.io.IOException;
import java.util.Optional;

import static de.unijena.bioinf.projectspace.SiriusLocations.SPECTRA;
import static de.unijena.bioinf.projectspace.SiriusLocations.TREES;

public class TreeSerializer implements ComponentSerializer<FormulaResultId, FormulaResult, FTree> {
    @Override
    public FTree read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        return reader.inDirectory(TREES.relDir(), () -> {
            final String relativePath = TREES.fileName(id);
            return reader.textFile(relativePath, (r) -> new FTJsonReader().parse(r, reader.asURI(relativePath)));
        });

        //NOTE: we do not need to read annotated spectra because the information is already in the trees.
        // The annotated spectra are just for the users spectra
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FTree> optTree) throws IOException {
        final FTree tree = optTree.orElseThrow(() -> new RuntimeException("Could not find tree for FormulaResult with ID: " + id));
        // write tree json
        writer.inDirectory(TREES.relDir(), () -> {
            writer.textFile(TREES.fileName(id), (w) -> new FTJsonWriter().writeTree(w, tree));
            return true;
        });

        //write annotated spectra
        writer.inDirectory(SPECTRA.relDir(), () -> {
            writer.textFile(SPECTRA.fileName(id), (w) -> new AnnotatedSpectrumWriter().write(w, tree));
            return true;
        });
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
        //delete trees
        writer.deleteIfExists(TREES.relFilePath(id));
        writer.deleteIfExists(SPECTRA.relFilePath(id));
    }

    @Override
    public void deleteAll(ProjectWriter writer) throws IOException {
        //delete trees
        writer.deleteIfExists(TREES.relDir());
        writer.deleteIfExists(SPECTRA.relDir());
    }
}