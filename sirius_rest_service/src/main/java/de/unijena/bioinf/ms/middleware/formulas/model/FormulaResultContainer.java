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

package de.unijena.bioinf.ms.middleware.formulas.model;

import de.unijena.bioinf.projectspace.FormulaResultId;
import lombok.Getter;
import lombok.Setter;

/**
 * Container for formula level results that holds a unique identifier (molecular formula + adduct).
 * It can be extended/annotated with a list of results that are available for this formula candidate and their scores.
 * It can further be extended/annotated with the action formula candidate results.
  */
@Getter
@Setter
public class FormulaResultContainer {

    /**
     * Unique identifier of this formula candidate
     */
    protected String id;

    /**
     * molecular formula of this formula candidate
     */
    protected String molecularFormula;
    /**
     * Adduct of this formula candidate
     */
    protected String adduct;

    /**
     * Available results for this formula candidate (OPTIONAL)
     */
    protected ResultOverview resultOverview;

    /**
     * The actual formula candidate represented by this identifier (OPTIONAL)
     */
    protected FormulaCandidate candidate;


    public FormulaResultContainer(FormulaResultId id) {
        this(id.fileName(), id.getMolecularFormula().toString(), id.getIonType().toString());
    }

    public FormulaResultContainer(String id, String molecularFormula, String adduct) {
        this.id = id;
        this.molecularFormula = molecularFormula;
        this.adduct = adduct;
    }
}

