package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.msviewer.data.MSViewerDataModel;
import de.unijena.bioinf.sirius.gui.msviewer.data.PeakInformation;

import java.util.TreeMap;

public class SpectrumContainer implements MSViewerDataModel {

	private CompactSpectrum sp;
	
	private TreeMap<Double,Integer> massToIndex;
	
	private SiriusMSViewerPeak[] peaks;

	protected double maxMass;

	public SpectrumContainer(CompactSpectrum sp) {
		this.sp = sp;
		
		//Zwischenschritt da Spektren denkbar bei denen es mehrere Peaks mit gleicher Masse geben kann...
		
		TreeMap<Double,SiriusMSViewerPeak> map = new TreeMap<>();
		double maxInt = 0;
		maxMass = 0d;
		for(int i=0;i<sp.getSize();i++){
			SiriusMSViewerPeak peak = new SiriusMSViewerPeak();
			peak.setMass(sp.getMass(i));
			peak.setAbsoluteIntensity(sp.getAbsoluteIntensity(i));
			maxMass = Math.max(peak.getMass(), maxMass);
			if(maxInt<peak.getAbsoluteIntensity()) maxInt = peak.getAbsoluteIntensity();
			if(map.containsKey(peak.getMass())){
				if(map.get(peak.getMass()).getAbsoluteIntensity()<peak.getAbsoluteIntensity()){
					map.put(peak.getMass(), peak);
				}
			}else{
				map.put(peak.getMass(), peak);
			}
		}
		
		peaks = new SiriusMSViewerPeak[map.size()];
		
		int index = 0;
		for(Double mass : map.keySet()){
			peaks[index] = map.get(mass);
			peaks[index].setRelativeIntensity(peaks[index].getAbsoluteIntensity()/maxInt);
			index++;
		}
		
		massToIndex = new TreeMap<>();
		for(int i=0;i<peaks.length;i++){
			SiriusMSViewerPeak peak = peaks[i];
			massToIndex.put(peak.getMass(),i);
		}
		
		
	}
	
	public CompactSpectrum getSpectrum(){
		return sp;
	}

	@Override
	public int findIndexOfPeak(double mass, double tolerance) {
		double smallerMass = massToIndex.floorKey(mass);
		double biggerMass = massToIndex.ceilingKey(mass);
		if(smallerMass==mass)return massToIndex.get(mass);
		double diff1 = mass-smallerMass;
		double diff2 = biggerMass-mass;
		if(diff1<=diff2){
			if(diff1>tolerance) return -1;
			else return massToIndex.get(smallerMass);
		}else{
			if(diff2>tolerance) return -1;
			else return massToIndex.get(biggerMass);
		}
	}

	@Override
	public double getAbsoluteIntensity(int index) {
		return peaks[index].getAbsoluteIntensity();
	}

	@Override
	public int getIndexWithMass(double mass) {
		if(massToIndex.containsKey(mass)){
			return massToIndex.get(mass);
		}else{
			return -1;
		}
	}

	@Override
	public PeakInformation getInformations(int index) {
		return peaks[index];
	}

	@Override
	public int[] getIsotopePeaks(int index) {
		return new int[0];
	}

	@Override
	public String getLabel() {
		return "";
	}

	@Override
	public double getMass(int index) {
		return peaks[index].getMass();
	}

	@Override
	public double getRelativeIntensity(int index) {
		return peaks[index].getRelativeIntensity();
	}

//	@Override
//	public double getSignalNoise(int index) {
//		return peaks[index].getSignalToNoise();
//	}

	@Override
	public double minMz() {
		return 0;
	}

	@Override
	public double maxMz() {
		return maxMass;
	}

	@Override
	public int getSize() {
		return peaks.length;
	}

	@Override
	public boolean isIsotope(int arg0) {
		return false;
	}

	@Override
	public boolean isMarked(int arg0) {
		return false;
	}

	@Override
	public boolean isPlusZeroPeak(int arg0) {
		return false;
	}

	@Override
	public String getMolecularFormula(int arg0) {
		return null;
	}

	@Override
	public boolean isImportantPeak(int index) {
		return false;
	}

	@Override
	public boolean isUnimportantPeak(int arg0) {
		// TODO Auto-generated method stub
		return false;
	}

}
