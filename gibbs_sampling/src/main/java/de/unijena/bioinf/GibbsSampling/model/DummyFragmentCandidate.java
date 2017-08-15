package de.unijena.bioinf.GibbsSampling.model;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

public class DummyFragmentCandidate extends FragmentsCandidate{

    private int numberOfIgnoredInstances;

    protected DummyFragmentCandidate(FragmentsAndLosses fragmentsAndLosses, double score, MolecularFormula formula, PrecursorIonType ionType, Ms2Experiment experiment) {
        super(fragmentsAndLosses, score, formula, ionType, experiment);
    }

    public static DummyFragmentCandidate newDummy(double scoreThres, int numberOfIgnoredInstances, Ms2Experiment experiment){

        FragmentsAndLosses fragmentsAndLosses = new FragmentsAndLosses(new FragmentWithIndex[0], new FragmentWithIndex[0]);
        MolecularFormula formula = MolecularFormula.emptyFormula();
        PrecursorIonType ionType = experiment.getPrecursorIonType(); //todo use unknown????
        DummyFragmentCandidate candidate = new DummyFragmentCandidate(fragmentsAndLosses, scoreThres, formula, ionType, experiment);

//        candidate.ionType = ionType;
//        candidate.formula = formula;
        candidate.addAnnotation(MolecularFormula.class, formula);
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
}
