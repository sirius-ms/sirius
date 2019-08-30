package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;

public interface FilenameFormatter {


    public String formatName(ExperimentResult experimentResult, int index);

    public String getFormatExpression();

}
