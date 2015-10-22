package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.gui.io.DataFormat;
import de.unijena.bioinf.sirius.gui.io.DataFormatIdentifier;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DropImportDialog extends JDialog implements ActionListener{
	
	private JProgressBar bar;
	private DataAnalyseThread dat;
	private JButton abort;
	
	private ReturnValue rv;

	public DropImportDialog(JFrame owner,List<File> rawFiles) {
		super(owner,true);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		JPanel centerPanel = new JPanel(new BorderLayout());
		centerPanel.setBorder(BorderFactory.createEtchedBorder());
		this.add(centerPanel, BorderLayout.CENTER);
		
		rv = ReturnValue.Abort;
		
		bar = new JProgressBar(JProgressBar.HORIZONTAL,0,100);
		bar.setValue(0);
		
		JPanel progressPanel = new JPanel(new BorderLayout());
		progressPanel.add(bar,BorderLayout.NORTH);
		JLabel  progressl = new JLabel("Analysing data...");
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
		
		this.setSize(new Dimension(300,125));
		this.setResizable(false);
		
		this.dat = new DataAnalyseThread(rawFiles, this);
		Thread t = new Thread(dat);
		t.start();
		
		setLocationRelativeTo(getParent());
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
		this.rv = ReturnValue.Success;
		this.dispose();
	}
	
	void progressAborted(){
		this.rv = ReturnValue.Abort;
		this.dispose();
	}
	
	public ReturnValue getReturnValue(){
		return this.rv;
	}
	
	public List<File> getCSVFiles(){
		if(this.rv == ReturnValue.Success) return this.dat.getCSVFiles();
		else return Collections.EMPTY_LIST;
	}
	
	public List<File> getMSFiles(){
		if(this.rv == ReturnValue.Success) return this.dat.getMSFiles();
		else return Collections.EMPTY_LIST;
	}
	
	public List<File> getMGFFiles(){
		return this.dat.getMGFFiles();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == this.abort){
			dat.abortProgress();
		}
	}

}

class DataAnalyseThread implements Runnable {
	
	private List<File> csvFiles, msFiles, mgfFiles, rawFiles;
	private DataFormatIdentifier ident;
	private DropImportDialog diag;
	private transient boolean stop;
	
	public DataAnalyseThread(List<File> rawFiles, DropImportDialog diag) {
		this.rawFiles = rawFiles;
		this.diag = diag;
		stop = false;
	}
	
	public void abortProgress(){
	}
	
	@Override
	public void run() {
		
		diag.initProgressBar(rawFiles.size());
		
		csvFiles = new ArrayList<>();
		msFiles = new ArrayList<>();
		mgfFiles = new ArrayList<>();
		
		ident = new DataFormatIdentifier();
		
		for(int i=0;i<rawFiles.size();i++){
			if(stop){
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						diag.progressAborted();
					}
				});
				return;
			}
			final int progress = i;
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					diag.updateProgressBar(progress);
				}
			});
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
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				diag.progressFinished();
			}
		});
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
