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

import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.stream.StreamSupport;

public class DummyFragmentCandidate extends FragmentsCandidate{

    private int numberOfIgnoredInstances;
    public static final DummyMolecularFormula dummy = new DummyMolecularFormula();

    protected DummyFragmentCandidate(FragmentsAndLosses fragmentsAndLosses, double score, MolecularFormula formula, PrecursorIonType ionType, Ms2Experiment experiment) {
        super(fragmentsAndLosses, score, formula, ionType, experiment);
    }

    public static DummyFragmentCandidate newDummy(double scoreThres, int numberOfIgnoredInstances, Ms2Experiment experiment){

        FragmentsAndLosses fragmentsAndLosses = new FragmentsAndLosses(new FragmentWithIndex[0], new FragmentWithIndex[0]);
        PrecursorIonType ionType = experiment==null?null:experiment.getPrecursorIonType(); //todo use unknown????
        DummyFragmentCandidate candidate = new DummyFragmentCandidate(fragmentsAndLosses, scoreThres, dummy, ionType, experiment);

//        candidate.ionType = ionType;
//        candidate.formula = formula;
        candidate.addAnnotation(MolecularFormula.class, dummy);
        candidate.addAnnotation(PrecursorIonType.class, ionType);
        candidate.numberOfIgnoredInstances = numberOfIgnoredInstances;
        return candidate;
    }

    public int getNumberOfIgnoredInstances() {
        return numberOfIgnoredInstances;
    }

    public static boolean isDummy(Candidate fragmentsCandidate) {
        return (fragmentsCandidate instanceof DummyFragmentCandidate);
    }

    public static class DummyMolecularFormula extends MolecularFormula {

        public DummyMolecularFormula(){
            super();
        }

        @Override
        public TableSelection getTableSelection() {
            return PeriodicTable.getInstance().getSelectionFor(StreamSupport.stream(PeriodicTable.getInstance().spliterator(), false).toArray(Element[]::new));
        }

        @Override
        protected short[] buffer() {
            return new short[0];
        }

        @Override
        public String toString() {
            return "not_explainable";
        }

        @Override
        public String formatByHill() {
            return "not_explainable";
        }
    }
}
