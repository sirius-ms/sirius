package de.unijena.bioinf.GibbsSampling.model;

/**
 * Created by ge28quv on 12/05/17.
 */
public interface HasLibraryHit {
    public boolean isCorrect();
    public boolean isInTrainingSet();
    public boolean isInEvaluationSet();

    public void setCorrect(boolean correct);
    public void setInTrainingSet(boolean inTrainingSet);
    public void setInEvaluationSet(boolean inEvaluationSet);

    public LibraryHit getLibraryHit();
    public void setLibraryHit(LibraryHit hit);
    public boolean hasLibraryHit();


}
