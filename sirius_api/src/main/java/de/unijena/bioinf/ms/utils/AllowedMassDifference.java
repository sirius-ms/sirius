package de.unijena.bioinf.ms.utils;

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


    public static AllowedMassDifference onlyAllowDirectMatches() {
        return new DirectMatch();
    }

    /**
     * allow same mz and biotransformations. but not conditional biotransformations
     * @return
     */
    public static AllowedMassDifference allowDirectMatchesAndBiotransformations() {
        return new AllowTransformations();
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

    }

    public class AllowTransformations implements AllowedMassDifference {

        private BioTransformation[] transformations;
        private double maxtransformation;

        public AllowTransformations() {
            List<BioTransformation> notConditional = new ArrayList<>();
            for (BioTransformation transformation : BioTransformation.values()) {
                if (!transformation.isConditional()) notConditional.add(transformation);
            }
            transformations = notConditional.toArray(new BioTransformation[0]);
            maxtransformation = 0;
            for (BioTransformation transformation : transformations) {
                //todo formula is difference??!!?
                maxtransformation = Math.max(maxtransformation, transformation.getFormula().getMass());
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
            for (BioTransformation transformation : transformations) {
                //todo optimize speed
                if (deviation.inErrorWindow(min+transformation.getFormula().getMass(), max)){
                    return true;
                }
            }
            return false;
        }

    }

}
