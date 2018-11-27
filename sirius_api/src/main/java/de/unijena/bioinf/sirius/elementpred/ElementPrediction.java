/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.sirius.elementpred;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePatternAnalysis;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.util.ArrayList;
import java.util.Collections;

/**
 * temporary class for predicting elements. Should be replaced by Marvins ML as soon as the
 * paper is published
 */
public class ElementPrediction {

    private Judge[] judges;

    public ElementPrediction(IsotopePatternAnalysis isoAnalyzer) {
        this.judges = new Judge[]{new PredictFromMs1(isoAnalyzer), new PredictFromMs2()};
    }

    public FormulaConstraints extendConstraints(FormulaConstraints input, Ms2Experiment experiment, MeasurementProfile profile) {

        final TObjectIntHashMap<Element> voter = new TObjectIntHashMap<Element>(5, 0.75f, 0);
        for (Judge j : judges) {
            j.vote(voter, experiment);
        }



        final ChemicalAlphabet alphabet = input.getChemicalAlphabet();
        final ArrayList<VotedElement> elems = new ArrayList<VotedElement>();
        final ArrayList<Element> toExtend = new ArrayList<Element>();

        voter.forEachEntry(new TObjectIntProcedure<Element>() {
            @Override
            public boolean execute(Element a, int b) {
                if (b > 0) {
                    elems.add(new VotedElement(a, b));
                }
                return true;
            }
        });

        Collections.sort(elems);
        int threshold = 0;
        for (int k=0; k < elems.size(); ++k) {
            if (elems.get(k).votes > threshold) {
                if (alphabet.indexOf(elems.get(k).element) < 0) {
                    toExtend.add(elems.get(k).element);
                }
                threshold += 4;
            }
        }


        if (toExtend.size() > 0) {
            final ArrayList<Element> newElements = new ArrayList<Element>(input.getChemicalAlphabet().getElements());
            for (Element e : toExtend)
                newElements.add(e);
            final ChemicalAlphabet newAlphabet = new ChemicalAlphabet(newElements.toArray(new Element[newElements.size()]));
            final FormulaConstraints newConstraints = new FormulaConstraints(newAlphabet, input.getFilters());
            for (Element e : alphabet) {
                newConstraints.setUpperbound(e, input.getUpperbound(e));
            }

            final PeriodicTable table = PeriodicTable.getInstance();
            final Element Chlorine = table.getByName("Cl");
            final Element Bromine = table.getByName("Br");
            final Element Iodine = table.getByName("I");
            final Element Florine = table.getByName("F");
            if (newAlphabet.indexOf(Chlorine)>=0 && alphabet.indexOf(Chlorine)<0)
                newConstraints.setUpperbound(Chlorine, 10);
            if (newAlphabet.indexOf(Bromine)>=0 && alphabet.indexOf(Bromine)<0)
                newConstraints.setUpperbound(Bromine, 4);
            if (newAlphabet.indexOf(Iodine)>=0 && alphabet.indexOf(Iodine)<0)
                newConstraints.setUpperbound(Iodine, 10);
            if (newAlphabet.indexOf(Florine)>=0 && alphabet.indexOf(Florine)<0)
                newConstraints.setUpperbound(Florine, 20);
            return newConstraints;
        } else return input;
    }

    private static class VotedElement implements Comparable<VotedElement> {
        private Element element;
        private int votes;

        public VotedElement(Element element, int votes) {
            this.element = element;
            this.votes = votes;
        }

        @Override
        public int compareTo(VotedElement o) {
            return o.votes-votes;
        }
    }

}
