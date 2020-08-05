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

package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.biotransformation.BioTransformation;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

import java.util.ArrayList;
import java.util.List;

/**
 * defines the mass difference which is allowed  between a compound and a spectral libary hit
 */
public interface AllowedMassDifference {

    /**
     * the maximum allowed mass difference without mz deviation
     * @return
     */
    public double maxAllowedShift();

    /**
     * is specific mass shift allowed
     * @param mz1
     * @param mz2
     * @param deviation
     * @return
     */
    public boolean isAllowed(double mz1, double mz2, Deviation deviation);


    /**
     * get all considered biotransformations
     * @return
     */
    public MolecularFormula[] getPossibleMassDifferenceExplanations();

    public static AllowedMassDifference onlyAllowDirectMatches() {
        return new DirectMatch();
    }

    public static AllowedMassDifference allowMaxDifference(double maxDifference) {
        return new MaxDifference(maxDifference);
    }

    /**
     * allow same mz and biotransformations. but not conditional biotransformations
     * @return
     */
    public static AllowedMassDifference allowDirectMatchesAndBiotransformations() {
        return new AllowTransformations();
    }


    /**
     * allow same mz and given biotransformations
     * @return
     */
    public static AllowedMassDifference allowDirectMatchesAndBiotransformations(MolecularFormula[] biotransformations) {
        return new AllowTransformations(biotransformations);
    }

    public class DirectMatch implements AllowedMassDifference {

        @Override
        public double maxAllowedShift() {
            return 0;
        }

        @Override
        public boolean isAllowed(double mz1, double mz2, Deviation deviation) {
            if (mz1<mz2){
                return deviation.inErrorWindow(mz1, mz2);
            } else {
                return deviation.inErrorWindow(mz2, mz1);
            }

        }


        @Override
        public MolecularFormula[] getPossibleMassDifferenceExplanations() {
            //only direct matches
            return new MolecularFormula[]{MolecularFormula.emptyFormula()};
        }

    }

    public class MaxDifference implements AllowedMassDifference {
        private double maxDiff;

        public MaxDifference(double maxDiff) {
            this.maxDiff = maxDiff;
        }

        @Override
        public double maxAllowedShift() {
            return maxDiff;
        }

        @Override
        public boolean isAllowed(double mz1, double mz2, Deviation deviation) {
            return Math.abs(mz1-mz2)<=maxDiff+deviation.absoluteFor(Math.max(mz1, mz2));
        }

        @Override
        public MolecularFormula[] getPossibleMassDifferenceExplanations() {
            //no biotransformations directly considered
            return new MolecularFormula[]{MolecularFormula.emptyFormula()};
        }
    }

    public class AllowTransformations implements AllowedMassDifference {

        private MolecularFormula[] transformations;
        private double maxtransformation;

        public AllowTransformations() {
            List<BioTransformation> notConditional = new ArrayList<>();
            for (BioTransformation transformation : BioTransformation.values()) {
                if (!transformation.isConditional()) notConditional.add(transformation);
            }
            transformations = notConditional.stream().map(BioTransformation::getFormula).toArray(l->new MolecularFormula[l]);
            maxtransformation = 0;
            for (MolecularFormula transformation : transformations) {
                //todo formula is difference??!!?
                maxtransformation = Math.max(maxtransformation, transformation.getMass());
            }
        }

        public AllowTransformations(MolecularFormula[] biotransformations) {
            transformations = biotransformations.clone();
            maxtransformation = 0;
            for (MolecularFormula transformation : transformations) {
                //todo formula is difference??!!?
                maxtransformation = Math.max(maxtransformation, transformation.getMass());
            }
        }

        @Override
        public double maxAllowedShift() {
            return maxtransformation;
        }

        @Override
        public boolean isAllowed(double mz1, double mz2, Deviation deviation) {
            double min,max;
            if (mz1<mz2){
                min = mz1;
                max = mz2;
            } else {
                min = mz2;
                max = mz1;
            }
            if (deviation.inErrorWindow(min, max)) return true;
            for (MolecularFormula transformation : transformations) {
                //todo optimize speed
                if (deviation.inErrorWindow(min+transformation.getMass(), max)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public MolecularFormula[] getPossibleMassDifferenceExplanations() {
            MolecularFormula[] transformationsPlusEmpty = new MolecularFormula[transformations.length+1];
            transformationsPlusEmpty[0] = MolecularFormula.emptyFormula();
            for (int i = 0; i < transformations.length; i++) {
                transformationsPlusEmpty[i+1] = transformations[i];

            }
            return transformationsPlusEmpty;
        }

    }

}
