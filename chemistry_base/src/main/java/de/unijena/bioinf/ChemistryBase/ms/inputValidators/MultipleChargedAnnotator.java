package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.DatasetStatistics;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Dataset;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumProperty;

import java.util.List;

public class MultipleChargedAnnotator implements QualityAnnotator {
    @Override
    public SpectrumProperty getPropertyToAnnotate() {
        return null;
    }

    @Override
    public List<SpectrumProperty> getPrerequisites() {
        return null;
    }

    @Override
    public void prepare(DatasetStatistics statistics) {

    }

    @Override
    public void annotate(Ms2Dataset dataset) {
        throw new UnsupportedOperationException("not implemented, yet.");
    }
}
