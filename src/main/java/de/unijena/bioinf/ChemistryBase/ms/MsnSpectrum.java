package de.unijena.bioinf.ChemistryBase.ms;

import java.util.List;

public interface MsnSpectrum<P extends Peak> extends Ms2Spectrum<P> {

    public List<? extends MsnSpectrum<P>> getChildSpectra();

    public int getMsLevel();

}
