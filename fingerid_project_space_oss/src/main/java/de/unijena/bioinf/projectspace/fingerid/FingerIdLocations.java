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

import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Location;

public interface FingerIdLocations {
    Location<FormulaResultId>
            SEARCH = new Location<>("search", "json", FormulaResultId::fileName),
            FINGERBLAST = new Location<>("fingerid", "tsv", FormulaResultId::fileName),
            FINGERBLAST_FPs = new Location<>("fingerid", "fps", FormulaResultId::fileName),
            MSNOVELIST_FINGERBLAST = new Location<>("msnovelist", "tsv", FormulaResultId::fileName),
            MSNOVELIST_FINGERBLAST_FPs = new Location<>("msnovelist", "fps", FormulaResultId::fileName),
            FINGERPRINTS = new Location<>("fingerprints", "fpt", FormulaResultId::fileName);

    String
            FINGERID_CLIENT_DATA = "csi_fingerid.tsv",
            FINGERID_CLIENT_DATA_NEG = "csi_fingerid_neg.tsv";
}
