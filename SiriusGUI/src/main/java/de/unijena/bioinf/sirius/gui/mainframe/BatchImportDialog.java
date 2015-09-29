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
	private ImportExperimentsThread importThread;
	private Thread t;
	private Lock lock;
	private JButton abort;
	private JLabel progressl;
	private ReturnValue rv;

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
	}
	
	public void start(File[] files){
		importThread = new ImportExperimentsThread(files, this);
		t = new Thread(importThread);
		t.start();
		this.pack();
		this.setVisible(true);
	}

	public void finished() {
		this.rv = importThread.wasSuccessful();
		this.dispose();
	}
	
	public List<ExperimentContainer> getResults(){
		return this.importThread.getResults();
	}
	
	public List<String> getErrors(){
		return this.importThread.getErrors();
	}

	public void init(int maxVal) {
		lock.lock();
		progBar.setMaximum(maxVal);
		progBar.setMinimum(0);
		progBar.setValue(0);
		progressl.setText("");
		lock.unlock();
	}

	public void update(int currentIndex, String currentFileName) {
		lock.lock();
		progBar.setValue(currentIndex);
		progressl.setText("import \""+currentFileName+"\"");
		lock.unlock();		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==this.abort){
			importThread.abort();
			this.dispose();
		}
		
	}
	
	public ReturnValue wasSucessful(){
		return this.rv;
	}

}

class ImportExperimentsThread implements Runnable{
	
	private File[] files;
	private List<ExperimentContainer> results;
	private boolean stop;
	private ReentrantLock lock;
	private ReturnValue rv;
	private List<String> errors;
	private BatchImportDialog bid;
	
	ImportExperimentsThread(File[] files, BatchImportDialog bid){
		this.files = files;
		this.results = Collections.EMPTY_LIST;
		this.errors = Collections.EMPTY_LIST;
		this.stop = false;
		this.lock = new ReentrantLock(true);
		this.rv = ReturnValue.Abort;
		this.bid = bid;
	}
	
	private void abortImport(){
		lock.lock();
		this.stop = true;
		lock.unlock();
	}

	@Override
	public void run() {
		this.results = new ArrayList<>(files.length);
		this.rv = ReturnValue.Abort;
		this.errors = Collections.synchronizedList(new ArrayList<String>());
		this.bid.init(files.length);
		int counter=0;
		for(File f : files){
			boolean trigger;
			lock.lock(); //TODO Lock vermutlich ueberfluessig, da nur eine atomare Operation
			trigger = this.stop;
			lock.unlock();
			this.bid.update(counter, f.getName());
			if(trigger){
				bid.finished();
				return;
			}
			ExperimentContainer ec = readCompound(f);
			if(ec!=null) results.add(ec);
			counter++;
		}
		this.rv = ReturnValue.Success;
		bid.finished();
		
	}
	
	public ExperimentContainer readCompound(File file){
		DataFormatIdentifier dfi = new  DataFormatIdentifier();
		DataFormat df = dfi.identifyFormat(file);
		if(df==DataFormat.MGF){
			MGFConverter conv = new MGFConverter();
			ExperimentContainer ec = null;
			try{
				ec = conv.convert(file);
			}catch(RuntimeException e2){
				errors.add(file.getName()+": Invalid file format.");
				return null;
			}
			return ec;
		}else if(df==DataFormat.JenaMS){
			JenaMSConverter conv = new JenaMSConverter();
			ExperimentContainer ec = null;
			try{
				ec = conv.convert(file);
			}catch(RuntimeException e2){
				errors.add(file.getName()+": Invalid file format.");
				return null;
			}
			return ec;
		}else{
			errors.add(file.getName()+": unsupported file format.");
			return null;
		}
	}
	
	public ReturnValue wasSuccessful(){
		return rv;
	}
	
	List<ExperimentContainer> getResults(){
		return this.results;
	}
	
	List<String> getErrors(){
		return this.errors;
	}
	
	public void abort(){
		lock.lock();
		this.stop = true;
		lock.unlock();
	}
	
	
}
