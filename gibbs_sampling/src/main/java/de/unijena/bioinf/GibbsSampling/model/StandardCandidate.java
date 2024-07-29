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
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

/**
 * Created by ge28quv on 12/05/17.
 */
public class StandardCandidate<T> extends Candidate<T> implements HasFormula,HasLibraryHit{
    protected MolecularFormula formula;
    protected PrecursorIonType ionType;
    public boolean isCorrect;
    public boolean inTrainingSet;
    public boolean inEvaluationSet;
    protected LibraryHit hit;

    public StandardCandidate(T candidate, double score, MolecularFormula formula, PrecursorIonType ionType, Ms2Experiment experiment) {
        super(candidate, score, experiment);
        this.ionType = ionType;
        this.formula = formula;
    }


    @Override
    public boolean isCorrect() {
        return isCorrect;
    }

    @Override
    public boolean isInTrainingSet() {
        return inTrainingSet;
    }

    @Override
    public boolean isInEvaluationSet() {
        return inEvaluationSet;
    }

    @Override
    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    @Override
    public void setInTrainingSet(boolean inTrainingSet) {
        this.inTrainingSet = inTrainingSet;
    }

    @Override
    public void setInEvaluationSet(boolean inEvaluationSet) {
        this.inEvaluationSet = inEvaluationSet;
    }

    @Override
    public LibraryHit getLibraryHit() {
        return hit;
    }

    @Override
    public void setLibraryHit(LibraryHit hit) {
        this.hit = hit;
    }

    @Override
    public boolean hasLibraryHit() {
        return this.hit != null;
    }


    @Override
    public MolecularFormula getFormula() {
        return formula;
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StandardCandidate<?> that = (StandardCandidate<?>) o;

        if (!candidate.equals(that.candidate)) return false;
        if (score != that.score) return false;
        if (isCorrect != that.isCorrect) return false;
        if (inTrainingSet != that.inTrainingSet) return false;
        if (inEvaluationSet != that.inEvaluationSet) return false;
        if (!formula.equals(that.formula)) return false;
        if (!ionType.equals(that.ionType)) return false;
        return hit != null ? hit.equals(that.hit) : that.hit == null;
    }

    @Override
    public int hashCode() {
        int result = formula.hashCode();
        result = 31 * result + candidate.hashCode();
        result = 31 * result + ionType.hashCode();
        result = 31 * result + (isCorrect ? 1 : 0);
        result = 31 * result + (inTrainingSet ? 1 : 0);
        result = 31 * result + (inEvaluationSet ? 1 : 0);
        result = 31 * result + (hit != null ? hit.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StandardCandidate{" +
                "formula=" + formula +
                ", ionType=" + ionType +
                ", name=" + experiment.getName() +
                ", score=" + score +
                '}';
    }
}


