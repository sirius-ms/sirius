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

/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.projectspace.fingerid;

import de.unijena.bioinf.ChemistryBase.fp.Fingerprint;
import de.unijena.bioinf.projectspace.ComponentSerializer;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.ProjectReader;
import de.unijena.bioinf.projectspace.ProjectWriter;
import de.unijena.bioinf.projectspace.sirius.FormulaResult;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class FBCandidateFingerprintSerializerGUI implements ComponentSerializer<FormulaResultId, FormulaResult, FBCandidateFingerprintsGUI> {
    final FBCandidateFingerprintSerializer source = new FBCandidateFingerprintSerializer();

    @Override
    public @Nullable FBCandidateFingerprintsGUI read(ProjectReader projectReader, FormulaResultId formulaResultId, FormulaResult formulaResult) throws IOException {
        formulaResult.setAnnotation(FBCandidateNumber.class, FBCandidateNumber.GUI_DEFAULT);
        final List<Fingerprint> fps = source.readFingerprints(projectReader, formulaResultId, formulaResult);
        return fps == null ? null : new FBCandidateFingerprintsGUI(fps);
    }

    @Override
    public void write(ProjectWriter projectWriter, FormulaResultId formulaResultId, FormulaResult formulaResult, Optional<FBCandidateFingerprintsGUI> optional) throws IOException {
//        LoggerFactory.getLogger(getClass()).warn("Cannot write, this is a read only serializer");
    }

    @Override
    public void delete(ProjectWriter projectWriter, FormulaResultId formulaResultId) throws IOException {
//        LoggerFactory.getLogger(getClass()).warn("Cannot delete, this is a read only serializer");
    }
}
