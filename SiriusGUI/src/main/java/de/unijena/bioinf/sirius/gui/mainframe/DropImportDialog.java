package de.unijena.bioinf.sirius.gui.mainframe;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

import de.unijena.bioinf.sirius.gui.io.DataFormat;
import de.unijena.bioinf.sirius.gui.io.DataFormatIdentifier;

public class DropImportDialog extends JDialog {
	
	private JProgressBar bar;
	private DataAnalyseThread dat;

	public DropImportDialog(JFrame owner,List<File> rawFiles) {
		super(owner,true);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setLayout(new BorderLayout());
		JPanel textPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,20,5));
		textPanel.add(new JLabel("Analysing data..."));
		this.add(textPanel,BorderLayout.NORTH);
		
		JPanel progressbarPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,10,5));
		bar = new JProgressBar(0, 100);
		progressbarPanel.add(bar);
		this.add(progressbarPanel,BorderLayout.SOUTH);
		
		this.pack();
		this.setResizable(false);
		
		this.dat = new DataAnalyseThread(rawFiles, this);
		Thread t = new Thread(dat);
		t.start();
		
		this.setVisible(true);
	}
	
	void initProgressBar(int elementNumber){
		this.bar.setMaximum(elementNumber);
		this.bar.setValue(0);
	}
	
	void updateProgressBar(int value){
		this.bar.setValue(value);
	}
	
	void progressFinished(){
		this.dispose();
	}
	
	public List<File> getCSVFiles(){
		return this.dat.getCSVFiles();
	}
	
	public List<File> getMSFiles(){
		return this.dat.getMSFiles();
	}
	
	public List<File> getMGFFiles(){
		return this.dat.getMGFFiles();
	}

}

class DataAnalyseThread implements Runnable {
	
	private List<File> csvFiles, msFiles, mgfFiles, rawFiles;
	private DataFormatIdentifier ident;
	private DropImportDialog diag;
	
	public DataAnalyseThread(List<File> rawFiles, DropImportDialog diag) {
		this.rawFiles = rawFiles;
		this.diag = diag;
	}
	
	@Override
	public void run() {
		
		diag.initProgressBar(rawFiles.size());
		
		csvFiles = new ArrayList<>();
		msFiles = new ArrayList<>();
		mgfFiles = new ArrayList<>();
		
		ident = new DataFormatIdentifier();
		
		for(int i=0;i<rawFiles.size();i++){
			diag.updateProgressBar(i);
			File file = rawFiles.get(i);
			DataFormat df = ident.identifyFormat(file);
			if(df==DataFormat.CSV){
				csvFiles.add(file);
			}else if(df==DataFormat.JenaMS){
				msFiles.add(file);
			}else if(df==DataFormat.MGF){
				mgfFiles.add(file);
			}else{
				continue;
			}
			
		}
		diag.progressFinished();
	}
	
	List<File> getMSFiles(){
		return this.msFiles;
	}
	
	List<File> getMGFFiles(){
		return this.mgfFiles;
	}
	
	List<File> getCSVFiles(){
		return this.csvFiles;
	}
}
