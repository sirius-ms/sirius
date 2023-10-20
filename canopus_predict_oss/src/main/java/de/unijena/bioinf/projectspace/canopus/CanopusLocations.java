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

import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.Location;

public interface CanopusLocations {

    Location<FormulaResultId>
            CF = new Location<>("canopus", "fpt", FormulaResultId::fileName),
            NPC = new Location<>("canopus_npc", "fpt", FormulaResultId::fileName);


    String
            CF_CLIENT_DATA = "canopus.tsv",
            CF_CLIENT_DATA_NEG = "canopus_neg.tsv",
            NPC_CLIENT_DATA = "canopus_npc.tsv",
            NPC_CLIENT_DATA_NEG = "canopus_npc_neg.tsv";
}
