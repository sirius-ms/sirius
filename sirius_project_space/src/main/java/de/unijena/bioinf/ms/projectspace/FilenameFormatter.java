package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

public interface FilenameFormatter {

    public String formatName(ExperimentResult experimentResult, int index);


}
