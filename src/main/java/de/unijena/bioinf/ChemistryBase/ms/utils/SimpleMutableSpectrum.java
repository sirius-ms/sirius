package de.unijena.bioinf.ChemistryBase.ms.utils;

import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;

public class SimpleMutableSpectrum extends BasicMutableSpectrum<Peak>{

	public <S extends Spectrum<? extends Peak>> SimpleMutableSpectrum(S immutable) {
		super(immutable);
	}
	
	public SimpleMutableSpectrum() {
		super();
	}

	@Override
	public Peak getPeakAt(int index) {
		return new Peak(masses.get(index), intensities.get(index));
	}

}
