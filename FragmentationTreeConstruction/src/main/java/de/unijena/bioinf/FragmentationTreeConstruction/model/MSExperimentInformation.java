package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.TableSelection;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;

public class MSExperimentInformation {

    private final ChemicalAlphabet alphabet;
    private final Deviation massError, intensityError, parentPeakMassError;

    public MSExperimentInformation(ChemicalAlphabet alphabet, Deviation massError, Deviation parentPeakMassError,  Deviation intensityError) {
        this.alphabet = alphabet;
        this.massError = massError;
        this.intensityError = intensityError;
        this.parentPeakMassError = parentPeakMassError;
    }

    public MSExperimentInformation(ChemicalAlphabet alphabet, Deviation massError, Deviation intensityError) {
        this(alphabet, massError, massError, intensityError);
    }

    public Deviation getParentPeakMassError() {
        return parentPeakMassError;
    }

    public TableSelection getPeriodicTableSelection() {
        return alphabet.getTableSelection();
    }

    public Deviation getMassError() {
        return massError;
    }

    public Deviation getIntensityError() {
        return intensityError;
    }

	public ChemicalAlphabet getAlphabet() {
		return alphabet;
	}
}
