package de.unijena.bioinf.GibbsSampling.model;

import com.google.common.collect.Lists;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

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
            return PeriodicTable.getInstance().getSelectionFor(Lists.newArrayList(PeriodicTable.getInstance().iterator()).toArray(new Element[0]));
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
