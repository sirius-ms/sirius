package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.compute.ComputeDialog;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedBatchDataFormatFilter;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultPanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame implements WindowListener, ActionListener, ListSelectionListener, DropTargetListener,MouseListener{
	
	private DefaultListModel<ExperimentContainer> compoundModel;
	private JList<ExperimentContainer> compoundList;
	private JButton newB, loadB, closeB, saveB, editB, computeB, batchB;
	
	private HashSet<String> names;
	private int nameCounter;
	
	private JPanel resultsPanel;
	private CardLayout resultsPanelCL;
	private ResultPanel showResultsPanel;
	private static final String DUMMY_CARD = "dummy";
	private static final String RESULTS_CARD = "results";
	private ConfigStorage config;

	private boolean removeWithoutWarning = false;
	
	private JPopupMenu expPopMenu;
	private JMenuItem newExpMI, batchMI, editMI, closeMI, computeMI;
	
	public MainFrame(){
		super(Sirius.VERSION_STRING);
		
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
		
		this.config = new ConfigStorage();
		
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
		
		showResultsPanel = new ResultPanel(this,config);
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
		compoundList.addMouseListener(this);
//		compoundList.setPreferredSize(new Dimension(200,0));
		
		JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setViewportView(compoundList);
		
		pane.getViewport().setPreferredSize(new Dimension(200,(int)pane.getViewport().getPreferredSize().getHeight()));
//		pane.getViewport().setMinimumSize(new Dimension(200,(int)pane.getViewport().getPreferredSize().getHeight()));
		
//		System.err.println(pane.getViewport().getPreferredSize().getWidth()+" "+pane.getViewport().getPreferredSize().getHeight());
//		
//		System.err.println(pane.getVerticalScrollBar().getPreferredSize().getWidth());
//		pane.setPreferredSize(new Dimension(221,0));
		
		compoundPanel.add(pane,BorderLayout.WEST);
		
		mainPanel.add(compoundPanel,BorderLayout.WEST);
		
		
		
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,3,0));
//		controlPanel.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
		tempP.setBorder(BorderFactory.createEtchedBorder());
		
		newB = new JButton("Import experiment",new ImageIcon(MainFrame.class.getResource("/icons/document-new.png")));
		newB.addActionListener(this);
		tempP.add(newB);
		batchB = new JButton("Batch import",new ImageIcon(MainFrame.class.getResource("/icons/document-multiple.png")));
		batchB.addActionListener(this);
		tempP.add(batchB);
		editB = new JButton("Edit",new ImageIcon(MainFrame.class.getResource("/icons/document-edit.png")));
		editB.addActionListener(this);
		editB.setEnabled(false);
		tempP.add(editB);
		closeB = new JButton("Close",new ImageIcon(MainFrame.class.getResource("/icons/document-close.png")));
		closeB.addActionListener(this);
		closeB.setEnabled(false);
		tempP.add(closeB);
		controlPanel.add(tempP);
		
		tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
		tempP.setBorder(BorderFactory.createEtchedBorder());
		
		loadB = new JButton("Open Workspace",new ImageIcon(MainFrame.class.getResource("/icons/document-open.png")));
		loadB.addActionListener(this);
		tempP.add(loadB);
		saveB = new JButton("Save Workspace",new ImageIcon(MainFrame.class.getResource("/icons/media-floppy.png")));
		saveB.addActionListener(this);
		saveB.setEnabled(false);
		tempP.add(saveB);
		controlPanel.add(tempP);
		
		tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
		tempP.setBorder(BorderFactory.createEtchedBorder());
		
		computeB = new JButton("Compute",new ImageIcon(MainFrame.class.getResource("/icons/applications-system.png")));
		computeB.addActionListener(this);
		computeB.setEnabled(false);
		tempP.add(computeB);
		controlPanel.add(tempP);
		
		mainPanel.add(controlPanel,BorderLayout.NORTH);
		
		DropTarget dropTarget = new DropTarget(this, this);
		
		constructExperimentListPopupMenu();
		
		this.setSize(new Dimension(1024,800));
		
		this.setVisible(true);
	}
	
	public void constructExperimentListPopupMenu(){
		expPopMenu = new JPopupMenu();
		newExpMI = new JMenuItem("Import experiment",new ImageIcon(MainFrame.class.getResource("/icons/document-new.png")));
		batchMI = new JMenuItem("Batch import",new ImageIcon(MainFrame.class.getResource("/icons/document-multiple.png")));
		editMI = new JMenuItem("Edit experiment",new ImageIcon(MainFrame.class.getResource("/icons/document-edit.png")));
		closeMI = new JMenuItem("Close experiment",new ImageIcon(MainFrame.class.getResource("/icons/document-close.png")));
		computeMI = new JMenuItem("Compute",new ImageIcon(MainFrame.class.getResource("/icons/applications-system.png")));
		
		newExpMI.addActionListener(this);
		batchMI.addActionListener(this);
		editMI.addActionListener(this);
		closeMI.addActionListener(this);
		computeMI.addActionListener(this);
		
		editMI.setEnabled(false);
		closeMI.setEnabled(false);
		computeMI.setEnabled(false);
		
		expPopMenu.add(computeMI);
		expPopMenu.addSeparator();
		expPopMenu.add(newExpMI);
		expPopMenu.add(batchMI);
//		expPopMenu.addSeparator();
		expPopMenu.add(editMI);
		expPopMenu.add(closeMI);
		expPopMenu.addSeparator();
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
		if(e.getSource()==newB || e.getSource()==newExpMI){
			LoadController lc = new LoadController(this,config);
			lc.showDialog();
			if(lc.getReturnValue() == ReturnValue.Success){
				ExperimentContainer ec = lc.getExperiment();
				
				importCompound(ec);
			}
		}else if(e.getSource()==computeB || e.getSource()==computeMI){
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
			
			JFileChooser jfc = new JFileChooser();
			jfc.setCurrentDirectory(config.getDefaultSaveFilePath());
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setAcceptAllFileFilterUsed(false);
			jfc.addChoosableFileFilter(new SiriusSaveFileFilter());
			
			File selectedFile = null;
			
			while(selectedFile==null){
				int returnval = jfc.showSaveDialog(this);
				if(returnval == JFileChooser.APPROVE_OPTION){
					File selFile = jfc.getSelectedFile();
					config.setDefaultSaveFilePath(selFile.getParentFile());
					
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
					WorkspaceIO io = new WorkspaceIO();
					io.store(new AbstractList<ExperimentContainer>() {
                        @Override
                        public ExperimentContainer get(int index) {
                            return compoundList.getModel().getElementAt(index);
                        }

                        @Override
                        public int size() {
                            return compoundList.getModel().getSize();
                        }
                    }, selectedFile);
				}catch(Exception e2){
					new ExceptionDialog(this, e2.getMessage());
				}
				
			}
			
			
		}else if(e.getSource()==loadB){
			
			JFileChooser jfc = new JFileChooser();
			jfc.setCurrentDirectory(config.getDefaultSaveFilePath());
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			jfc.setAcceptAllFileFilterUsed(false);
			jfc.addChoosableFileFilter(new SiriusSaveFileFilter());
			
			int returnVal = jfc.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION){
				WorkspaceIO io = new WorkspaceIO();
				File selFile = jfc.getSelectedFile();
				config.setDefaultSaveFilePath(selFile.getParentFile());
				ImportWorkspaceDialog workspaceDialog = new ImportWorkspaceDialog(this);
				final WorkspaceWorker worker = new WorkspaceWorker(this, workspaceDialog,selFile);
				worker.execute();
				workspaceDialog.setVisible(true);
				worker.flushBuffer();
			}
			
		}else if(e.getSource()==closeB || e.getSource()==closeMI){
			int index = this.compoundList.getSelectedIndex();
			ExperimentContainer cont = this.compoundModel.get(index);
			if (cont.getResults()!=null && cont.getResults().size()>0 && !config.isCloseNeverAskAgain()) {
				CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(this, "When removing this experiment you will loose the computed identification results for \""  +cont.getGUIName()+"\"?", config);
				CloseDialogReturnValue val = diag.getReturnValue();
				if (val==CloseDialogReturnValue.abort) return;
			}
			this.compoundModel.remove(index);
			this.compoundList.setSelectedIndex(-1);
			this.names.remove(cont.getGUIName());
		}else if(e.getSource()==editB || e.getSource()==editMI){
			ExperimentContainer ec = this.compoundList.getSelectedValue();
			String guiname = ec.getGUIName();
			
			LoadController lc = new LoadController(this,ec,config);
			lc.showDialog();
			if(lc.getReturnValue() == ReturnValue.Success){
//				ExperimentContainer ec = lc.getExperiment();
				
				if(!ec.getGUIName().equals(guiname)){
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
				}
				this.compoundList.repaint();
			}
			
		}else if(e.getSource()==batchB || e.getSource()==batchMI){
			JFileChooser chooser = new JFileChooser(config.getDefaultLoadDialogPath());
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(true);
			chooser.addChoosableFileFilter(new SupportedBatchDataFormatFilter());
			chooser.setAcceptAllFileFilterUsed(false);
			int returnVal = chooser.showOpenDialog(this);
			if(returnVal == JFileChooser.APPROVE_OPTION){
				File[] files = chooser.getSelectedFiles();
				config.setDefaultLoadDialogPath(files[0].getParentFile());
				importOneExperimentPerFile(files);
			}
			
			
			//zu unfangreich, extra Methode
			
		}
		
		
		
	}

	public void importOneExperimentPerFile(List<File> msFiles, List<File> mgfFiles){
		BatchImportDialog batchDiag = new BatchImportDialog(this);
		batchDiag.start(msFiles,mgfFiles);
		
		List<ExperimentContainer> ecs = batchDiag.getResults();
		List<String> errors = batchDiag.getErrors(); 
		importOneExperimentPerFileStep2(ecs, errors);
	}
	
	public void importOneExperimentPerFile(File[] files){
		BatchImportDialog batchDiag = new BatchImportDialog(this);
		batchDiag.start(resolveFileList(files));
		
		List<ExperimentContainer> ecs = batchDiag.getResults();
		List<String> errors = batchDiag.getErrors(); 
		importOneExperimentPerFileStep2(ecs, errors);
	}

	public File[] resolveFileList(File[] files) {
		final ArrayList<File> filelist = new ArrayList<>();
		for (File f : files) {
			if (f.isDirectory()) {
				final File[] fl = f.listFiles();
				if (fl!=null) {
					for (File g : fl)
						if (!g.isDirectory()) filelist.add(g);
				}
			} else {
				filelist.add(f);
			}
		}
		return filelist.toArray(new File[filelist.size()]);
	}
	
	public void importOneExperimentPerFileStep2(List<ExperimentContainer> ecs, List<String> errors){
		if(ecs!=null){
			for(ExperimentContainer ec : ecs){
				if(ec==null){
					continue;
				}else{
					importCompound(ec);
				}
			}
		}
		
		
		if(errors!=null){
			if(errors.size()>1){
				ErrorListDialog elDiag = new ErrorListDialog(this, errors);
			}else if(errors.size()==1){
				ExceptionDialog eDiag = new ExceptionDialog(this, errors.get(0)); 
			}
			
		}
	}
	
	
	
	
	public void importCompound(ExperimentContainer ec){
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
		this.compoundList.setSelectedValue(ec, true);
	}

	public void clearWorkspace() {
		this.names.clear();
		this.compoundModel.removeAllElements();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource()==this.compoundList){
			int index = compoundList.getSelectedIndex();
			if(index<0){
				closeB.setEnabled(false);
				editB.setEnabled(false);
				saveB.setEnabled(false);
				computeB.setEnabled(false);
				
				closeMI.setEnabled(false);
				editMI.setEnabled(false);
				computeMI.setEnabled(false);
				this.showResultsPanel.changeData(null);
			}else{
				closeB.setEnabled(true);
				editB.setEnabled(true);
				saveB.setEnabled(true);
				computeB.setEnabled(true);
				
				closeMI.setEnabled(true);
				editMI.setEnabled(true);
				computeMI.setEnabled(true);
				this.showResultsPanel.changeData(compoundModel.getElementAt(index));
				resultsPanelCL.show(resultsPanel,RESULTS_CARD);
			}
		}
	}
	
	//////////////////////////////////////////////////
	////////////////// drag and drop /////////////////
	//////////////////////////////////////////////////
	
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		Transferable tr = dtde.getTransferable();
		dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		DataFlavor[] flavors = tr.getTransferDataFlavors();
	    List<File> newFiles = new ArrayList<File>();
	    try{
			for (int i = 0; i < flavors.length; i++) {
				if (flavors[i].isFlavorJavaFileListType()) {
					List files = (List) tr.getTransferData(flavors[i]);
					for (Object o : files) {
						File file = (File) o;
						newFiles.add(file);
					}
				}
				dtde.dropComplete(true);
			}
	    }catch(Exception e){
	    	e.printStackTrace();
	    	dtde.rejectDrop();
	    }
	    
		if(newFiles.size()>0){
			importDragAndDropFiles(Arrays.asList(resolveFileList(newFiles.toArray(new File[newFiles.size()]))));
		}
	}
	
	private void importDragAndDropFiles(List<File> rawFiles){
		
		// entferne nicht unterstuetzte Files und suche nach CSVs
		
		DropImportDialog dropDiag = new DropImportDialog(this, rawFiles);
		if(dropDiag.getReturnValue()==ReturnValue.Abort){
			return;
		}
		
		List<File> csvFiles = dropDiag.getCSVFiles();
		List<File> msFiles = dropDiag.getMSFiles();
		List<File> mgfFiles = dropDiag.getMGFFiles();
		
		if(csvFiles.isEmpty()&&msFiles.isEmpty()&&mgfFiles.isEmpty()) return;
		
		//Frage den Anwender ob er batch-Import oder alles zu einen Experiment packen moechte
		
		if(csvFiles.size()>0&&(msFiles.size()+mgfFiles.size()==0)){   //nur CSV
			LoadController lc = new LoadController(this, config);
//			files
			
			lc.addSpectra(csvFiles,msFiles,mgfFiles);
			lc.showDialog();
			
			if(lc.getReturnValue() == ReturnValue.Success){
				ExperimentContainer ec = lc.getExperiment();
				
				importCompound(ec);
			}
		}else{
			DragAndDropOpenDialog diag = new DragAndDropOpenDialog(this);
			DragAndDropOpenDialogReturnValue rv = diag.getReturnValue();
			if(rv==DragAndDropOpenDialogReturnValue.abort){
				return;
			}else if(rv==DragAndDropOpenDialogReturnValue.oneExperimentForAll){
				LoadController lc = new LoadController(this, config);
				lc.addSpectra(csvFiles,msFiles,mgfFiles);
				lc.showDialog();
				
				if(lc.getReturnValue() == ReturnValue.Success){
					ExperimentContainer ec = lc.getExperiment();
					
					importCompound(ec);
				}
			}else if(rv==DragAndDropOpenDialogReturnValue.oneExperimentPerFile){
				importOneExperimentPerFile(msFiles,mgfFiles);
			}
		}
	}
	
	/////////////////// Mouselistener ///////////////////////

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(e.isPopupTrigger()){
			this.expPopMenu.show(e.getComponent(), e.getX(), e.getY());			
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(e.isPopupTrigger()){
			this.expPopMenu.show(e.getComponent(), e.getX(), e.getY());			
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
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
