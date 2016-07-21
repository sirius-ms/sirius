package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.Sirius;
import de.unijena.bioinf.sirius.gui.compute.*;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedBatchDataFormatFilter;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.fingerid.*;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultPanel;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainFrame extends JFrame implements WindowListener, ActionListener, ListSelectionListener, DropTargetListener,MouseListener, KeyListener, JobLog.JobListener{

    private Logger logger = LoggerFactory.getLogger(MainFrame.class);

    private CompoundModel compoundModel;
	private JList<ExperimentContainer> compoundList;
	private JButton newB, loadB, closeB, saveB, editB, computeB, batchB, computeAllB, exportResultsB,aboutB,configFingerID, jobs;

	protected CSIFingerIdComputation csiFingerId;
	
	private HashSet<String> names;
	private int nameCounter;
	
	private JPanel resultsPanel;
	private CardLayout resultsPanelCL;
	private ResultPanel showResultsPanel;
	private static final String DUMMY_CARD = "dummy";
	private static final String RESULTS_CARD = "results";
	private ConfigStorage config;
    private JobDialog jobDialog;
    private ImageIcon jobRunning, jobNotRunning;

	private BackgroundComputation backgroundComputation;

	private boolean removeWithoutWarning = false;

    private DropTarget dropTarget;
	private ConfidenceList confidenceList;
	private JPopupMenu expPopMenu;
	private JMenuItem newExpMI, batchMI, editMI, closeMI, computeMI, cancelMI;
	private JLabel aboutL;
	private boolean computeAllActive;


    public ConfigStorage getConfig() {
        return config;
    }

    public MainFrame(){
		super(Sirius.VERSION_STRING);
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if ("Nimbus".equals(info.getName())) {
		            UIManager.setLookAndFeel(info.getClassName());
		        }
		    }
		} catch (Exception e) {
		    // If Nimbus is not available, you can set the GUI to another look and feel.
		}

        csiFingerId = new CSIFingerIdComputation(new CSIFingerIdComputation.Callback() {
			@Override
			public void computationFinished(final ExperimentContainer container, final SiriusResultElement element) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						refreshCompound(container);
						confidenceList.refreshList();
					}
				});
			}
		});


		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		computeAllActive = false;


		this.config = new ConfigStorage();

		this.backgroundComputation = new BackgroundComputation(this);

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
		
		//JPanel compoundPanel = new JPanel(new BorderLayout());
		//compoundPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"experiments"));


		final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

		compoundModel = new CompoundModel();
		compoundList = new JList<>(compoundModel);
		compoundList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		compoundList.setCellRenderer(new CompoundCellRenderer());
		compoundList.addListSelectionListener(this);
		compoundList.setMinimumSize(new Dimension(200,0));
		compoundList.addMouseListener(this);
//		compoundList.setPreferredSize(new Dimension(200,0));


		JScrollPane paneConfidence = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        confidenceList = new ConfidenceList(this);
		paneConfidence.setViewportView(confidenceList);

        csiFingerId.setConfidenceCallback(new CSIFingerIdComputation.Callback() {
            @Override
            public void computationFinished(ExperimentContainer container, SiriusResultElement element) {
                refreshCompound(container);
            }
        });

		JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setViewportView(compoundList);
		
		pane.getViewport().setPreferredSize(new Dimension(200,(int)pane.getViewport().getPreferredSize().getHeight()));
//		pane.getViewport().setMinimumSize(new Dimension(200,(int)pane.getViewport().getPreferredSize().getHeight()));
		
//		System.err.println(pane.getViewport().getPreferredSize().getWidth()+" "+pane.getViewport().getPreferredSize().getHeight());
//		
//		System.err.println(pane.getVerticalScrollBar().getPreferredSize().getWidth());
//		pane.setPreferredSize(new Dimension(221,0));

		tabbedPane.addTab("Experiments", pane);
		tabbedPane.addTab("Identifications", paneConfidence);


		//compoundPanel.add(pane,BorderLayout.WEST);
		
		mainPanel.add(tabbedPane,BorderLayout.WEST);

//		JPanel controlPanel = new JPanel(new WrapLayout(FlowLayout.LEFT,3,0));
		JPanel leftControlPanel = new JPanel(new WrapLayout(FlowLayout.LEFT,3,0));
//		controlPanel.setBorder(BorderFactory.createEtchedBorder());
		
		JPanel tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
		tempP.setBorder(BorderFactory.createEtchedBorder());
		
		newB = new JButton("Import",new ImageIcon(MainFrame.class.getResource("/icons/document-new.png")));
		newB.addActionListener(this);
        //newB.setToolTipText("Import measurements of a single compound");
		tempP.add(newB);
		batchB = new JButton("Batch Import",new ImageIcon(MainFrame.class.getResource("/icons/document-multiple.png")));
		batchB.addActionListener(this);
        //batchB.setToolTipText("Import measurements of several compounds");
		tempP.add(batchB);
		editB = new JButton("Edit",new ImageIcon(MainFrame.class.getResource("/icons/document-edit.png")));
		editB.addActionListener(this);
		editB.setEnabled(false);
        //editB.setToolTipText("Edit an experiment");
		tempP.add(editB);
		closeB = new JButton("Close",new ImageIcon(MainFrame.class.getResource("/icons/document-close.png")));
		closeB.addActionListener(this);
		closeB.setEnabled(false);
        //closeB.setToolTipText("Remove an experiment together with its results from the workspace");
		tempP.add(closeB);
		leftControlPanel.add(tempP);
		
		tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
		tempP.setBorder(BorderFactory.createEtchedBorder());
		
		loadB = new JButton("Open Workspace",new ImageIcon(MainFrame.class.getResource("/icons/document-open.png")));
		loadB.addActionListener(this);
        //loadB.setToolTipText("Load all experiments and computed results from a previously saved workspace.");
		tempP.add(loadB);
		saveB = new JButton("Save Workspace",new ImageIcon(MainFrame.class.getResource("/icons/media-floppy.png")));
        //saveB.setToolTipText("Save the entire workspace (all experiments and computed results).");
		saveB.addActionListener(this);
		saveB.setEnabled(false);
		tempP.add(saveB);
		leftControlPanel.add(tempP);
		
		tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
		tempP.setBorder(BorderFactory.createEtchedBorder());

		computeB = new JButton("Compute",new ImageIcon(MainFrame.class.getResource("/icons/applications-system.png")));
		/*
		computeB.addActionListener(this);
		computeB.setEnabled(false);
		tempP.add(computeB);
		*/
		computeAllB = new JButton("Compute All", new ImageIcon(MainFrame.class.getResource("/icons/applications-system.png")));
		computeAllB.addActionListener(this);
		computeAllB.setEnabled(false);
        //computeAllB.setToolTipText("Compute all compounds asynchronously");
		tempP.add(computeAllB);

        exportResultsB = new JButton("Export Results", new ImageIcon(MainFrame.class.getResource("/icons/document-export.png")));
        exportResultsB.addActionListener(this);
        exportResultsB.setEnabled(false);
        //exportResultsB.setToolTipText("Export identified molecular formulas into a CSV file.");
        tempP.add(exportResultsB);
        leftControlPanel.add(tempP);

        tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
        tempP.setBorder(BorderFactory.createEtchedBorder());

        configFingerID = new JButton("CSI:FingerId",new ImageIcon(MainFrame.class.getResource("/icons/fingerprint.png")));
        configFingerID.addActionListener(this);
        configFingerID.setEnabled(false);

        tempP.add(configFingerID);
        leftControlPanel.add(tempP);
		tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
		//tempP.setBorder(BorderFactory.createEtchedBorder());
        this.jobRunning = new ImageIcon(MainFrame.class.getResource("/icons/ajax-loader.gif"));
        this.jobNotRunning = new ImageIcon(MainFrame.class.getResource("/icons/ajax-stop.png"));
		jobs = new JButton("Jobs", jobNotRunning);


        jobDialog = new JobDialog(this);
        jobs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jobDialog.showDialog();
            }
        });

		tempP.add(jobs);
        JobLog.getInstance().addListener(this);
		leftControlPanel.add(tempP);

//        tempP = new JPanel(new FlowLayout(FlowLayout.LEFT,5,2));
//        tempP.setBorder(BorderFactory.createEtchedBorder());
        JPanel rightControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,2,2));
//        rightControlPanel.setBorder(Borderfac);

//        aboutB = new JButton(new ImageIcon(MainFrame.class.getResource("/icons/help-about.png")));
//        aboutB.addActionListener(this);
//        aboutB.setEnabled(true);
//        
//        aboutB.setBorderPainted(false);
//        aboutB.setOpaque(false);
//        aboutB.setFocusPainted(false);
//        aboutB.setContentAreaFilled(false);
//        aboutB.setMargin(new Insets(0, 0, 0, 0));
        
        AboutMouseAdapter adapter = new AboutMouseAdapter(this);
        
        aboutL = new JLabel(new ImageIcon(MainFrame.class.getResource("/icons/help-about.png"))); 
        aboutL.addMouseListener(this);
        
        aboutL.addMouseListener(adapter);
        
        rightControlPanel.add(aboutL);
        

        Box controlPanel = Box.createHorizontalBox();
//        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.add(leftControlPanel);
        controlPanel.add(Box.createHorizontalGlue());
        controlPanel.add(rightControlPanel);
        

		mainPanel.add(controlPanel,BorderLayout.NORTH);
		
		this.dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);
		
		constructExperimentListPopupMenu();
		{
			KeyStroke delKey = KeyStroke.getKeyStroke("DELETE");
			KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
			String delAction = "deleteItems";
			compoundList.getInputMap().put(delKey, delAction);
			compoundList.getInputMap().put(enterKey, "compute");
			compoundList.getActionMap().put(delAction, new AbstractAction()
			{
				@Override public void actionPerformed(ActionEvent e) {
					deleteCurrentCompound();
				}
			});
			compoundList.getActionMap().put("compute", new AbstractAction()
			{
				@Override public void actionPerformed(ActionEvent e) {
					computeCurrentCompound();
				}
			});
		}



		this.setSize(new Dimension(1368, 1024));

		addKeyListener(this);

        final SwingWorker w = new SwingWorker<VersionsInfo, VersionsInfo>() {

            @Override
            protected VersionsInfo doInBackground() throws Exception {
                try {
                    final VersionsInfo result = new WebAPI().needsUpdate();
                    publish(result);
                    return result;
                } catch (Exception e) {
                    e.printStackTrace();
                    final VersionsInfo resultAlternative = new VersionsInfo("unknown","unknown","unknown");
                    publish(resultAlternative);
                    return resultAlternative;
                }
            }

            @Override
            protected void process(List<VersionsInfo> chunks) {
                super.process(chunks);
                final VersionsInfo versionsNumber = chunks.get(0);
                System.out.println(String.valueOf(versionsNumber));
                if (versionsNumber!=null) {
                    csiFingerId.setVersionNumber(versionsNumber);
                    if (versionsNumber.outdated()) {
                        new UpdateDialog(MainFrame.this, versionsNumber.siriusGuiVersion);
                    } else {
                        configFingerID.setEnabled(true);
                        csiFingerId.setEnabled(true);
                    }
                }
            }

            @Override
            protected void done() {
                super.done();
            }
        };
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                w.execute();
            }
        });


		this.setVisible(true);
	}

    public CSIFingerIdComputation getCsiFingerId() {
        return csiFingerId;
    }

    public void constructExperimentListPopupMenu(){
		expPopMenu = new JPopupMenu();
		newExpMI = new JMenuItem("Import Experiment",new ImageIcon(MainFrame.class.getResource("/icons/document-new.png")));
		batchMI = new JMenuItem("Batch Import",new ImageIcon(MainFrame.class.getResource("/icons/document-multiple.png")));
		editMI = new JMenuItem("Edit Experiment",new ImageIcon(MainFrame.class.getResource("/icons/document-edit.png")));
		closeMI = new JMenuItem("Close Experiment",new ImageIcon(MainFrame.class.getResource("/icons/document-close.png")));
		computeMI = new JMenuItem("Compute",new ImageIcon(MainFrame.class.getResource("/icons/applications-system.png")));

        cancelMI = new JMenuItem("Cancel Computation", new ImageIcon(MainFrame.class.getResource("/icons/cancel.png")));



		
		newExpMI.addActionListener(this);
		batchMI.addActionListener(this);
		editMI.addActionListener(this);
		closeMI.addActionListener(this);
		computeMI.addActionListener(this);
        cancelMI.addActionListener(this);
		
		editMI.setEnabled(false);
		closeMI.setEnabled(false);
		computeMI.setEnabled(false);
        cancelMI.setEnabled(false);
		
		expPopMenu.add(computeMI);
        expPopMenu.add(cancelMI);
		expPopMenu.addSeparator();
		expPopMenu.add(newExpMI);
		expPopMenu.add(batchMI);
//		expPopMenu.addSeparator();
		expPopMenu.add(editMI);
		expPopMenu.add(closeMI);
//		expPopMenu.addSeparator();
	}

	@Override
	public void dispose() {
        showResultsPanel.dispose();
        csiFingerId.shutdown();
        super.dispose();
	}

	public BackgroundComputation getBackgroundComputation() {
		return backgroundComputation;
	}

	public static void main(String[] args){
		new MainFrame();
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

	public Enumeration<ExperimentContainer> getCompounds() {
		return compoundModel.elements();
	}

	public CompoundModel getCompoundModel() {
		return compoundModel;
	}

	public void refreshCompound(final ExperimentContainer c) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                compoundModel.refresh(c);
                refreshResultListFor(c);
                refreshComputationMenuItem();
                refreshExportMenuButton();
            }
        });

	}

	private void refreshResultListFor(ExperimentContainer c) {
		if (compoundList.getSelectedValue()==c) {
			showResultsPanel.changeData(c);
		}
	}

	private void refreshExportMenuButton() {
        final Enumeration<ExperimentContainer> ecs = getCompounds();
        while (ecs.hasMoreElements()) {
            final ExperimentContainer e = ecs.nextElement();
            if (e.getComputeState()== ComputingStatus.COMPUTED) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        exportResultsB.setEnabled(true);
                    }
                });
                return;
            }
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                exportResultsB.setEnabled(false);
            }
        });
    }

    private void refreshComputationMenuItem() {
        final ExperimentContainer ec = this.compoundList.getSelectedValue();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (ec != null && (ec.isComputing() || ec.isQueued()) ) {
                    cancelMI.setEnabled(true);
                } else {
                    cancelMI.setEnabled(false);
                }
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource()==configFingerID) {
            final FingerIdDialog dialog = new FingerIdDialog(this, csiFingerId, null, true);
			final int returnState = dialog.run();
            if (returnState==FingerIdDialog.COMPUTE_ALL) {
                csiFingerId.computeAll(getCompounds());
            }
        } else if(e.getSource()==newB || e.getSource()==newExpMI) {
            LoadController lc = new LoadController(this, config);
            lc.showDialog();
            if (lc.getReturnValue() == ReturnValue.Success) {
                ExperimentContainer ec = lc.getExperiment();

                importCompound(ec);
            }
        } else if (e.getSource()==exportResultsB) {
            exportResults();
		}else if(e.getSource()==computeB || e.getSource()==computeMI) {
			computeCurrentCompound();
		} else if (e.getSource() == computeAllB) {
			System.out.println(computeAllActive);
			if(computeAllActive){
				cancelComputation();
			}else{
				final BatchComputeDialog dia = new BatchComputeDialog(this);
			}
        } else if (e.getSource() == cancelMI) {
            final ExperimentContainer ec = compoundList.getSelectedValue();
            if (ec!=null)
                backgroundComputation.cancel(ec);
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
			
			
		}else if(e.getSource()==loadB) {

            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(config.getDefaultSaveFilePath());
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setAcceptAllFileFilterUsed(false);
            jfc.addChoosableFileFilter(new SiriusSaveFileFilter());

            int returnVal = jfc.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();
                config.setDefaultSaveFilePath(selFile.getParentFile());
				importWorkspace(Arrays.asList(selFile));
            }
        } else if (e.getSource()==aboutB) {
            new AboutDialog(this);
		}else if(e.getSource()==closeB || e.getSource()==closeMI){
			deleteCurrentCompound();
		}else if(e.getSource()==editB || e.getSource()==editMI){
			ExperimentContainer ec = this.compoundList.getSelectedValue();
            if (ec==null) return;
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

	private void importWorkspace(List<File> selFile) {
		ImportWorkspaceDialog workspaceDialog = new ImportWorkspaceDialog(this);
		final WorkspaceWorker worker = new WorkspaceWorker(this, workspaceDialog, selFile);
		worker.execute();
		workspaceDialog.start();
		worker.flushBuffer();
		try {
			worker.get();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		}
		worker.flushBuffer();
		if (worker.hasErrorMessage()) {
			new ExceptionDialog(this, worker.getErrorMessage());
		}
	}

	private void exportResults() {

		JFileChooser jfc = new JFileChooser();
		jfc.setCurrentDirectory(config.getCsvExportPath());
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setAcceptAllFileFilterUsed(false);
		jfc.addChoosableFileFilter(new SupportedExportCSVFormatsFilter());

        final ExporterAccessory accessory = new ExporterAccessory(jfc);
        jfc.setAccessory(accessory);

		File selectedFile = null;

		while(selectedFile==null){
			int returnval = jfc.showSaveDialog(this);
			if(returnval == JFileChooser.APPROVE_OPTION){
				File selFile = jfc.getSelectedFile();
                if (selFile==null) continue;
				config.setCsvExportPath((selFile.exists() && selFile.isDirectory()) ? selFile : selFile.getParentFile());

                if (accessory.isSingleFile()) {
                    String name = selFile.getName();
                    if(!name.endsWith(".csv") && !name.endsWith(".tsv")){
                        selFile = new File(selFile.getAbsolutePath()+".csv");
                    }

                    if(selFile.exists()){
                        FilePresentDialog fpd = new FilePresentDialog(this, selFile.getName());
                        ReturnValue rv = fpd.getReturnValue();
                        if(rv==ReturnValue.Success){
                            selectedFile = selFile;
                        }
                    }else{
                        selectedFile = selFile;
                    }

                } else {
                    if (!selFile.exists()) {
                        selFile.mkdirs();
                    }
                }
                selectedFile = selFile;
                break;
			}else{
				break;
			}
		}

        if (selectedFile==null) return;
        if (accessory.isSingleFile()) {
            try (final BufferedWriter fw = new BufferedWriter(new FileWriter(selectedFile))) {
                final Enumeration<ExperimentContainer> ecs = getCompounds();
                while (ecs.hasMoreElements()) {
                    final ExperimentContainer ec = ecs.nextElement();
                    if (ec.isComputed() && ec.getResults().size()>0) {
                        IdentificationResult.writeIdentifications(fw, SiriusDataConverter.experimentContainerToSiriusExperiment(ec), ec.getRawResults());
                    }
                }
            } catch (IOException e) {
                new ExceptionDialog(this, e.toString());
            }
        } else {
            try {
                writeMultiFiles(selectedFile, accessory.isExportingSirius(), accessory.isExportingFingerId());
            } catch (IOException e) {
                new ExceptionDialog(this, e.toString());
            }
        }
    }

    private void writeMultiFiles(File selectedFile, boolean withSirius, boolean withFingerid) throws IOException {
        final Enumeration<ExperimentContainer> containers = getCompounds();
        final HashSet<String> names = new HashSet<>();
        while (containers.hasMoreElements()) {
            final ExperimentContainer container = containers.nextElement();
            if (container.getResults()==null || container.getResults().size()==0) continue;
            final String name;
            {
                String origName = escapeFileName(container.getName());
                String aname = origName;
                int i=0;
                while (names.contains(aname)) {
                    aname = origName + (++i);
                }
                name = aname;
                names.add(name);
            }

            if (withSirius) {

                final File resultFile = new File(selectedFile, name + "_formula_candidates.csv");
                try (final BufferedWriter bw = Files.newBufferedWriter(resultFile.toPath(), Charset.defaultCharset())) {
                    bw.write("formula\trank\tscore\ttreeScore\tisoScore\texplainedPeaks\texplainedIntensity\n");
                    for (IdentificationResult result : container.getRawResults()) {
                        bw.write(result.getMolecularFormula().toString());
                        bw.write('\t');
                        bw.write(String.valueOf(result.getRank()));
                        bw.write('\t');
                        bw.write(String.valueOf(result.getScore()));
                        bw.write('\t');
                        bw.write(String.valueOf(result.getTreeScore()));
                        bw.write('\t');
                        bw.write(String.valueOf(result.getIsotopeScore()));
                        bw.write('\t');
                        final TreeScoring scoring = result.getTree().getAnnotationOrNull(TreeScoring.class);
                        bw.write(String.valueOf(result.getTree().numberOfVertices()));
                        bw.write('\t');
                        bw.write(scoring == null ? "\"\"" : String.valueOf(scoring.getExplainedIntensity()));
                        bw.write('\n');
                    }
                }
            }
            if (withFingerid) {
				final ArrayList<FingerIdData> datas = new ArrayList<>();
                for (SiriusResultElement elem : container.getResults()) {
                    if (elem.getFingerIdData()==null) continue;
					datas.add(elem.getFingerIdData());
                }
				final File resultFile = new File(selectedFile, name +".csv");
				new CSVExporter().exportToFile(resultFile, datas);
            }
        }
    }

    private String escapeFileName(String name) {
        final String n = name.replaceAll("[:\\\\/*\"?|<>']", "");
        if (n.length() > 128) {
            return n.substring(0,128);
        } else return n;
    }

    private void computeCurrentCompound() {
		ExperimentContainer ec = this.compoundList.getSelectedValue();
		if (ec != null) {
			ComputeDialog cd = new ComputeDialog(this, ec);
			if (cd.isSuccessful()) {
//					System.err.println("ComputeDialog erfolgreich");
//					System.err.println("Anzahl Ergebnisse: "+ec.getResults().size());
				this.showResultsPanel.changeData(ec);
				resultsPanelCL.show(resultsPanel, RESULTS_CARD);
			} else {
//					System.err.println("ComputeDialog nicht erfolgreich");
			}
		}
	}

	private void deleteCurrentCompound() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int index = compoundList.getSelectedIndex();
                if (index < 0) return;
                ExperimentContainer cont = compoundModel.get(index);
                backgroundComputation.cancel(cont);
                if (cont.getResults()!=null && cont.getResults().size()>0 && !config.isCloseNeverAskAgain()) {
                    CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(MainFrame.this, "When removing this experiment you will loose the computed identification results for \""  +cont.getGUIName()+"\"?", config);
                    CloseDialogReturnValue val = diag.getReturnValue();
                    if (val==CloseDialogReturnValue.abort) return;
                }
                compoundModel.remove(index);
                //this.compoundList.setSelectedIndex(-1);
                names.remove(cont.getGUIName());
            }
        });
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
	
	
	
	
	public void importCompound(final ExperimentContainer ec){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(ec.getGUIName()!=null&&!ec.getGUIName().isEmpty()){
                        if(names.contains(ec.getGUIName())){
                            ec.setSuffix(ec.getSuffix()+1);
                        }else{
                            names.add(ec.getGUIName());
                            break;
                        }
                    }else{
                        ec.setName("Unknown");
                        ec.setSuffix(1);
                    }
                }
                compoundModel.addElement(ec);
                compoundList.setSelectedValue(ec, true);
                if (ec.getResults().size()>0) ec.setComputeState(ComputingStatus.COMPUTED);
                if (ec.getComputeState()==ComputingStatus.COMPUTED) {
                    exportResultsB.setEnabled(true);
                }
            }
        });
	}

	public void clearWorkspace() {
		this.names.clear();
		this.compoundModel.removeAllElements();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource()==this.compoundList){
			if(this.compoundModel.size()>0){
				this.computeAllB.setEnabled(true);
			}else{
				this.computeAllB.setEnabled(false);
			}
			int index = compoundList.getSelectedIndex();
            refreshComputationMenuItem();
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

    public void selectExperimentContainer(ExperimentContainer container) {
        this.showResultsPanel.changeData(container);
        compoundList.setSelectedIndex(compoundModel.indexOf(container));
        resultsPanelCL.show(resultsPanel,RESULTS_CARD);
    }

    public void selectExperimentContainer(ExperimentContainer container, SiriusResultElement element) {
        selectExperimentContainer(container);
        showResultsPanel.select(element, true);
    }
	
	public void computationStarted(){
		this.computeAllActive = true;
		this.computeAllB.setText("Cancel Computation");
		this.computeAllB.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/cancel.png")));
	}
	
	public void computationComplete(){
        // check if computation is complete
		this.computeAllActive = false;
		this.computeAllB.setText("Compute All");
		this.computeAllB.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/applications-system.png")));
	}
	
	public void cancelComputation(){
        for (ExperimentContainer c : backgroundComputation.cancelAll()) {
            refreshCompound(c);
        }
        computationCanceled();
	}
	
	public void computationCanceled(){
		this.computeAllActive = false;
		this.computeAllB.setText("Compute All");
		this.computeAllB.setIcon(new ImageIcon(MainFrame.class.getResource("/icons/applications-system.png")));
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
		dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		Transferable tr = dtde.getTransferable();
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
	    	try {
                dtde.rejectDrop();
            } catch (Exception e2) {
                e.printStackTrace();
            }
	    }
	    
		if(newFiles.size()>0){
			importDragAndDropFiles(Arrays.asList(resolveFileList(newFiles.toArray(new File[newFiles.size()]))));
		}
	}
	
	private void importDragAndDropFiles(List<File> rawFiles){
		
		// entferne nicht unterstuetzte Files und suche nach CSVs

		// suche nach Sirius files
		final List<File> siriusFiles = new ArrayList<>();
		for (File f : rawFiles) {
			if (f.getName().toLowerCase().endsWith(".sirius")) {
				siriusFiles.add(f);
			}
		}
		if (siriusFiles.size() > 0 ) {
			importWorkspace(siriusFiles);
		}

		DropImportDialog dropDiag = new DropImportDialog(this, rawFiles);
		if(dropDiag.getReturnValue()==ReturnValue.Abort){
			return;
		}
		
		List<File> csvFiles = dropDiag.getCSVFiles();
		List<File> msFiles = dropDiag.getMSFiles();
		List<File> mgfFiles = dropDiag.getMGFFiles();
		
		if(csvFiles.isEmpty()&&msFiles.isEmpty()&&mgfFiles.isEmpty()) return;
		
		//Frage den Anwender ob er batch-Import oder alles zu einen Experiment packen moechte
		
		if( (csvFiles.size()>0&&(msFiles.size()+mgfFiles.size()==0)) ||
				(csvFiles.size()==0&&msFiles.size()==1&&mgfFiles.size()==0) ){   //nur CSV bzw. nur ein File
			LoadController lc = new LoadController(this, config);
//			files
			
			lc.addSpectra(csvFiles,msFiles,mgfFiles);
			lc.showDialog();
			
			if(lc.getReturnValue() == ReturnValue.Success){
				ExperimentContainer ec = lc.getExperiment();
				
				importCompound(ec);
			}
		} else if (csvFiles.size()==0 && mgfFiles.size()==0 && msFiles.size()>0) {
            importOneExperimentPerFile(msFiles,mgfFiles);
		} else {
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

	public int numberOfCompounds() {
		return compoundModel.getSize();
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode()==27) {
			deleteCurrentCompound();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

    public void fingerIdComputationComplete() {

    }

    @Override
    public void jobIsSubmitted(JobLog.Job job) {

    }

    @Override
    public void jobIsRunning(JobLog.Job job) {

    }

    @Override
    public void jobIsDone(final JobLog.Job job) {
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (JobLog.getInstance().hasActiveJobs()) {
					jobs.setIcon(jobRunning);
				} else jobs.setIcon(jobNotRunning);
			}
		});
    }

    @Override
    public void jobIsFailed(JobLog.Job job) {

    }

	@Override
	public void jobDescriptionChanged(JobLog.Job job) {

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

class AboutMouseAdapter extends MouseAdapter{
	
	private JFrame owner;
	
	public AboutMouseAdapter(JFrame owner) {
		this.owner = owner;
	}
	
	public void mousePressed(MouseEvent m){
		new AboutDialog(owner);
	}
}
