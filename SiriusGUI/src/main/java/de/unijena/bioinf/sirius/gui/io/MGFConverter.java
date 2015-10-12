package de.unijena.bioinf.sirius.gui.io;

import java.util.*;
import java.io.*;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.myxo.structure.DefaultCompactExperiment;
import de.unijena.bioinf.myxo.structure.DefaultCompactSpectrum;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;

public class MGFConverter {

	public MGFConverter() {
	}
	
	public ExperimentContainer convert(File path){
		
		ExperimentContainer ec = new ExperimentContainer();
		
		try(BufferedReader reader = new BufferedReader(new FileReader(path))){
			MgfParser parser = new MgfParser();
			Ms2Experiment rawExp = parser.parse(reader);
			de.unijena.bioinf.ChemistryBase.chem.Ionization ion = rawExp.getIonization();
			if(ion==null){
				ec.setIonization(Ionization.Unknown);
			}else{
				int charge = ion.getCharge();
				if(charge>0){
					ec.setIonization(Ionization.MPlusH);
				}else if(charge<0){
					ec.setIonization(Ionization.MMinusH);
				}else{
					ec.setIonization(Ionization.M);
				}
			}
			String name = path.getName();
			ec.setName(name.substring(0,name.length()-4));
			ec.setDataFocusedMass(rawExp.getIonMass());
			
			if(rawExp.getMs1Spectra()!=null&&rawExp.getMs1Spectra().size()>0){
				Spectrum<Peak> ms1 = rawExp.getMs1Spectra().get(0);
				double[] masses = new double[ms1.size()];
				double[] absInts = new double[ms1.size()];
				for(int i=0;i<ms1.size();i++){
					masses[i] = ms1.getMzAt(i);
					absInts[i] = ms1.getIntensityAt(i);
				}
				DefaultCompactSpectrum sp = new DefaultCompactSpectrum(masses, absInts);
				sp.setMSLevel(1);
				ArrayList<CompactSpectrum> ms1Spectra = new ArrayList<>();
				ms1Spectra.add(sp);
				ec.setMs1Spectra(ms1Spectra);
			}else{
				ec.setMs1Spectra(new ArrayList<CompactSpectrum>());
			}
			
			List<? extends Ms2Spectrum<? extends Peak>> ms2s = rawExp.getMs2Spectra();
			if(ms2s!=null){
				ArrayList<CompactSpectrum> ms2Spectra = new ArrayList<>();
				for(Ms2Spectrum<? extends Peak> ms2 : ms2s){
					
					double[] masses = new double[ms2.size()];
					double[] absInts = new double[ms2.size()];
					
					for(int i=0;i<ms2.size();i++){
						masses[i] = ms2.getMzAt(i);
						absInts[i] = ms2.getIntensityAt(i);
					}
					
					DefaultCompactSpectrum ms2Spectrum = new DefaultCompactSpectrum(masses, absInts);
					ms2Spectrum.setCollisionEnergy(ms2.getCollisionEnergy());
					ms2Spectrum.setMSLevel(2);
					
					ms2Spectra.add(ms2Spectrum);
				}
				ec.setMs2Spectra(ms2Spectra);
			}else{
				ec.setMs2Spectra(new ArrayList<CompactSpectrum>());
			}
			
			
			ec.setIonization(de.unijena.bioinf.sirius.gui.mainframe.Ionization.Unknown);
			rawExp.getMergedMs1Spectrum();
			
		}catch(IOException e){
			throw new RuntimeException(e);
		}
		
		return ec;
	}

}
