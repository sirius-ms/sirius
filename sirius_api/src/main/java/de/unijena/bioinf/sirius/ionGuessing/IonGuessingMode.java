package de.unijena.bioinf.sirius.ionGuessing;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.PossibleIonModes;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;
import de.unijena.bioinf.ms.properties.DefaultProperty;

import java.util.Arrays;

@DefaultProperty
@Deprecated
public enum IonGuessingMode implements Ms2ExperimentAnnotation {
    DISABLED,
    SELECT,
    ADD_IONS;

    public boolean isEnabled() {
        return this.equals(SELECT) || this.equals(ADD_IONS);
    }

    public void updateGuessedIons(PossibleIonModes modeToModify, PrecursorIonType[] ionTypes) {
        updateGuessedIons(modeToModify, ionTypes, null);
    }

    /**
     * use this method to update this {@link PossibleIonModes} after guessing from MS1.
     * Don't forget to set appropriate {@link IonGuessingMode}
     *
     * @param ionTypes
     * @param probabilities
     */
    public void updateGuessedIons(PossibleIonModes modeToModify, PrecursorIonType[] ionTypes, double[] probabilities) {
        if (probabilities == null) {
            probabilities = new double[ionTypes.length];
            Arrays.fill(probabilities, 1d);
        }

        if (equals(IonGuessingMode.ADD_IONS)) {
            //adds new iondetection with their probabilities
            modeToModify.add(ionTypes, probabilities);
        } else if (equals(IonGuessingMode.SELECT)) {
            //selects from known ion modes. no new modes allowed
            //set all probabilities to 0
            for (Ionization ionType : modeToModify.getIonModes()) {
                modeToModify.add(ionType, 0d);
            }
            //add new probabilities
            for (int i = 0; i < ionTypes.length; i++) {
                if (modeToModify.add(ionTypes[i], probabilities[i])) {
                    throw new RuntimeException("Adding new ion mode is forbidden. It is only allowed to select known ion modes.");
                }
            }
        } else {
            throw new RuntimeException("guessing ionization is disabled");
        }
    }
}
