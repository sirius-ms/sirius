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

import java.io.IOException;

public class FormulaResultSerializer implements ContainerSerializer<FormulaResultId, FormulaResult> {
    @Override
    public void writeToProjectSpace(ProjectWriter writer, ProjectWriter.ForContainer<FormulaResultId, FormulaResult> containerSerializer, FormulaResultId id, FormulaResult container) throws IOException {
        writer.inDirectory(id.getParentId().getDirectoryName(), ()->{
            containerSerializer.writeAllComponents(writer, container, container::getAnnotation);
            return true;
        });
    }

    @Override
    public FormulaResult readFromProjectSpace(ProjectReader reader, ProjectReader.ForContainer<FormulaResultId, FormulaResult> containerSerializer, FormulaResultId id) throws IOException {
        return reader.inDirectory(id.getParentId().getDirectoryName(), ()->{
            final FormulaResult formulaResult = new FormulaResult(id);
            containerSerializer.readAllComponents(reader, formulaResult, formulaResult::setAnnotation);
            return formulaResult;
        });
    }

    @Override
    public void deleteFromProjectSpace(ProjectWriter writer, ProjectWriter.DeleteContainer<FormulaResultId> containerSerializer, FormulaResultId id) throws IOException {
        writer.inDirectory(id.getParentId().getDirectoryName(), ()->{
            containerSerializer.deleteAllComponents(writer,id);
            return true;
        });
    }
}
