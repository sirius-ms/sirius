package de.unijena.bioinf.sirius.gui.mainframe;

import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.compute.BackgroundComputation;
import de.unijena.bioinf.sirius.gui.compute.BatchComputeDialog;
import de.unijena.bioinf.sirius.gui.compute.JobDialog;
import de.unijena.bioinf.sirius.gui.db.DatabaseDialog;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.ext.DragAndDrop;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedBatchDataFormatFilter;
import de.unijena.bioinf.sirius.gui.fingerid.*;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultPanel;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.COMPOUNT_LIST;
import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.CONFIG_STORAGE;

public class MainFrame extends JFrame implements /*WindowListener,*//* ActionListener,*/ /*ListSelectionListener,*/ DropTargetListener/*, KeyListener*/ {
     //todo better place
    public static final MainFrame MF;

    static {
        MF = new MainFrame();
        decoradeMainFrameInstance(MF);
    }


    private ExperimentListPanel compountListPanel;
    public ExperimentListPanel getCompountListPanel() {return compountListPanel;}

    private CSIFingerIdComputation csiFingerId;
    public CSIFingerIdComputation getCsiFingerId() {return csiFingerId;}

    private JobDialog jobDialog;
    public JobDialog getJobDialog() {return jobDialog;}

    private ActionMap ACTIONS;

    public ActionMap getACTIONS() {
        return ACTIONS;
    }

    private SiriusToolbar toolbar;
    private JPanel resultsPanel;
    private CardLayout resultsPanelCL;
    private ResultPanel showResultsPanel;
    private static final String DUMMY_CARD = "dummy";
    private static final String RESULTS_CARD = "results";
    private DatabaseDialog dbDialog;

    private BackgroundComputation backgroundComputation;

    private boolean removeWithoutWarning = false; //todo config storage

    private DropTarget dropTarget;
    private ConfidenceList confidenceList;


    private MainFrame() { //todo should not be public. but where to store
        super(ApplicationCore.VERSION_STRING);
    }

    private static void decoradeMainFrameInstance(final MainFrame mf) {
        mf.csiFingerId = new CSIFingerIdComputation(new CSIFingerIdComputation.Callback() {
            @Override
            public void computationFinished(final ExperimentContainer container, final SiriusResultElement element) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        container.fireUpdateEvent();
                    }
                });
            }
        });

        mf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        mf.backgroundComputation = new BackgroundComputation(mf);

//        nameCounter = 1;


//        this.addWindowListener(this);
        mf.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mf.ACTIONS = mainPanel.getActionMap();
        mf.initActions();

        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        mf.add(mainPanel, BorderLayout.CENTER);


        /////////////////////////////// LEFT Panel (Compunt list) //////////////////////////////

        mf.compountListPanel = new ExperimentListPanel();
//        compountListPanel.compoundList.addListSelectionListener(this);
        mf.compountListPanel.compoundList.setMinimumSize(new Dimension(200, 0));
        /////////////////////////////// LEFT Panel (Compunt list) DONE //////////////////////////////
        mf.jobDialog = new JobDialog(mf);

        //todo reintegrate this panel wenn confidence works
        /*JPanel tmp_wrapper = new JPanel(new BorderLayout());
        JLabel label = new JLabel("<html>Confidence score is an experimental feature.Use with caution.<html>");
        tmp_wrapper.add(label, BorderLayout.NORTH);

        confidenceList = new ConfidenceList(COMPOUNT_LIST);
        confidenceList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!confidenceList.isSelectionEmpty()) {
                    final ExperimentContainer container = confidenceList.getSelectedValue();
                    selectExperimentContainer(container, container.getBestHit());
                }
            }
        });

        JScrollPane paneConfidence = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        paneConfidence.setViewportView(confidenceList);
        label.setPreferredSize(new Dimension(paneConfidence.getPreferredSize().width, label.getPreferredSize().height * 3));
        tmp_wrapper.add(paneConfidence, BorderLayout.CENTER);

        csiFingerId.setConfidenceCallback(new CSIFingerIdComputation.Callback() {
            @Override
            public void computationFinished(ExperimentContainer container, SiriusResultElement element) {
                refreshCompound(container);
            }
        });*/

        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.addTab("Experiments", mf.compountListPanel);
        tabbedPane.addTab("Identifications", new JPanel());
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setPreferredSize(new Dimension(218, (int) tabbedPane.getPreferredSize().getHeight()));
        mainPanel.add(tabbedPane, BorderLayout.WEST);


        // results PAnel
        mf.resultsPanelCL = new CardLayout();
        mf.resultsPanel = new JPanel(mf.resultsPanelCL);
        JPanel dummyPanel = new JPanel();
        mf.resultsPanel.add(dummyPanel, DUMMY_CARD);

        mf.showResultsPanel = new ResultPanel();
        mainPanel.add(mf.showResultsPanel, BorderLayout.CENTER);
        // resluts done

        mf.toolbar = new SiriusToolbar(mf.jobDialog);
        mf.add(mf.toolbar, BorderLayout.NORTH);

        mf.dropTarget = new DropTarget(mf, DnDConstants.ACTION_COPY_OR_MOVE, mf);


        mf.setSize(new Dimension(1368, 1024));

//        addKeyListener(this); //todo maybe we need this??

        final SwingWorker w = new SwingWorker<VersionsInfo, VersionsInfo>() {

            @Override
            protected VersionsInfo doInBackground() throws Exception {
                try {
                    final VersionsInfo result = new WebAPI().needsUpdate();
                    publish(result);
                    return result;
                } catch (Exception e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                    final VersionsInfo resultAlternative = new VersionsInfo("unknown", "unknown", "unknown");
                    publish(resultAlternative);
                    return resultAlternative;
                }
            }

            @Override
            protected void process(List<VersionsInfo> chunks) {
                super.process(chunks);
                final VersionsInfo versionsNumber = chunks.get(0);
                if (versionsNumber != null) {
                    mf.csiFingerId.setVersionNumber(versionsNumber);
                    if (versionsNumber.outdated()) {
                        new UpdateDialog(mf, versionsNumber.siriusGuiVersion);
                    } else {
                        mf.toolbar.setFingerIDEnabled(true);
                        mf.csiFingerId.setEnabled(true);
                    }
                    if (versionsNumber.hasNews()) {
                        new NewsDialog(mf, versionsNumber.getNews());
                    }
                } else {
                    new NoConnectionDialog(mf);
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


        mf.setVisible(true);
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


    /*public void windowOpened(WindowEvent e) {
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

    }*/

    public List<ExperimentContainer> getCompounds() {
        return compountListPanel.compoundEventList;
    }

    /*public List<ExperimentContainer> getSelectedCompounds() {
        return compountListPanel.compoundList.getSelectedValuesList();
    }*/

//    public EventList<ExperimentContainer> getCompoundsList() {
//        return compoundEventList;
//    }

    /* public ListModel getCompoundModel() {
        return compoundModel;
    }*/

   /* public void refreshCompound(final ExperimentContainer c) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (compoundList.getSelectedValue() == c) {
                    showResultsPanel.changeData(c);
                }
                refreshComputationMenuItem();
                refreshExportMenuButton();

            }
        });

    }*/


    /*public void actionPerformed(ActionEvent e) {
        if (e.getSource() == configFingerID) { //compute fingerid

        } else if (e.getSource() == newB || e.getSource() == newExpMI) {

        } else if (e.getSource() == exportResultsB) {
            exportResults();
        } else if (e.getSource() == computeMI) {

        } else if (e.getSource() == computeAllB) {
            if (computeAllActive) {

            }
        } else if (e.getSource() == cancelMI) {

        } else if (e.getSource() == saveB) {




        } else if (e.getSource() == loadB) {


        } else if (e.getSource() == closeMI) {

        } else if (e.getSource() == editMI) {

        } else if (e.getSource() == batchB || e.getSource() == batchMI) {



            //zu unfangreich, extra Methode

        }


    }*/


    private void initActions() {
        final ActionMap am = ACTIONS;
        am.put("compute_csi", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final FingerIdDialog dialog = new FingerIdDialog(MF, MF.csiFingerId, null, true);
                final int returnState = dialog.run();
                if (returnState == FingerIdDialog.COMPUTE_ALL) {
                    csiFingerId.computeAll(getCompounds());
                } else if (returnState == FingerIdDialog.COMPUTE) {
                    csiFingerId.computeAll(compountListPanel.compoundList.getSelectedValuesList());
                }
            }
        });
        am.put("compute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<ExperimentContainer> ecs = compountListPanel.compoundList.getSelectedValuesList();
                if (ecs != null && !ecs.isEmpty()) {
                    new BatchComputeDialog(MF, ecs); //todo no check button should not be active
                }
            }
        });

        am.put("cancel_compute", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (ExperimentContainer ec : compountListPanel.compoundList.getSelectedValuesList()) {
                    backgroundComputation.cancel(ec);
                }
            }
        });

        am.put("compute_all", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (toolbar.computeAllActive) {
                    backgroundComputation.cancelAll();
                } else {
                    new BatchComputeDialog(MF, compountListPanel.compoundEventList);
                }
            }
        });


        am.put("new_exp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoadController lc = new LoadController(MF, CONFIG_STORAGE);
                lc.showDialog();
                if (lc.getReturnValue() == ReturnValue.Success) {
                    ExperimentContainer ec = lc.getExperiment();
                    Workspace.importCompound(ec);
                }
            }
        });

        am.put("edit_exp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ExperimentContainer ec = compountListPanel.compoundList.getSelectedValue();
                if (ec == null) return;
                String guiname = ec.getGUIName();
                LoadController lc = new LoadController(MF, ec, CONFIG_STORAGE);
                lc.showDialog();
                if (lc.getReturnValue() == ReturnValue.Success) {
                    if (!ec.getGUIName().equals(guiname)) {
                        Workspace.resolveCompundNameConflict(ec);
                    }
                }
            }
        });


        am.put("save-ws", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setCurrentDirectory(CONFIG_STORAGE.getDefaultSaveFilePath());
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jfc.setAcceptAllFileFilterUsed(false);
                jfc.addChoosableFileFilter(new Workspace.SiriusSaveFileFilter());

                File selectedFile = null;

                while (selectedFile == null) {
                    int returnval = jfc.showSaveDialog(MF);
                    if (returnval == JFileChooser.APPROVE_OPTION) {
                        File selFile = jfc.getSelectedFile();
                        CONFIG_STORAGE.setDefaultSaveFilePath(selFile.getParentFile());

                        String name = selFile.getName();
                        if (!selFile.getAbsolutePath().endsWith(".sirius")) {
                            selFile = new File(selFile.getAbsolutePath() + ".sirius");
                        }

                        if (selFile.exists()) {
                            FilePresentDialog fpd = new FilePresentDialog(MF, selFile.getName());
                            ReturnValue rv = fpd.getReturnValue();
                            if (rv == ReturnValue.Success) {
                                selectedFile = selFile;
                            }
                        } else {
                            selectedFile = selFile;
                        }
                    } else {
                        break;
                    }
                }
                if (selectedFile != null) {
                    try {
                        WorkspaceIO io = new WorkspaceIO();
                        io.store(new AbstractList<ExperimentContainer>() {
                            @Override
                            public ExperimentContainer get(int index) {
                                return COMPOUNT_LIST.get(index);
                            }

                            @Override
                            public int size() {
                                return COMPOUNT_LIST.size();
                            }
                        }, selectedFile);
                    } catch (Exception e2) {
                        new ErrorReportDialog(MF, e2.getMessage());
                    }

                }
            }
        });

        am.put("load_ws", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jfc = new JFileChooser();
                jfc.setCurrentDirectory(CONFIG_STORAGE.getDefaultSaveFilePath());
                jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jfc.setAcceptAllFileFilterUsed(false);
                jfc.addChoosableFileFilter(new Workspace.SiriusSaveFileFilter());

                int returnVal = jfc.showOpenDialog(MF);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File selFile = jfc.getSelectedFile();
                    CONFIG_STORAGE.setDefaultSaveFilePath(selFile.getParentFile());
                    Workspace.importWorkspace(Arrays.asList(selFile));
                }
            }
        });

        am.put("batch_import", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(CONFIG_STORAGE.getDefaultLoadDialogPath());
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.setMultiSelectionEnabled(true);
                chooser.addChoosableFileFilter(new SupportedBatchDataFormatFilter());
                chooser.setAcceptAllFileFilterUsed(false);
                int returnVal = chooser.showOpenDialog(MF);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File[] files = chooser.getSelectedFiles();
                    CONFIG_STORAGE.setDefaultLoadDialogPath(files[0].getParentFile());
                    Workspace.importOneExperimentPerFile(files);
                }
            }
        });

        am.put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<ExperimentContainer> toRemove = compountListPanel.compoundList.getSelectedValuesList();

                if (!CONFIG_STORAGE.isCloseNeverAskAgain()) {
                    CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(MainFrame.this, "When removing the selected experiment(s) you will loose all computed identification results?");
                    CloseDialogReturnValue val = diag.getReturnValue();
                    if (val == CloseDialogReturnValue.abort) return;
                }
                for (ExperimentContainer cont : toRemove) {
                    backgroundComputation.cancel(cont);
                }
                Workspace.removeAll(toRemove);
            }
        });

        am.put("show_settings", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SettingsDialog(MF);
            }
        });

        am.put("show_bug", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new BugReportDialog(MF);
            }
        });

        am.put("show_about", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new AboutDialog(MF);
            }
        });

        am.put("show_db", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dbDialog == null) dbDialog = new DatabaseDialog(MF, CONFIG_STORAGE);
                dbDialog.setVisible(true);
            }
        });


    }

    //todo i think we nee a listener for this
    /*@Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == this.compoundList) {

            if (index < 0) {

                this.showResultsPanel.changeData(null);
            } else {

                this.showResultsPanel.changeData(compoundEventList.get(index));
                resultsPanelCL.show(resultsPanel, RESULTS_CARD);
            }
        }
    }*/

  /*  public void selectExperimentContainer(ExperimentContainer container) {
        this.showResultsPanel.changeData(container);
        compoundList.setSelectedValue(container, true);
        resultsPanelCL.show(resultsPanel, RESULTS_CARD);
    }

    public void selectExperimentContainer(ExperimentContainer container, SiriusResultElement element) {
        selectExperimentContainer(container);
        showResultsPanel.select(element, true);
    }*/


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

    public void drop(DropTargetDropEvent dtde) {
        final List<File> newFiles = DragAndDrop.getFileListFromDrop(dtde);

        if (newFiles.size() > 0) {
            importDragAndDropFiles(Arrays.asList(Workspace.resolveFileList(newFiles.toArray(new File[newFiles.size()]))));
        }
    }

    private void importDragAndDropFiles(List<File> rawFiles) {

        // entferne nicht unterstuetzte Files und suche nach CSVs

        // suche nach Sirius files
        final List<File> siriusFiles = new ArrayList<>();
        for (File f : rawFiles) {
            if (f.getName().toLowerCase().endsWith(".sirius")) {
                siriusFiles.add(f);
            }
        }
        if (siriusFiles.size() > 0) {
            Workspace.importWorkspace(siriusFiles);
        }

        DropImportDialog dropDiag = new DropImportDialog(this, rawFiles);
        if (dropDiag.getReturnValue() == ReturnValue.Abort) {
            return;
        }

        List<File> csvFiles = dropDiag.getCSVFiles();
        List<File> msFiles = dropDiag.getMSFiles();
        List<File> mgfFiles = dropDiag.getMGFFiles();

        if (csvFiles.isEmpty() && msFiles.isEmpty() && mgfFiles.isEmpty()) return;

        //Frage den Anwender ob er batch-Import oder alles zu einen Experiment packen moechte

        if ((csvFiles.size() > 0 && (msFiles.size() + mgfFiles.size() == 0)) ||
                (csvFiles.size() == 0 && msFiles.size() == 1 && mgfFiles.size() == 0)) {   //nur CSV bzw. nur ein File
            LoadController lc = new LoadController(this, CONFIG_STORAGE);
//			files

            lc.addSpectra(csvFiles, msFiles, mgfFiles);
            lc.showDialog();

            if (lc.getReturnValue() == ReturnValue.Success) {
                ExperimentContainer ec = lc.getExperiment();

                Workspace.importCompound(ec);
            }
        } else if (csvFiles.size() == 0 && mgfFiles.size() == 0 && msFiles.size() > 0) {
            Workspace.importOneExperimentPerFile(msFiles, mgfFiles);
        } else {
            DragAndDropOpenDialog diag = new DragAndDropOpenDialog(this);
            DragAndDropOpenDialogReturnValue rv = diag.getReturnValue();
            if (rv == DragAndDropOpenDialogReturnValue.abort) {
                return;
            } else if (rv == DragAndDropOpenDialogReturnValue.oneExperimentForAll) {
                LoadController lc = new LoadController(this, CONFIG_STORAGE);
                lc.addSpectra(csvFiles, msFiles, mgfFiles);
                lc.showDialog();

                if (lc.getReturnValue() == ReturnValue.Success) {
                    ExperimentContainer ec = lc.getExperiment();

                    Workspace.importCompound(ec);
                }
            } else if (rv == DragAndDropOpenDialogReturnValue.oneExperimentPerFile) {
                Workspace.importOneExperimentPerFile(msFiles, mgfFiles);
            }
        }
    }
}



