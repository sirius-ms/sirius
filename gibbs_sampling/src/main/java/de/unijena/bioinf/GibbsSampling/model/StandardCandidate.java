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


}


