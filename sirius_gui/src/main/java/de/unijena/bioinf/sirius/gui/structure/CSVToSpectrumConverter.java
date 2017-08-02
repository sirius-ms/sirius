package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.load.CSVDialogReturnContainer;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.List;

public class CSVToSpectrumConverter {
	
	public CompactSpectrum convertCSVToSpectrum(List<TDoubleArrayList> data, CSVDialogReturnContainer cont){
		return convertCSVToSpectrum(data, cont.getMassIndex(), cont.getIntIndex(), cont.getMinEnergy(), cont.getMaxEnergy(), cont.getMsLevel());
	}

	public CompactSpectrum convertCSVToSpectrum(List<TDoubleArrayList> data, int massIndex, int absIntIndex, double minEnergy, double maxEnergy, int msLevel){
		int rowNumber = data.size();
		int columnNumber = data.get(0).size();
		double[] masses = new double[rowNumber];
		double[] ints = new double[rowNumber];
		
		for(int i=0;i<rowNumber;i++){
			masses[i] = data.get(i).get(massIndex);
			ints[i] = data.get(i).get(absIntIndex);
		}
		
//		for(int i=0;i<columnNumber;i++){
//			for(int j=0;j<rowNumber;j++){
//				mass
//				dtm.setValueAt(data.get(j).get(i), j, i);
//			}
//		}
//		
//		for(int i=0;i<size;i++){
//			masses[i] = data.get(i).get(massIndex);
//			ints[i] = data.get(i).get(absIntIndex);
//		}

		CompactSpectrum sp = new CompactSpectrum(masses,ints);
		sp.setMSLevel(msLevel);
		if(minEnergy>0 && maxEnergy>0 && minEnergy<=maxEnergy) sp.setCollisionEnergy(new CollisionEnergy(minEnergy,maxEnergy));
		return sp;
	}

}
