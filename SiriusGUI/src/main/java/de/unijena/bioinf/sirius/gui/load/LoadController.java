package de.unijena.bioinf.sirius.gui.load;

import gnu.trove.list.array.TDoubleArrayList;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.myxo.io.spectrum.CSVNumberReader;
import de.unijena.bioinf.myxo.io.spectrum.DataFormat;
import de.unijena.bioinf.myxo.io.spectrum.DataFormatIdentifier;
import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

public class LoadController implements LoadDialogListener{

	LoadDialog loadDialog;
	
//	private List<CompactSpectrum> spectra; 
	
//	private double focMass;
//	private Ionization ionization;
//	private String compoundName;
	
	private ExperimentContainer exp;
	
	private ReturnValue returnValue;
	
	public LoadController(JFrame owner,ExperimentContainer exp) {
//		this.spectra = new ArrayList<>();
//		if(exp.getMs1Spectra()!=null){
//			spectra.addAll(exp.getMs1Spectra());
//		}
//		
//		if(exp.getMs2Spectra()!=null){
//			spectra.addAll(exp.getMs2Spectra());
//		}
		
		returnValue = ReturnValue.Abort;
		
//		this.focMass = exp.getFocusedMass();
//		this.ionization = exp.getIonization();
//		this.compoundName = exp.getName();
		
		this.exp = exp;
		
		loadDialog = new DefaultLoadDialog(owner);
		
		List<CompactSpectrum> ms1Spectrum = this.exp.getMs1Spectra();
		List<CompactSpectrum> ms2Spectrum = this.exp.getMs2Spectra();
		
		if(ms1Spectrum!=null){
			for(CompactSpectrum spectrum : ms1Spectrum){
				loadDialog.spectraAdded(spectrum);
			}
		}
		
		if(ms2Spectrum!=null){
			for(CompactSpectrum spectrum : ms2Spectrum){
				loadDialog.spectraAdded(spectrum);
			}
		}
		
		
		
		loadDialog.addLoadDialogListener(this);
		if(this.exp.getName()!=null||this.exp.getName().isEmpty()) loadDialog.experimentNameChanged(this.exp.getName());
		loadDialog.showDialog();
	}
	
	public LoadController(JFrame owner) {
		this(owner,new ExperimentContainer());
	}
	
//	public LoadController(JFrame owner,double focMass,Ionization ionization,String compoundName,List<CompactSpectrum> spectra) {
//		
//		returnValue = ReturnValue.Abort;
//		
//		this.focMass = focMass;
//		this.ionization = ionization;
//		this.compoundName = compoundName;
//		
//		if(spectra==null){
//			this.spectra = new ArrayList<CompactSpectrum>();
//		}else{
//			this.spectra = spectra;
//		}
//		
//		loadDialog = new DefaultLoadDialog(owner);
//		
//		for(CompactSpectrum spectrum : this.spectra){
//			loadDialog.spectraAdded(spectrum);
//		}
//		
//		loadDialog.addLoadDialogListener(this);
//		if(this.compoundName!=null&&!this.compoundName.isEmpty()) loadDialog.experimentNameChanged(this.compoundName);
//		loadDialog.showDialog();
//	}

	@Override
	public void addSpectra() {
		JFileChooser chooser = new JFileChooser(new File("/media/Ext4_log/gnps/gnps_ms/"));
		int returnVal = chooser.showOpenDialog((JDialog)loadDialog);
		if(returnVal == JFileChooser.APPROVE_OPTION){
			File f = chooser.getSelectedFile();
			int dotIndex = f.getName().lastIndexOf(".");
			if(dotIndex>0){
				String ending = f.getName().substring(dotIndex+1, f.getName().length());
				DataFormatIdentifier dfi = new  DataFormatIdentifier();
				DataFormat df = dfi.identifyFormat(f);
				
				if(df==DataFormat.JenaMS){
					MS2FormatSpectraReader reader = new MS2FormatSpectraReader();
					CompactExperiment cexp = reader.read(f);
					String ion = cexp.getIonization();
					if(ion!=null && !ion.isEmpty() &&this.exp.getIonization()==Ionization.Unknown){
						if(ion.contains("[M+H]+")){
							this.exp.setIonization(Ionization.MPlusH);
						}else if(ion.contains("[M+Na]+")){
							this.exp.setIonization(Ionization.MPlusNa);
						}else if(ion.contains("[M-H]-")){
							this.exp.setIonization(Ionization.MMinusH);
						}else if(ion.contains("M+")){
							this.exp.setIonization(Ionization.M);
						}else{
							this.exp.setIonization(Ionization.Unknown);
						}
					}
					double focusedMass = cexp.getFocusedMass();
					if(focusedMass>0){
						this.exp.setDataFocusedMass(focusedMass);
					}
					String name = cexp.getCompoundName();
					if(name!=null&&!name.isEmpty()&&(this.exp.getName()==null||this.exp.getName().isEmpty())){
						this.exp.setName(name);
						loadDialog.experimentNameChanged(this.exp.getName());
					}
					CompactSpectrum ms1 = cexp.getMS1Spectrum();
					List<CompactSpectrum> newSP = new ArrayList<>();
					if(ms1!=null){
						if(this.exp.getMs1Spectra().isEmpty()){
							this.exp.getMs1Spectra().add(ms1);
							newSP.add(ms1);
						}else{
							this.exp.getMs2Spectra().add(ms1);
						}
					}
					
					for(CompactSpectrum sp : cexp.getMS2Spectra()){
						this.exp.getMs2Spectra().add(sp);
						newSP.add(sp);
					}
					for(CompactSpectrum sp : newSP){
						loadDialog.spectraAdded(sp);
					}
				}else if(df==DataFormat.CSV){
					CSVNumberReader reader = new CSVNumberReader();
					List<TDoubleArrayList> data = reader.readCSV(f);
					CSVDialog diag = new CSVDialog((JDialog)loadDialog,data);
					if(diag.getReturnValue() == ReturnValue.Success){
						CompactSpectrum sp = diag.getSpectrum();
						this.exp.getMs2Spectra().add(sp);
						loadDialog.spectraAdded(sp);
					}
				}
			}
			
			
		}
		
		
	}
	
	public ExperimentContainer getExperiment(){
		return this.exp;
//		ExperimentContainer cont = new ExperimentContainer();
//		List<CompactSpectrum> ms1Spectra = new ArrayList<>();
//		List<CompactSpectrum> ms2Spectra = new ArrayList<>();
//		for(CompactSpectrum sp : spectra){
//			if(sp.getMSLevel()==1){
//				ms1Spectra.add(sp);
//			}else{
//				ms2Spectra.add(sp);
//			}
//		}
//		cont.setMs1Spectra(ms1Spectra);
//		cont.setMs2Spectra(ms2Spectra);
//		if(compoundName!=null) cont.setName(compoundName);
//		if(focMass>0) cont.setFocusedMass(focMass);
//		if(ionization!=null) cont.setIonization(ionization);
//		return cont;
	}
	
	public ReturnValue getReturnValue(){
		return this.returnValue;
	}

	@Override
	public void removeSpectrum(CompactSpectrum sp) {
		if(sp.getMSLevel()==1){
			exp.getMs1Spectra().remove(sp);
			this.loadDialog.spectraRemoved(sp);
		}else if(sp.getMSLevel()==2){
			exp.getMs2Spectra().remove(sp);
			this.loadDialog.spectraRemoved(sp);
		}
		
	}

	@Override
	public void abortProcess() {
		this.returnValue = ReturnValue.Abort;
	}

	@Override
	public void completeProcess() {
		this.returnValue = ReturnValue.Success;
	}

	@Override
	public void changeCollisionEnergy(CompactSpectrum sp) {
		double oldMin = sp.getCollisionEnergy().getMinEnergy();
		double oldMax = sp.getCollisionEnergy().getMaxEnergy();
		CollisionEnergyDialog ced = new CollisionEnergyDialog((JDialog) loadDialog, oldMin, oldMax);
		if(ced.getReturnValue() == ReturnValue.Success){
			double newMin = ced.getMinCollisionEnergy();
			double newMax = ced.getMaxCollisionEnergy();
			if(oldMin!=newMin || oldMax!=newMax){
				sp.setCollisionEnergy(new CollisionEnergy(newMin,newMax));
				loadDialog.newCollisionEnergy(sp);
			}
		}
	}

	@Override
	public void changeMSLevel(CompactSpectrum sp, int msLevel) {
		System.out.println("Change: "+sp.getSize()+" "+msLevel);
		sp.setMSLevel(msLevel);
		List<CompactSpectrum> ms1Spectra = this.exp.getMs1Spectra();
		List<CompactSpectrum> ms2Spectra = this.exp.getMs2Spectra();
		if(msLevel==1){
			if(ms1Spectra.isEmpty()){
				ms2Spectra.remove(sp);
				ms1Spectra.add(sp);
				loadDialog.msLevelChanged(sp);
			}else{
				CompactSpectrum oldMS1 = ms1Spectra.get(0);
				oldMS1.setMSLevel(2);
				ms2Spectra.add(oldMS1);
				ms2Spectra.remove(sp);
				ms1Spectra.remove(oldMS1);
				ms1Spectra.add(sp);
				loadDialog.msLevelChanged(sp);
				loadDialog.msLevelChanged(oldMS1);
				
			}
		}else{
			ms1Spectra.remove(sp);
			ms2Spectra.add(sp);
			loadDialog.msLevelChanged(sp);
		}
	}

	@Override
	public void experimentNameChanged(String name) {
		if(name!=null && !name.isEmpty()){
			this.exp.setName(name);
			loadDialog.experimentNameChanged(this.exp.getName());
		}
	}

}
