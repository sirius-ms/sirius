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

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import java.io.IOException;

import static de.unijena.bioinf.projectspace.SiriusLocations.TREES;

public class CompoundContainerSerializer implements ContainerSerializer<CompoundContainerId, CompoundContainer> {

    @Override
    public void writeToProjectSpace(ProjectWriter writer, ProjectWriter.ForContainer containerSerializer, CompoundContainerId id, CompoundContainer container) throws IOException {
        // ensure that we are in the right directory
        writer.inDirectory(id.getDirectoryName(), ()->{
            containerSerializer.writeAllComponents(writer, container, container::getAnnotation);
            return true;
        });
    }

    @Override
    public CompoundContainer readFromProjectSpace(ProjectReader reader, ProjectReader.ForContainer<CompoundContainerId, CompoundContainer> containerSerializer, CompoundContainerId id) throws IOException {
        return reader.inDirectory(id.getDirectoryName(), ()->{
            final CompoundContainer container = new CompoundContainer(id);
            if (reader.exists(TREES.relDir())) {
                reader.inDirectory(TREES.relDir(), () -> {
                    for (String file : reader.list("**" + TREES.fileExtDot())) { //todo change to score
                        final String name = file.substring(0, file.length() - TREES.fileExtDot().length());
                        String[] pt = name.split("_");
                        final FormulaResultId fid = new FormulaResultId(id, MolecularFormula.parseOrThrow(pt[0]), PrecursorIonType.fromString(pt[1]));
                        container.results.put(fid.fileName(), fid);
                    }
                    return true;
                });
            }

            containerSerializer.readAllComponents(reader, container, container::setAnnotation);
            return container;
        });
    }

    @Override
    public void deleteFromProjectSpace(ProjectWriter writer, ProjectWriter.DeleteContainer<CompoundContainerId> containerSerializer, CompoundContainerId id) throws IOException {
        writer.inDirectory(id.getDirectoryName(), ()->{
            containerSerializer.deleteAllComponents(writer, id);
            return null;
        });
        writer.delete(id.getDirectoryName());
    }
}
