package de.unijena.bioinf.sirius.gui.mainframe;

import java.util.*;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.json.JSONException;

import com.mysql.jdbc.authentication.Sha256PasswordPlugin;

import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.myxo.io.spectrum.MS2FormatSpectraReader;
import de.unijena.bioinf.myxo.structure.CompactExperiment;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;
import de.unijena.bioinf.sirius.Progress;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.compute.ComputeDialog;
import de.unijena.bioinf.sirius.gui.dialogs.ExceptionDialog;
import de.unijena.bioinf.sirius.gui.dialogs.FilePresentDialog;
import de.unijena.bioinf.sirius.gui.io.ZipExperimentIO;
import de.unijena.bioinf.sirius.gui.load.DefaultLoadDialog;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultPanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.cli.*;

public class MainFrame extends JFrame implements WindowListener, ActionListener, ListSelectionListener{
	
	private DefaultListModel<ExperimentContainer> compoundModel;
	private JList<ExperimentContainer> compoundList;
	private JButton newB, loadB, closeB, saveB, /*editB,*/ computeB;
	
	private HashSet<String> names;
	private int nameCounter;
	
	private JPanel resultsPanel;
	private CardLayout resultsPanelCL;
	private ResultPanel showResultsPanel;
	private static final String DUMMY_CARD = "dummy";
	private static final String RESULTS_CARD = "results";
	
	public MainFrame(){
		super("Sirius Prototype");
		
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		    	System.out.println(info.getName());
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		        }
		    }
		} catch (Exception e) {
		    // If Nimbus is not available, you can set the GUI to another look and feel.
		}
		nameCounter=1;
		this.names = new HashSet<>();
		
		this.addWindowListener(this);
		this.setLayout(new BorderLayout());
		
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		this.add(mainPanel,BorderLayout.CENTER);
//		JButton dummy = new JButton("compute");
//		dummy.addActionListener(this);
//		mainPanel.add(dummy,BorderLayout.CENTER);
		
		resultsPanelCL = new CardLayout();
		resultsPanel = new JPanel(resultsPanelCL);
		JPanel dummyPanel = new JPanel();
		resultsPanel.add(dummyPanel,DUMMY_CARD);
		
		showResultsPanel = new ResultPanel(this);
//		resultsPanel.add(showResultsPanel,RESULTS_CARD);
//		resultsPanelCL.show(resultsPanel, RESULTS_CARD);
		mainPanel.add(showResultsPanel,BorderLayout.CENTER);
		
		JPanel compoundPanel = new JPanel(new BorderLayout());
		compoundPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"experiments"));
		
		compoundModel = new DefaultListModel<>();
		compoundList = new JList<>(compoundModel);
		compoundList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		compoundList.setCellRenderer(new CompoundCellRenderer());
		compoundList.addListSelectionListener(this);
		compoundList.setMinimumSize(new Dimension(200,0));
		compoundList.setPreferredSize(new Dimension(200,0));
		
		JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setViewportView(compoundList);
		
		pane.getViewport().setPreferredSize(new Dimension(200,(int)pane.getViewport().getPreferredSize().getHeight()));
		
//		System.err.println(pane.getViewport().getPreferredSize().getWidth()+" "+pane.getViewport().getPreferredSize().getHeight());
//		
//		System.err.println(pane.getVerticalScrollBar().getPreferredSize().getWidth());
//		pane.setPreferredSize(new Dimension(221,0));
		
		compoundPanel.add(pane,BorderLayout.WEST);
		
		mainPanel.add(compoundPanel,BorderLayout.WEST);
		
		
		
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		controlPanel.setBorder(BorderFactory.createEtchedBorder());
		newB = new JButton("new");
		newB.addActionListener(this);
		controlPanel.add(newB);
		loadB = new JButton("open");
		loadB.addActionListener(this);
		controlPanel.add(loadB);
//		editB = new JButton("edit");
//		editB.addActionListener(this);
//		editB.setEnabled(false);
//		controlPanel.add(editB);
		closeB = new JButton("close");
		closeB.addActionListener(this);
		closeB.setEnabled(false);
		controlPanel.add(closeB);
		saveB = new JButton("save");
		saveB.addActionListener(this);
		saveB.setEnabled(false);
		controlPanel.add(saveB);
		computeB = new JButton("compute");
		computeB.addActionListener(this);
		computeB.setEnabled(false);
		controlPanel.add(computeB);
		
		mainPanel.add(controlPanel,BorderLayout.NORTH);
		
		
//		JPanel compoundControls = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
//		add = new JButton("load");
//		add.addActionListener(this);
//		remove = new JButton("close");
//		remove.addActionListener(this);
//		save = new JButton("save");
//		save.addActionListener(this);
//		compoundControls.add(add);
//		compoundControls.add(remove);
//		compoundControls.add(save);
//		compoundPanel.add(compoundControls,BorderLayout.SOUTH);
		
		this.setSize(new Dimension(1024,800));
		
		System.err.println(this.compoundList.getWidth());
		this.setVisible(true);
		System.err.println(this.compoundList.getWidth());
		
		System.err.println(this.compoundList.getSize().getWidth()+" "+this.compoundList.getSize().getHeight());
		System.err.println(pane.getSize().getWidth()+" "+pane.getSize().getHeight());
		System.err.println(compoundPanel.getSize().getWidth()+" "+compoundPanel.getSize().getHeight());
	}
	
	public static void main(String[] args){
		MainFrame mf = new MainFrame();
	}

	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosing(WindowEvent e) {
		this.dispose();
	}

	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==newB){
			LoadController lc = new LoadController(this);
			if(lc.getReturnValue() == ReturnValue.Success){
				ExperimentContainer ec = lc.getExperiment();
				
				while(true){
					System.out.println(ec.getSuffix());
					if(ec.getGUIName()!=null&&!ec.getGUIName().isEmpty()){
						if(this.names.contains(ec.getGUIName())){
							ec.setSuffix(ec.getSuffix()+1);
						}else{
							this.names.add(ec.getGUIName());
							break;
						}
					}else{
						ec.setName("Unknown");
						ec.setSuffix(1);
					}
				}
				
//				if(ec.getName()!=null&&!ec.getName().isEmpty()){
//					if(this.names.contains(ec.getName())){
//						ec.setName(ec.getName().trim()+" ("+nameCounter+")");
//						nameCounter++;
//						
//					}
//					this.names.add(ec.getName());
//				}else{
//					ec.setName("Compound "+nameCounter);
//					nameCounter++;
//				}
				this.compoundModel.addElement(ec);
//				System.out.println(ec.getName());
			}
		}else if(e.getSource()==computeB){
			ExperimentContainer ec = this.compoundList.getSelectedValue();
			if(ec!=null){
				ComputeDialog cd = new ComputeDialog(this,ec);
				if(cd.isSuccessful()){
//					System.err.println("ComputeDialog erfolgreich");
//					System.err.println("Anzahl Ergebnisse: "+ec.getResults().size());
					this.showResultsPanel.changeData(ec);
					resultsPanelCL.show(resultsPanel,RESULTS_CARD);
				}else{
//					System.err.println("ComputeDialog nicht erfolgreich");
				}
			}
			this.compoundList.repaint();
		}else if(e.getSource()==saveB){
			ExperimentContainer ec = this.compoundList.getSelectedValue();
			
			JFileChooser jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setAcceptAllFileFilterUsed(true);
			jfc.addChoosableFileFilter(new SiriusSaveFileFilter());
			
			File selectedFile = null;
			
			while(selectedFile==null){
				int returnval = jfc.showSaveDialog(this);
				if(returnval == JFileChooser.APPROVE_OPTION){
					File selFile = jfc.getSelectedFile();
					
					String name = selFile.getName();
					if(!selFile.getAbsolutePath().endsWith(".sirius")){
						selFile = new File(selFile.getAbsolutePath()+".sirius");
					}
					
					if(selFile.exists()){
						FilePresentDialog fpd = new FilePresentDialog(this, selFile.getName());
						ReturnValue rv = fpd.getReturnValue();
						if(rv==ReturnValue.Success){
							selectedFile = selFile;
						}
//						int rt = JOptionPane.showConfirmDialog(this, "The file \""+selFile.getName()+"\" is already present. Override it?");
					}else{
						selectedFile = selFile;	
					}
					
					
				}else{
					break;
				}
			}
			
			if(selectedFile!=null){
				try{
					ZipExperimentIO io = new ZipExperimentIO();
					io.save(ec, selectedFile);
				}catch(Exception e2){
					new ExceptionDialog(this, e2.getMessage());
				}
				
			}
			
			
		}else if(e.getSource()==loadB){
			
			JFileChooser jfc = new JFileChooser();
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setAcceptAllFileFilterUsed(true);
			jfc.addChoosableFileFilter(new SiriusSaveFileFilter());
			
			int returnVal = jfc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION){
				ZipExperimentIO io = new ZipExperimentIO();
				try{
					ExperimentContainer ec = io.load(jfc.getSelectedFile());
					while(true){
						if(ec.getGUIName()!=null&&!ec.getGUIName().isEmpty()){
							if(this.names.contains(ec.getGUIName())){
								ec.setSuffix(ec.getSuffix()+1);
							}else{
								this.names.add(ec.getGUIName());
								break;
							}
						}else{
							ec.setName("Unknown");
							ec.setSuffix(1);
						}
					}
					this.compoundModel.addElement(ec);
					this.compoundList.setSelectedValue(ec,true);
				}catch(Exception e2){
					new ExceptionDialog(this, e2.getMessage());
				}
			}
			
			
			
			
			
//			if(ec.getGUIName()!=null&&!ec.getGUIName().isEmpty()){
//				if(this.names.contains(ec.getName())){
//					ec.setName(ec.getName().trim()+" ("+nameCounter+")");
//					nameCounter++;
//					
//				}
//				this.names.add(ec.getName());
//			}else{
//				ec.setName("Compound "+nameCounter);
//				nameCounter++;
//			}
			
		}
//		}else if(e.getSource()==editB){
////			ExperimentContainer ec = this.compoundList.getSelectedValue();
////			if(ec!=null){
////				ComputeDialog cd = new ComputeDialog(this,ec);
////				System.out.println("werte cd aus");
////				if(cd.isSuccessful()){
////					System.err.println("ComputeDialog erfolgreich");
////					System.err.println("Anzahl Ergebnisse: "+ec.getResults().size());
////					this.showResultsPanel.changeData(ec);
////					resultsPanelCL.show(resultsPanel,RESULTS_CARD);
//////					dhh
//////					ResultPanel rp = new ResultPanel(ec);
//////					resultsPanel.add(rp, "blablabla");
//////					this.validate();
//////					resultsPanel.validate();
//////					resultsPanel.repaint();
//////					resultsPanelCL.show(resultsPanel,"blablabla");
//////					this.validate();
//////					resultsPanel.validate();
//////					resultsPanel.repaint();
//////					this.repaint();
//////					
//////					this.repaint();
////				}else{
////					System.err.println("ComputeDialog nicht erfolgreich");
////				}
//////				compute(ec);
////			}
//		}
		
		
		
	}
	
	
//	private void compute(ExperimentContainer ec){
////		Sirius sirius = new 
//	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource()==this.compoundList){
			int index = compoundList.getSelectedIndex();
			if(index<0){
				closeB.setEnabled(false);
//				editB.setEnabled(false);
				saveB.setEnabled(false);
				computeB.setEnabled(false);
			}else{
				closeB.setEnabled(true);
//				editB.setEnabled(true);
				saveB.setEnabled(true);
				computeB.setEnabled(true);
				this.showResultsPanel.changeData(compoundModel.getElementAt(index));
				resultsPanelCL.show(resultsPanel,RESULTS_CARD);
			}
		}
	}

}

class DummyProgress implements Progress {

	@Override
	public void finished() {
		System.out.println("done");
	}

	@Override
	public void info(String s) {
		System.out.println("info: "+s);
	}

	@Override
	public void init(double max) {
		System.out.println("max: "+max);
	}

	@Override
	public void update(double current, double max, String value) {
		System.out.println("current: "+current+"/"+max+" "+value);
	}
	
}

class SiriusSaveFileFilter extends FileFilter{

	@Override
	public boolean accept(File f) {
		if(f.isDirectory()) return true;
		String name = f.getName();
		if(name.endsWith(".sirius")){
			return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return ".sirius";
	}
	
}
