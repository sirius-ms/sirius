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

package de.isas.mztab2.io;

import de.isas.mztab2.model.Parameter;
import de.isas.mztab2.model.SmallMoleculeEvidence;

public enum MZTabParameter {
    MS_LEVEL("MS", "MS:1000511", SmallMoleculeEvidence.Properties.msLevel.getPropertyName()),
    RELIABILITY("MS", "MS:1002955", "hr-ms compound identification confidence level"/*SmallMoleculeSummary.Properties.reliability.getPropertyName()*/);

    public final String cvLabel;
    public final String cvAccession;
    public final String parameterName;

    MZTabParameter(String cvLabel, String cvAccession, String name) {
        this.cvLabel = cvLabel;
        this.cvAccession = cvAccession;
        this.parameterName = name;
    }

    public static Parameter newInstance(MZTabParameter p) {
        return new Parameter().cvLabel(p.cvLabel).cvAccession(p.cvAccession).name(p.parameterName);
    }
}