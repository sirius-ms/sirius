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

package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

public class SimpleReaction implements Reaction {
    private final MolecularFormula group;
    private final int stepSize;

    public SimpleReaction(MolecularFormula group) {
        this(group, 1);
    }

    public SimpleReaction(MolecularFormula group, int stepSize) {
        if(group == null) {
            throw new RuntimeException("group null");
        } else {
            this.group = group;
            this.stepSize = stepSize;
        }
    }

    public boolean hasReaction(MolecularFormula mf1, MolecularFormula mf2) {
        return mf1.add(this.group).equals(mf2);
    }

    public boolean hasReactionAnyDirection(MolecularFormula mf1, MolecularFormula mf2) {
        return mf1.add(this.group).equals(mf2)?true:mf2.add(this.group).equals(mf1);
    }

    public MolecularFormula netChange() {
        return this.group.clone();
    }

    public Reaction negate() {
        return new SimpleReaction(this.group.negate(), this.stepSize);
    }

    public int stepSize() {
        return this.stepSize;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("SimpleReaction{");
        sb.append(this.group);
        sb.append('}');
        return sb.toString();
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof SimpleReaction)) {
            return false;
        } else {
            SimpleReaction that = (SimpleReaction)o;
            return this.group.equals(that.group);
        }
    }

    public int hashCode() {
        return this.group.hashCode();
    }
}
