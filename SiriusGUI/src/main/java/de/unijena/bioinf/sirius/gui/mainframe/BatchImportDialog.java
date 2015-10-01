package de.unijena.bioinf.sirius.gui.mainframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IsotopePatternHandling;
import de.unijena.bioinf.sirius.Progress;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.io.DataFormat;
import de.unijena.bioinf.sirius.gui.io.DataFormatIdentifier;
import de.unijena.bioinf.sirius.gui.io.JenaMSConverter;
import de.unijena.bioinf.sirius.gui.io.MGFConverter;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

public class BatchImportDialog extends JDialog implements ActionListener{
	
	private JProgressBar progBar;
//	private double minVal, maxVal;
	ImportExperimentsThread importThread ;
//	private Thread t;
	private Lock lock;
	private JButton abort;
//	private boolean abortPressed;
	private JLabel progressl;
	private ReturnValue rv;
	private AnalyseFileTypesThread analyseThread;
	private List<String> errors;

	public BatchImportDialog(JFrame owner) {
		super(owner,true);
		this.setTitle("Batch Import");
		this.lock = new ReentrantLock(true);
		
		this.rv = ReturnValue.Abort;
		this.setLayout(new BorderLayout());
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		this.add(centerPanel, BorderLayout.CENTER);
		
		progBar = new JProgressBar(JProgressBar.HORIZONTAL,0,100);
		progBar.setValue(0);
		
		JPanel progressPanel = new JPanel(new BorderLayout());
		progressPanel.add(progBar,BorderLayout.NORTH);
		progressl = new JLabel("");
		JPanel progressLabelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		progressLabelPanel.add(progressl);
		progressPanel.add(progressLabelPanel,BorderLayout.SOUTH);
		progressPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
//		progressPanel.setBorder(BorderFactory.createEtchedBorder());
		centerPanel.add(progressPanel,BorderLayout.NORTH);
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
		abort = new JButton("Abort");
		abort.addActionListener(this);
		buttonPanel.add(abort);
		this.add(buttonPanel, BorderLayout.SOUTH);
		
//		abortPressed = false;
	}
	
	public void start(List<File> msFiles, List<File> mgfFiles){
		errors = new ArrayList<>();
		importThread = new ImportExperimentsThread(msFiles,mgfFiles, this,errors);
		Thread t = new Thread(importThread);
		t.start();
		this.setSize(new Dimension(300,125));
		this.setVisible(true);
	}
	
	public void start(File[] files){
		errors = new ArrayList<>();
		analyseThread = new AnalyseFileTypesThread(files, this, errors);
		Thread thread = new Thread(analyseThread);
		thread = new Thread(analyseThread);
		thread.start();
		this.setSize(new Dimension(300,125));
		this.setVisible(true);
	}
	
	public List<ExperimentContainer> getResults(){
		if(this.rv == ReturnValue.Abort) return new ArrayList<>();
		else return this.importThread.getResults();
	}
	
	public List<String> getErrors(){
		return this.errors;
	}
	
	public ReturnValue wasSucessful(){
		return this.rv;
	}
	
	/////// File import ///////////

	void fileImportInit(int maxVal) {
		lock.lock();
		progBar.setMaximum(maxVal);
		progBar.setMinimum(0);
		progBar.setValue(0);
		progressl.setText("");
		lock.unlock();
	}

	void fileImportUpdate(int currentIndex, String currentFileName) {
		lock.lock();
		progBar.setValue(currentIndex);
		progressl.setText("import \""+currentFileName+"\"");
		lock.unlock();		
	}
	
	void fileImportFinished() {
		this.rv = ReturnValue.Success;
		this.dispose();
	}
	
	void fileImportAborted(){
		this.rv = ReturnValue.Abort;
		this.dispose();
	}
	
	///////////// fuer DF Analyse ////////////////////
	
	void fileAnalysisInit(int maxVal) {
		lock.lock();
		progBar.setMaximum(maxVal);
		progBar.setMinimum(0);
		progBar.setValue(0);
		progressl.setText("Analysing data formats ...");
		lock.unlock();
	}

	
	void FileAnaysisUpdate(int currentIndex) {
		lock.lock();
		progBar.setValue(currentIndex);
		lock.unlock();		
	}
	
	void fileAnalysisAborted(){
		this.rv = ReturnValue.Abort;
		this.dispose();
	}
	
	void fileAnalysisFinished() {
		List<File> msFiles = this.analyseThread.getMSFiles();
		List<File> mgfFiles = this.analyseThread.getMGFFiles();
		
		this.importThread = new ImportExperimentsThread(msFiles,mgfFiles, this, this.errors);
		Thread t = new Thread(importThread);
		t.start();
	}
	
	
	//// Action Listener ////
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==this.abort){
//			abortPressed = true;
			if(analyseThread!=null) analyseThread.abortProgress();
			if(importThread!=null) importThread.abortImport();
			this.dispose();
		}
		
	}

}

class AnalyseFileTypesThread implements Runnable{
	
	private File[] files;
	private List<File> msFiles, mgfFiles;
	private ReentrantLock lock;
	private boolean stop;
	private BatchImportDialog bid;
	private List<String> errors;
	
	
	public AnalyseFileTypesThread(File[] files,BatchImportDialog bid, List<String> errors) {
		this.files = files;
		this.bid = bid;
		stop = false;
		lock = new ReentrantLock();
		msFiles = new ArrayList<>();
		mgfFiles = new ArrayList<>();
		this.errors = errors;
	}	
	
	void abortProgress(){
		lock.lock();
		this.stop = true;
		lock.unlock();
	}
	
	List<File> getMSFiles(){
		return this.msFiles;
	}
	
	List<File> getMGFFiles(){
		return this.mgfFiles;
	}

	@Override
	public void run() {
		DataFormatIdentifier dfi = new  DataFormatIdentifier();
		int counter = 0;
		this.bid.fileAnalysisInit(files.length);
		for(File f : files){
			lock.lock();
			if(this.stop){
				this.bid.fileAnalysisAborted();
				return;
			}
			lock.unlock();
			this.bid.FileAnaysisUpdate(counter);
			DataFormat df = dfi.identifyFormat(f);
			if(df==DataFormat.JenaMS){
				msFiles.add(f);
			}else if(df==DataFormat.MGF){
				mgfFiles.add(f);
			}else if(df==DataFormat.NotSupported){
				this.errors.add(f.getName()+": unsupported file format.");
			}
			counter++;
		}
		
		bid.fileAnalysisFinished();
		
	}
	
}

class ImportExperimentsThread implements Runnable{
	
	private List<File> msFiles, mgfFiles;
	private List<ExperimentContainer> results;
	private boolean stop;
	private ReentrantLock lock;
	private List<String> errors;
	private BatchImportDialog bid;
	
	ImportExperimentsThread(List<File> msFiles, List<File> mgfFiles, BatchImportDialog bid, List<String> errors){
		this.msFiles = msFiles;
		this.mgfFiles = mgfFiles;
		this.results = new ArrayList<>();
		this.errors = errors;
		this.stop = false;
		this.lock = new ReentrantLock(true);
		this.bid = bid;
	}
	
	void abortImport(){
		lock.lock();
		this.stop = true;
		lock.unlock();
	}

	@Override
	public void run() {
		int size = msFiles.size()+mgfFiles.size();
		this.results = new ArrayList<>(size);
		this.bid.fileImportInit(size);
		int counter=0;
		for(File msFile : msFiles){
			this.bid.fileImportUpdate(counter, msFile.getName());
			boolean trigger;
			lock.lock(); //TODO Lock vermutlich ueberfluessig, da nur eine atomare Operation
			trigger = this.stop;
			lock.unlock();
			if(trigger){
				bid.fileImportAborted();
				return;
			}
			ExperimentContainer ec = readMSCompound(msFile);
			if(ec!=null) results.add(ec);
			counter++;
		}
		
		for(File mgfFile : mgfFiles){
			boolean trigger;
			lock.lock(); //TODO Lock vermutlich ueberfluessig, da nur eine atomare Operation
			trigger = this.stop;
			lock.unlock();
			this.bid.fileImportUpdate(counter, mgfFile.getName());
			if(trigger){
				bid.fileAnalysisAborted();
				return;
			}
			ExperimentContainer ec = readMGFCompound(mgfFile);
			if(ec!=null) results.add(ec);
			counter++;
		}
		
		bid.fileImportFinished();
		
	}
	
	public ExperimentContainer readMSCompound(File file){
		
		JenaMSConverter conv = new JenaMSConverter();
		ExperimentContainer ec = null;
		try{
			ec = conv.convert(file);
		}catch(RuntimeException e2){
			errors.add(file.getName()+": Invalid file format.");
			return null;
		}
		return ec;
		
	}
	
	public ExperimentContainer readMGFCompound(File file){
		
		MGFConverter conv = new MGFConverter();
		ExperimentContainer ec = null;
		try{
			ec = conv.convert(file);
		}catch(RuntimeException e2){
			errors.add(file.getName()+": Invalid file format.");
			return null;
		}
		return ec;
		
	}
	
	List<ExperimentContainer> getResults(){
		return this.results;
	}
	
	
}
