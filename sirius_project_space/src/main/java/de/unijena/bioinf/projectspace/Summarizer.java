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

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.ChemistryBase.algorithm.scoring.SScored;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * A summarizer does not contain any "new" data, but summarizes or visualize the data of other components.
 * Summarizers can be written automatically with other components.
 * <p>
 * ThreadSafe
 * Note: Summarizers might be called in parallel on different compounds and need to be TREAD SAFE!
 */
public interface Summarizer {
    List<Class<? extends DataAnnotation>> requiredFormulaResultAnnotations();

    void addWriteCompoundSummary(ProjectWriter writer, @NotNull final CompoundContainer exp, List<? extends SScored<FormulaResult, ? extends FormulaScore>> results) throws IOException;

    void writeProjectSpaceSummary(ProjectWriter writer) throws IOException;
}
