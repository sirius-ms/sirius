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

package de.unijena.bioinf.sirius.elementdetection;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.ft.Ms1IsotopePattern;
import de.unijena.bioinf.ms.annotations.Provides;
import de.unijena.bioinf.ms.annotations.Requires;
import de.unijena.bioinf.sirius.ProcessedInput;

import java.util.Set;

@Requires(Ms1IsotopePattern.class)
@Provides(FormulaConstraints.class)
public interface ElementDetection {

    /**
     * detect chemical elements from isotope pattern.
     * @return null, if no isotope pattern available, otherwise the formula constraints for the detected elements
     */
    public FormulaConstraints detect(ProcessedInput processedInput);

    public Set<Element> getPredictableElements();

    public default boolean isPredictable(Element e) {
        return getPredictableElements().contains(e);
    }

}
