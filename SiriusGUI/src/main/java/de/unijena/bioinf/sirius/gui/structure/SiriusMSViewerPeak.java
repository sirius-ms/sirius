package de.unijena.bioinf.sirius.gui.structure;

import java.util.Collections;
import java.util.List;

import de.unijena.bioinf.myxo.gui.msview.data.MolecularFormulaInformation;
import de.unijena.bioinf.myxo.gui.msview.data.PeakInformation;

public class SiriusMSViewerPeak implements PeakInformation{
	
	private double absInt, relInt, mass, sn;
//	private 
	
	public SiriusMSViewerPeak() {
		absInt = 0;
		relInt = 0;
		mass = 0;
		sn = 0;
	}
	
	

	public void setAbsoluteIntensity(double absInt) {
		this.absInt = absInt;
	}



	public void setRelativeIntensity(double relInt) {
		this.relInt = relInt;
	}



	public void setMass(double mass) {
		this.mass = mass;
	}



	public void setSn(double sn) {
		this.sn = sn;
	}
	
	@Override
	public double getAbsoluteIntensity() {
		return absInt;
	}

	@Override
	public List<MolecularFormulaInformation> getDecompositions() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public double getMass() {
		return mass;
	}

	@Override
	public double getRelativeIntensity() {
		return relInt;
	}

	@Override
	public double getSignalToNoise() {
		return sn;
	}

	@Override
	public boolean isIsotope() {
		return false;
	}
	
}
