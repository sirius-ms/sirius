/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.properties;

import de.unijena.bioinf.ChemistryBase.utils.DescriptiveOptions;

public enum ConfidenceDisplayMode implements DescriptiveOptions {
    APPROXIMATE ("Approximate (default)", "Select the confidence score display mode. \"exact\" will show confidences for the exact top hit structure to be correct. \"approximate\" will show confidences for the top hit or a sufficiently similar structure to be correct. Structure candidates that are within the similarity threshold are marked in green"),
    EXACT ("Exact", "Select the confidence score display mode. \"exact\" will show confidences for the exact top hit structure to be correct. \"approximate\" will show confidences for the top hit or a sufficiently similar structure to be correct. Structure candidates that are within the similarity threshold are marked in green");

    private final String description;
    private final String displayName;

    ConfidenceDisplayMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
