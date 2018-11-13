package de.unijena.bioinf.ChemistryBase.ms.inputValidators;

import de.unijena.bioinf.ChemistryBase.ms.DatasetStatistics;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Dataset;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumProperty;

import java.util.List;

public interface QualityAnnotator {

    public SpectrumProperty getPropertyToAnnotate();

    public List<SpectrumProperty> getPrerequisites();

    public void prepare(DatasetStatistics statistics);

    public void annotate(Ms2Dataset dataset);
}
