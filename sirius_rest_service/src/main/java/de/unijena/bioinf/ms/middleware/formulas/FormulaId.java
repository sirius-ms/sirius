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

package de.unijena.bioinf.ms.middleware.formulas;

import de.unijena.bioinf.projectspace.FormulaResultId;

/**
 * The CompoundId contains the ID of a compound together with some read-only information that might be displayed in
 * some summary view.
 */
public class FormulaId {

    // identifier
    protected String id;

    // identifier source
    protected String molecularFormula;
    protected String ionType;

    // optional detail
    protected FormulaResultScores resultScores;

    public FormulaId(FormulaResultId id) {
        this(id.fileName(), id.getMolecularFormula().toString(), id.getIonType().toString());
    }

    public FormulaId(String id, String molecularFormula, String ionType) {
        this.id = id;
        this.molecularFormula = molecularFormula;
        this.ionType = ionType;
    }

    public String getId() {
        return id;
    }

    public String getIonType() {
        return ionType;
    }

    public String getMolecularFormula() {
        return molecularFormula;
    }

    public FormulaResultScores getResultScores() {
        return resultScores;
    }

    public void setResultScores(FormulaResultScores availableResults) {
        this.resultScores = availableResults;
    }
}

