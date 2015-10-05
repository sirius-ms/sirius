package de.unijena.bioinf.sirius.gui.load;

import gnu.trove.list.array.TDoubleArrayList;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;

import org.apache.commons.collections.map.HashedMap;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.babelms.mgf.MgfParser;
import de.unijena.bioinf.myxo.io.spectrum.CSVFormatReader;
import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.dialogs.ErrorListDialog;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedDataFormatsFilter;
import de.unijena.bioinf.sirius.gui.io.DataFormat;
import de.unijena.bioinf.sirius.gui.io.DataFormatIdentifier;
import de.unijena.bioinf.sirius.gui.io.JenaMSConverter;
import de.unijena.bioinf.sirius.gui.io.MGFConverter;
import de.unijena.bioinf.sirius.gui.mainframe.Ionization;
import de.unijena.bioinf.sirius.gui.structure.CSVToSpectrumConverter;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

public class LoadController implements LoadDialogListener{

	LoadDialog loadDialog;
	
	private ExperimentContainer exp;
	
	private ReturnValue returnValue;
	
	private ConfigStorage config;
	
	private JFrame owner;
	
	public LoadController(JFrame owner,ExperimentContainer exp, ConfigStorage config) {
		
		returnValue = ReturnValue.Abort;
		
		this.owner = owner;
		
		this.exp = exp;
		this.config = config;
		
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
//		loadDialog.showDialog();
	}
	
	public void showDialog(){
		loadDialog.showDialog();
	}
	
	public LoadController(JFrame owner, ConfigStorage config) {
		this(owner,new ExperimentContainer(),config);
	}

	@Override
	public void addSpectra() {
		JFileChooser chooser = new JFileChooser(config.getDefaultLoadDialogPath());
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(true);
		chooser.addChoosableFileFilter(new SupportedDataFormatsFilter());
		chooser.setAcceptAllFileFilterUsed(false);
		int returnVal = chooser.showOpenDialog((JDialog)loadDialog);
		if(returnVal == JFileChooser.APPROVE_OPTION){
			
			File[] files = chooser.getSelectedFiles();
			
			//setzt Pfad
			config.setDefaultLoadDialogPath(files[0].getParentFile());
			
			//untersuche die Dateitypen und schaue ob CSV vorhanden, wenn vorhanden behandelte alle CSVs auf
			//gleiche Weise
			
			importSpectra(files);
			
		}
		
		
	}
	
	private void importSpectra(File[] files){
		
		DataFormatIdentifier dfi = new  DataFormatIdentifier();
		List<File> csvFiles = new ArrayList<>();
		List<File> msFiles = new ArrayList<>();
		List<File> mgfFiles = new ArrayList<>();
//		File firstCSV = null;
		for(File file : files){
			DataFormat df = dfi.identifyFormat(file);
			if(df==DataFormat.CSV){
				csvFiles.add(file);
			}else if(df==DataFormat.JenaMS){
				msFiles.add(file);
			}else if(df==DataFormat.MGF){
				mgfFiles.add(file);
			}
		}
		
		importSpectra(csvFiles, msFiles, mgfFiles);
	}
		
	
	private void importSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles){
		
		List<String> errorStorage = new ArrayList<>();
		
		CSVDialogReturnContainer cont = null;
		CSVFormatReader csvReader = new CSVFormatReader();
		
		if(csvFiles.size()>0){
			
			HashMap<Integer,List<List<TDoubleArrayList>>> columnNumberToData = new HashMap<>();
			
			for(File file : csvFiles){
				try{
					List<TDoubleArrayList> data = csvReader.readCSV(file);
					Integer key = data.get(0).size();
					if(columnNumberToData.containsKey(key)){
						columnNumberToData.get(key).add(data);
					}else{
						List<List<TDoubleArrayList>> list = new ArrayList<>();
						list.add(data);
						columnNumberToData.put(key, list);
					}
				}catch(Exception e){
					errorStorage.add(file.getName()+": Invalid file format.");
				}
			}
			
			if(columnNumberToData.size()>0){
				for(Integer key : columnNumberToData.keySet()){
					List<List<TDoubleArrayList>> list = columnNumberToData.get(key);
					if(list.size()==1){
						CSVDialog diag = new CSVDialog((JDialog)loadDialog,list.get(0),false);
						if(diag.getReturnValue() == ReturnValue.Success){
							cont = diag.getResults();
							CSVToSpectrumConverter conv = new CSVToSpectrumConverter();
							CompactSpectrum sp = conv.convertCSVToSpectrum(list.get(0), cont);
							if(sp.getMSLevel()==1){
								if(exp.getMs1Spectra().size()==0){
									this.exp.getMs1Spectra().add(sp);
								}else{
									sp.setMSLevel(2);
									this.exp.getMs2Spectra().add(sp);
								}
								this.loadDialog.spectraAdded(sp);
							}else{
								this.exp.getMs2Spectra().add(sp);
								this.loadDialog.spectraAdded(sp);
							}
						}else{
							return; //breche ab
						}
					}else{
						CSVDialog diag = new CSVDialog((JDialog)loadDialog,list.get(0),true);
						if(diag.getReturnValue() == ReturnValue.Success){
							cont = diag.getResults();
							cont.setMaxEnergy(-1);
							cont.setMinEnergy(-1);
							cont.setMsLevel(2);
							
							for(List<TDoubleArrayList> data : list){
								CSVToSpectrumConverter conv = new CSVToSpectrumConverter();
								CompactSpectrum sp = conv.convertCSVToSpectrum(data, cont);
								this.exp.getMs2Spectra().add(sp);
								loadDialog.spectraAdded(sp);
							}
							
						}else{
							
						}
					}
				}
			}
		}
			
		if(msFiles.size()>0){
			for(File file : msFiles){
				ExperimentContainer ec = null;
				JenaMSConverter conv = new JenaMSConverter();
				try{
					ec = conv.convert(file);
				}catch(Exception e){
					errorStorage.add(file.getName()+": Invalid file format.");
					continue;
				}
				
				importExperimentContainer(ec,errorStorage);
				
			}
		}
			
		if(mgfFiles.size()>0){
			for(File file : mgfFiles){
				MGFConverter conv = new MGFConverter();
				ExperimentContainer ec = null;
				
				try{
					ec = conv.convert(file);
				}catch(RuntimeException e2){
					errorStorage.add(file.getName()+": Invalid file format.");
					continue;
				}
				
				importExperimentContainer(ec,errorStorage);
			}
		}
		
		if(errorStorage.size()>1){
			ErrorListDialog elDiag = new ErrorListDialog(this.owner, errorStorage);
		}else if(errorStorage.size()==1){
			ExceptionDialog eDiag = new ExceptionDialog(this.owner, errorStorage.get(0)); 
		}
		
	}
	
	public void importExperimentContainer(ExperimentContainer ec, List<String> errorStorage){
		if(exp.getIonization()==Ionization.Unknown && ec.getIonization()!=Ionization.Unknown){
			exp.setIonization(ec.getIonization());
		}
		
		if(exp.getName()==null || exp.getName().isEmpty()){
			String name = ec.getName();
			if(name!=null&&!name.isEmpty()){
				this.exp.setName(name);
				loadDialog.experimentNameChanged(this.exp.getName());
			}
		}
		
		List<CompactSpectrum> newSP = new ArrayList<>();
		
		double ecFM = ec.getDataFocusedMass();
		if(exp.getDataFocusedMass()<=0 && ecFM>0){
			exp.setDataFocusedMass(ecFM);
		}
		
		if(ec.getMs1Spectra().size()>0){
			CompactSpectrum ms1 = ec.getMs1Spectra().get(0);
			
			if(ms1!=null){
				if(this.exp.getMs1Spectra().isEmpty()){
					this.exp.getMs1Spectra().add(ms1);
					ms1.setMSLevel(1);
					newSP.add(ms1);
//					if(exp.getDataFocusedMass()<=0){
//						double focusedMass = ec.getDataFocusedMass();
//						if(focusedMass>0){
//							this.exp.setDataFocusedMass(focusedMass);
//						}
//					}
				}else{
					this.exp.getMs2Spectra().add(ms1);
					ms1.setMSLevel(2);
					newSP.add(ms1);
				}
			}
		}
		
		
		for(CompactSpectrum sp : ec.getMs2Spectra()){
			this.exp.getMs2Spectra().add(sp);
			newSP.add(sp);
		}
		for(CompactSpectrum sp : newSP){
			loadDialog.spectraAdded(sp);
		}
	}
	
	public ExperimentContainer getExperiment(){
		return this.exp;
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
		}else{
			System.err.println("unexpected ms level: "+sp.getMSLevel());
		}
		
	}

	@Override
	public void abortProcess() {
		this.returnValue = ReturnValue.Abort;
	}

	@Override
	public void completeProcess() {
		if(this.exp.getMs1Spectra().isEmpty()&&this.exp.getMs2Spectra().isEmpty()) this.returnValue = ReturnValue.Abort;
		else this.returnValue = ReturnValue.Success;
	}

	@Override
	public void changeCollisionEnergy(CompactSpectrum sp) {
		double oldMin,oldMax;
		if(sp.getCollisionEnergy()==null){
			oldMin = 0;
			oldMax = 0;
		}else{
			oldMin = sp.getCollisionEnergy().getMinEnergy();
			oldMax = sp.getCollisionEnergy().getMaxEnergy();
		}
		
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
		if(sp.getMSLevel()==msLevel){
			return;
		}
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

	@Override
	public void addSpectra(List<File> files) {
		File[] fileArr = new File[files.size()];
		importSpectra(files.toArray(fileArr));
		
	}
	
	public void addSpectra(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles){
		importSpectra(csvFiles, msFiles, mgfFiles);
	}

}
