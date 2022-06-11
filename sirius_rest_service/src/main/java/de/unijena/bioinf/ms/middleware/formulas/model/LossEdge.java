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

package de.unijena.bioinf.ms.middleware.formulas.model;

public class LossEdge {
    FragmentNode sourceFragment;
    FragmentNode targetFragment;
    String molecularFormula;
    double score;

    public FragmentNode getSourceFragment() {
        return sourceFragment;
    }

    public void setSourceFragment(FragmentNode sourceFragment) {
        this.sourceFragment = sourceFragment;
    }

    public FragmentNode getTargetFragment() {
        return targetFragment;
    }

    public void setTargetFragment(FragmentNode targetFragment) {
        this.targetFragment = targetFragment;
    }

    public String getMolecularFormula() {
        return molecularFormula;
    }

    public void setMolecularFormula(String molecularFormula) {
        this.molecularFormula = molecularFormula;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
