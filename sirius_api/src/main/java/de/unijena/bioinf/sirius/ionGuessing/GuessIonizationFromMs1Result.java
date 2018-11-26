package de.unijena.bioinf.sirius.ionGuessing;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

public class GuessIonizationFromMs1Result {
    public final PrecursorIonType[] guessedIonTypes;
    public final PrecursorIonType[] candidateIonTypes;
    private IonGuessingSource guessIonizationSource;

    public GuessIonizationFromMs1Result(PrecursorIonType[] guessedIonTypes, PrecursorIonType[] candidateIonTypes, IonGuessingSource guessingSource) {
        this.guessedIonTypes = guessedIonTypes;
        this.candidateIonTypes = candidateIonTypes;
        this.guessIonizationSource = guessingSource;
    }

    public PrecursorIonType[] getGuessedIonTypes() {
        return guessedIonTypes;
    }

    public PrecursorIonType[] getCandidateIonTypes() {
        return candidateIonTypes;
    }

    public IonGuessingSource getGuessingSource() {
        return guessIonizationSource;
    }
}