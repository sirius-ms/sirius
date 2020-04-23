package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MolecularProperty;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;

public class DummyMolecularProperty extends MolecularProperty{

    public final int absoluteIndex;
    public final int relativeIndex;
    public final PredictionPerformance performance;

    public DummyMolecularProperty(int absoluteIndex, int relativeIndex, PredictionPerformance performance) {
        this.absoluteIndex = absoluteIndex;
        this.relativeIndex = relativeIndex;
        this.performance = performance;
    }

    @Override
    public String getDescription() {
        return CdkFingerprintVersion.getComplete().getMolecularProperty(absoluteIndex).getDescription();
    }
}
