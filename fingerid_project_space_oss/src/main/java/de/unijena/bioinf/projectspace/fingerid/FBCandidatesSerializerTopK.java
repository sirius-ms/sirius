/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.CompoundCandidate;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.FormulaResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class FBCandidatesSerializerTopK implements ComponentSerializer<FormulaResultId, FormulaResult, FBCandidatesTopK> {
    final FBCandidateNumber k;
    final FBCandidatesSerializer source = new FBCandidatesSerializer();

    public FBCandidatesSerializerTopK(FBCandidateNumber k) {
        this.k = k;
    }

    @Override
    public FBCandidatesTopK read(ProjectReader reader, FormulaResultId id, FormulaResult container) throws IOException {
        boolean hasK = id.hasAnnotation(FBCandidateNumber.class);
        try {
            if (!hasK)
                id.setAnnotation(FBCandidateNumber.class, k);
            ArrayList<Scored<CompoundCandidate>> c = source.readCandidates(reader, id, container);
            return c == null ? null : new FBCandidatesTopK(c);
        } finally {
            if (!hasK)
                id.removeAnnotation(FBCandidateNumber.class);
        }
    }

    @Override
    public void write(ProjectWriter writer, FormulaResultId id, FormulaResult container, Optional<FBCandidatesTopK> optFingeridResult) throws IOException {
//        LoggerFactory.getLogger(getClass()).warn("Cannot write, this is a read only serializer");
    }

    @Override
    public void delete(ProjectWriter writer, FormulaResultId id) throws IOException {
//        LoggerFactory.getLogger(getClass()).warn("Cannot delete, this is a read only serializer");
    }
}
