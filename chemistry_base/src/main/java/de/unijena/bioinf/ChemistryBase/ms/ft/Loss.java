
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

package de.unijena.bioinf.ChemistryBase.ms.ft;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;

import java.util.Arrays;

public class Loss {

    private final static Object[] EMPTY_ARRAY = new Object[0];
    protected final Fragment source;
    protected final Fragment target;
    protected MolecularFormula formula;
    protected double weight;
    protected int sourceEdgeOffset, targetEdgeOffset;
    protected Object[] annotations;

    public Loss(Fragment from, Fragment to, MolecularFormula loss, double weight) {
        this.source = from;
        this.target = to;
        this.formula = loss;
        this.weight = weight;
        this.annotations = EMPTY_ARRAY;
        this.sourceEdgeOffset = 0;
        this.targetEdgeOffset = 0;
    }

    protected Loss(Loss old, Fragment newFrom, Fragment newTo) {
        this.source = newFrom;
        this.target = newTo;
        this.formula = old.formula;
        this.weight = old.weight;
        this.annotations = old.annotations.clone();
        this.sourceEdgeOffset = old.sourceEdgeOffset;
        this.targetEdgeOffset = old.targetEdgeOffset;
    }

    public Loss(Fragment from, Fragment to) {
        this(from, to, from.formula.subtract(to.formula), 0d);
    }

    /**
     * Artificial edges are edges which do not correspond to a fragmentation reaction
     */
    public boolean isArtificial() {
        return formula.isEmpty();
    }

    public boolean isDeleted() {
        return sourceEdgeOffset >= 0;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Fragment getSource() {
        return source;
    }

    public Fragment getTarget() {
        return target;
    }

    public MolecularFormula getFormula() {
        return formula;
    }

    public void setFormula(MolecularFormula formula) {
        this.formula = formula;
    }

    final Object getAnnotation(int id) {
        if (id >= annotations.length) return null;
        return annotations[id];
    }

    final void setAnnotation(int id, int capa, Object o) {
        if (id >= annotations.length) annotations = Arrays.copyOf(annotations, Math.max(capa, id + 1));
        annotations[id] = o;
    }

    public String toString() {
        return formula.toString();
    }

}
