package de.unijena.bioinf.sirius.gui.mainframe;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import de.unijena.bioinf.ChemistryBase.ms.ft.TreeScoring;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.compute.*;
import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.db.DatabaseDialog;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.ext.DragAndDrop;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedBatchDataFormatFilter;
import de.unijena.bioinf.sirius.gui.filefilter.SupportedExportCSVFormatsFilter;
import de.unijena.bioinf.sirius.gui.fingerid.*;
import de.unijena.bioinf.sirius.gui.io.SiriusDataConverter;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultPanel;
import de.unijena.bioinf.sirius.gui.mainframe.results.results_table.SiriusResultTablePanel;
import de.unijena.bioinf.sirius.gui.settings.TwoCloumnPanel;
import de.unijena.bioinf.sirius.gui.structure.ComputingStatus;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import de.unijena.bioinf.sirius.gui.utils.Colors;
import de.unijena.bioinf.sirius.gui.utils.Icons;
import de.unijena.bioinf.sirius.gui.utils.ToolbarButton;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
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

public class MainFrame extends JFrame implements WindowListener, ActionListener, ListSelectionListener, DropTargetListener, MouseListener, KeyListener, JobLog.JobListener {

    private FilterList<ExperimentContainer> compoundEventList;
    public final JList<ExperimentContainer> compoundList;


    private ToolbarButton newB, loadB, saveB, batchB, computeAllB, exportResultsB, configFingerID, jobs, db, settings, bug, about;
    public final CSIFingerIdComputation csiFingerId;

    private HashSet<String> names;
    private int nameCounter;

    private JPanel resultsPanel;
    private CardLayout resultsPanelCL;
    private ResultPanel showResultsPanel;
    private static final String DUMMY_CARD = "dummy";
    private static final String RESULTS_CARD = "results";
    private ConfigStorage config;
    private JobDialog jobDialog;
    //    private Icon jobRunning, jobNotRunning;
    private DatabaseDialog dbDialog;

    private BackgroundComputation backgroundComputation;

    private boolean removeWithoutWarning = false;

    private DropTarget dropTarget;
    private ConfidenceList confidenceList;
    private JPopupMenu expPopMenu;
    private JMenuItem newExpMI, batchMI, editMI, closeMI, computeMI, cancelMI;
    private boolean computeAllActive;


    public ConfigStorage getConfig() {
        return config;
    }

    public MainFrame() {
        super(ApplicationCore.VERSION_STRING);
        csiFingerId = new CSIFingerIdComputation(new CSIFingerIdComputation.Callback() {
            @Override
            public void computationFinished(final ExperimentContainer container, final SiriusResultElement element) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refreshCompound(container);
                    }
                });
            }
        });


        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        computeAllActive = false;

        this.config = new ConfigStorage();

        this.backgroundComputation = new BackgroundComputation(this);

        nameCounter = 1;
        this.names = new HashSet<>();

        this.addWindowListener(this);
        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        this.add(mainPanel, BorderLayout.CENTER);


        /////////////////////////////// LEFT Panel (Compunt list) //////////////////////////////
        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        JTextField searchField = new JTextField();
        BasicEventList<ExperimentContainer> compountBaseList = new BasicEventList<>();
        compoundEventList = new FilterList<>(new ObservableElementList<>(compountBaseList, GlazedLists.beanConnector(ExperimentContainer.class)),
                new TextComponentMatcherEditor<>(searchField, new TextFilterator<ExperimentContainer>() {
                    @Override
                    public void getFilterStrings(List<String> baseList, ExperimentContainer element) {
                        baseList.add(element.getGUIName());
                        baseList.add(element.getIonization().toString());
                        baseList.add(String.valueOf(element.getFocusedMass()));
                    }
                }, true));


        compoundList = new JList<>(new DefaultEventListModel<>(compoundEventList));
        compoundList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        compoundList.setCellRenderer(new CompoundCellRenderer());
        compoundList.addListSelectionListener(this);
        compoundList.setMinimumSize(new Dimension(200, 0));
        compoundList.addMouseListener(this);


        JPanel tmp_wrapper = new JPanel(new BorderLayout());
        JLabel label = new JLabel("<html>Confidence score is an experimental feature.Use with caution.<html>");
        tmp_wrapper.add(label, BorderLayout.NORTH);


        confidenceList = new ConfidenceList(compountBaseList);
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
        });

        JScrollPane pane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setViewportView(compoundList);

        TwoCloumnPanel compoundListPanel = new TwoCloumnPanel(new JLabel(" Filter:"), searchField);
        compoundListPanel.add(pane, 0, true);
        compoundListPanel.setBorder(new EmptyBorder(0,0,0,0));

        tabbedPane.addTab("Experiments", compoundListPanel);
        tabbedPane.addTab("Identifications", tmp_wrapper);
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setPreferredSize(new Dimension(218, (int) tabbedPane.getPreferredSize().getHeight()));

        mainPanel.add(tabbedPane, BorderLayout.WEST);
        /////////////////////////////// LEFT Panel (Compunt list) DONE //////////////////////////////


        // results PAnel
        resultsPanelCL = new CardLayout();
        resultsPanel = new JPanel(resultsPanelCL);
        JPanel dummyPanel = new JPanel();
        resultsPanel.add(dummyPanel, DUMMY_CARD);

        showResultsPanel = new ResultPanel(this, config);
        mainPanel.add(showResultsPanel, BorderLayout.CENTER);
        // resluts done



        // ########## Toolbar ############
        JToolBar controlPanel = new JToolBar();
        controlPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Colors.ICON_BLUE));

        newB = new ToolbarButton("Import", Icons.DOC_32);
        newB.addActionListener(this);
        newB.setToolTipText("Import measurements of a single compound");
        controlPanel.add(newB);

        batchB = new ToolbarButton("Batch Import", Icons.DOCS_32);
        batchB.addActionListener(this);
        batchB.setToolTipText("Import measurements of several compounds");

        controlPanel.add(batchB);
        controlPanel.addSeparator(new Dimension(20, 20));

        loadB = new ToolbarButton("Load Workspace", Icons.FOLDER_OPEN_32);
        loadB.addActionListener(this);
        loadB.setToolTipText("Load all experiments and computed results from a previously saved workspace.");
        controlPanel.add(loadB);
        saveB = new ToolbarButton("Save Workspace", Icons.FOLDER_CLOSE_32);
        saveB.addActionListener(this);
        saveB.setEnabled(false);
        controlPanel.add(saveB);
        controlPanel.addSeparator(new Dimension(20, 20));
//
        computeAllB = new ToolbarButton("Compute All", Icons.RUN_32);
        computeAllB.addActionListener(this);
        computeAllB.setEnabled(false);
        controlPanel.add(computeAllB);

        exportResultsB = new ToolbarButton("Export Results", Icons.EXPORT_32);
        exportResultsB.addActionListener(this);
        exportResultsB.setEnabled(false);
        controlPanel.add(exportResultsB);
        controlPanel.addSeparator(new Dimension(20, 20));
//        controlPanel.add(Box.createGlue());

        configFingerID = new ToolbarButton("CSI:FingerId", Icons.FINGER_32);
        configFingerID.addActionListener(this);
        configFingerID.setEnabled(false);

        controlPanel.add(configFingerID);
        controlPanel.addSeparator(new Dimension(20, 20));
//        controlPanel.add(Box.createGlue());

        //todo implement database menu
        db = new ToolbarButton("Database", Icons.DB_32);
        /*controlPanel.add(db);
        db.addActionListener(this);
        controlPanel.addSeparator(new Dimension(20,20));*/


        jobs = new ToolbarButton("Jobs", Icons.FB_LOADER_STOP_32);
        jobDialog = new JobDialog(this);
        jobs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jobDialog.showDialog();
            }
        });
        controlPanel.add(jobs);
        controlPanel.addSeparator(new Dimension(20, 20));
        controlPanel.add(Box.createGlue());
        controlPanel.addSeparator(new Dimension(20, 20));
        JobLog.getInstance().addListener(this);

        settings = new ToolbarButton("Settings", Icons.GEAR_32);
        settings.setToolTipText("Settings");
        settings.addActionListener(this);
        controlPanel.add(settings);

        bug = new ToolbarButton("Bug Report", Icons.BUG_32);
        bug.setToolTipText("Report a bug or send a feature request");
        bug.addActionListener(this);
        controlPanel.add(bug);

        about = new ToolbarButton("About", Icons.INFO_32);
        about.setToolTipText("About Sirius");
        about.addActionListener(this);
        controlPanel.add(about);

        controlPanel.setRollover(true);
        controlPanel.setFloatable(false);
        //Toolbar end

        this.add(controlPanel, BorderLayout.NORTH);

        this.dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

        constructExperimentListPopupMenu();
        {
            KeyStroke delKey = KeyStroke.getKeyStroke("DELETE");
            KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
            String delAction = "deleteItems";
            compoundList.getInputMap().put(delKey, delAction);
            compoundList.getInputMap().put(enterKey, "compute");
            compoundList.getActionMap().put(delAction, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    deleteCurrentCompound();
                }
            });
            compoundList.getActionMap().put("compute", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
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
                    csiFingerId.setVersionNumber(versionsNumber);
                    if (versionsNumber.outdated()) {
                        new UpdateDialog(MainFrame.this, versionsNumber.siriusGuiVersion);
                    } else {
                        configFingerID.setEnabled(true);
                        csiFingerId.setEnabled(true);
                    }
                    if (versionsNumber.hasNews()){
                        new NewsDialog(MainFrame.this, versionsNumber.getNews());
                    }
                } else {
                    new NoConnectionDialog(MainFrame.this);
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

    public void constructExperimentListPopupMenu() {
        expPopMenu = new JPopupMenu();
        newExpMI = new JMenuItem("Import Experiment", Icons.ADD_DOC_16);
        batchMI = new JMenuItem("Batch Import", Icons.BATCH_DOC_16);
        editMI = new JMenuItem("Edit Experiment", Icons.EDIT_16);
        closeMI = new JMenuItem("Remove Experiment(s)", Icons.REMOVE_DOC_16);
        computeMI = new JMenuItem("Compute", Icons.RUN_16);
        cancelMI = new JMenuItem("Cancel Computation", Icons.CANCEL_16);


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

    public Iterator<ExperimentContainer> getCompounds() {
        return compoundEventList.iterator();
    }

    public EventList<ExperimentContainer> getCompoundsList() {
        return compoundEventList;
    }

    /* public ListModel getCompoundModel() {
        return compoundModel;
    }*/

    public void refreshCompound(final ExperimentContainer c) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refreshResultListFor(c);
                refreshComputationMenuItem();
                refreshExportMenuButton();
                c.fireUpdateEvent();
            }
        });

    }

    private void refreshResultListFor(ExperimentContainer c) {
        if (compoundList.getSelectedValue() == c) {
            showResultsPanel.changeData(c);
        }
    }

    private void refreshExportMenuButton() {
        final Iterator<ExperimentContainer> ecs = getCompounds();
        while (ecs.hasNext()) {
            final ExperimentContainer e = ecs.next();
            if (e.getComputeState() == ComputingStatus.COMPUTED) {
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
                if (ec != null && (ec.isComputing() || ec.isQueued())) {
                    cancelMI.setEnabled(true);
                } else {
                    cancelMI.setEnabled(false);
                }
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == configFingerID) {
            final FingerIdDialog dialog = new FingerIdDialog(this, csiFingerId, null, true);
            final int returnState = dialog.run();
            if (returnState == FingerIdDialog.COMPUTE_ALL) {
                csiFingerId.computeAll(getCompounds());
            }else if (returnState == FingerIdDialog.COMPUTE){
                csiFingerId.computeAll(compoundList.getSelectedValuesList().iterator());
            }
        } else if (e.getSource() == newB || e.getSource() == newExpMI) {
            LoadController lc = new LoadController(this, config);
            lc.showDialog();
            if (lc.getReturnValue() == ReturnValue.Success) {
                ExperimentContainer ec = lc.getExperiment();

                importCompound(ec);
            }
        } else if (e.getSource() == exportResultsB) {
            exportResults();
        } else if (e.getSource() == computeMI) {
            computeCurrentCompound();
        } else if (e.getSource() == computeAllB) {
            if (computeAllActive) {
                cancelComputation();
            } else {
                final BatchComputeDialog dia = new BatchComputeDialog(this, compoundEventList);
            }
        } else if (e.getSource() == cancelMI) {
            for (ExperimentContainer ec : compoundList.getSelectedValuesList()) {
                backgroundComputation.cancel(ec);
            }
        } else if (e.getSource() == saveB) {

            JFileChooser jfc = new JFileChooser();
            jfc.setCurrentDirectory(config.getDefaultSaveFilePath());
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jfc.setAcceptAllFileFilterUsed(false);
            jfc.addChoosableFileFilter(new SiriusSaveFileFilter());

            File selectedFile = null;

            while (selectedFile == null) {
                int returnval = jfc.showSaveDialog(this);
                if (returnval == JFileChooser.APPROVE_OPTION) {
                    File selFile = jfc.getSelectedFile();
                    config.setDefaultSaveFilePath(selFile.getParentFile());

                    String name = selFile.getName();
                    if (!selFile.getAbsolutePath().endsWith(".sirius")) {
                        selFile = new File(selFile.getAbsolutePath() + ".sirius");
                    }

                    if (selFile.exists()) {
                        FilePresentDialog fpd = new FilePresentDialog(this, selFile.getName());
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
                            return compoundList.getModel().getElementAt(index);
                        }

                        @Override
                        public int size() {
                            return compoundList.getModel().getSize();
                        }
                    }, selectedFile);
                } catch (Exception e2) {
                    new ErrorReportDialog(this, e2.getMessage());
                }

            }


        } else if (e.getSource() == loadB) {

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
        } else if (e.getSource() == closeMI) {
            deleteCurrentCompound();
        } else if (e.getSource() == editMI) {
            ExperimentContainer ec = this.compoundList.getSelectedValue();
            if (ec == null) return;
            String guiname = ec.getGUIName();

            LoadController lc = new LoadController(this, ec, config);
            lc.showDialog();
            if (lc.getReturnValue() == ReturnValue.Success) {
//				ExperimentContainer ec = lc.getExperiment();

                if (!ec.getGUIName().equals(guiname)) {
                    while (true) {
                        if (ec.getGUIName() != null && !ec.getGUIName().isEmpty()) {
                            if (this.names.contains(ec.getGUIName())) {
                                ec.setSuffix(ec.getSuffix() + 1);
                            } else {
                                this.names.add(ec.getGUIName());
                                break;
                            }
                        } else {
                            ec.setName("Unknown");
                            ec.setSuffix(1);
                        }
                    }
                }
                this.compoundList.repaint();
            }
        } else if (e.getSource() == batchB || e.getSource() == batchMI) {
            JFileChooser chooser = new JFileChooser(config.getDefaultLoadDialogPath());
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setMultiSelectionEnabled(true);
            chooser.addChoosableFileFilter(new SupportedBatchDataFormatFilter());
            chooser.setAcceptAllFileFilterUsed(false);
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] files = chooser.getSelectedFiles();
                config.setDefaultLoadDialogPath(files[0].getParentFile());
                importOneExperimentPerFile(files);
            }


            //zu unfangreich, extra Methode

        } else if (e.getSource() == db) {
            if (dbDialog == null) dbDialog = new DatabaseDialog(this, config);
            dbDialog.setVisible(true);
        } else if (e.getSource() == about) {
            new AboutDialog(this);
        } else if (e.getSource() == settings) {
            new SettingsDialog(this);
        } else if (e.getSource() == bug) {
            new BugReportDialog(this);
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
        } catch (InterruptedException | ExecutionException e1) {
            LoggerFactory.getLogger(this.getClass()).error(e1.getMessage(), e1);
        }
        worker.flushBuffer();
        if (worker.hasErrorMessage()) {
            new ErrorReportDialog(this, worker.getErrorMessage());
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

        while (selectedFile == null) {
            int returnval = jfc.showSaveDialog(this);
            if (returnval == JFileChooser.APPROVE_OPTION) {
                File selFile = jfc.getSelectedFile();
                if (selFile == null) continue;
                config.setCsvExportPath((selFile.exists() && selFile.isDirectory()) ? selFile : selFile.getParentFile());

                if (accessory.isSingleFile()) {
                    String name = selFile.getName();
                    if (!name.endsWith(".csv") && !name.endsWith(".tsv")) {
                        selFile = new File(selFile.getAbsolutePath() + ".csv");
                    }

                    if (selFile.exists()) {
                        FilePresentDialog fpd = new FilePresentDialog(this, selFile.getName());
                        ReturnValue rv = fpd.getReturnValue();
                        if (rv == ReturnValue.Success) {
                            selectedFile = selFile;
                        }
                    } else {
                        selectedFile = selFile;
                    }

                } else {
                    if (!selFile.exists()) {
                        selFile.mkdirs();
                    }
                }
                selectedFile = selFile;
                break;
            } else {
                break;
            }
        }

        if (selectedFile == null) return;
        if (accessory.isSingleFile()) {
            try (final BufferedWriter fw = new BufferedWriter(new FileWriter(selectedFile))) {
                final Iterator<ExperimentContainer> ecs = getCompounds();
                while (ecs.hasNext()) {
                    final ExperimentContainer ec = ecs.next();
                    if (ec.isComputed() && ec.getResults().size() > 0) {
                        IdentificationResult.writeIdentifications(fw, SiriusDataConverter.experimentContainerToSiriusExperiment(ec), ec.getRawResults());
                    }
                }
            } catch (IOException e) {
                new ErrorReportDialog(this, e.toString());
            }
        } else {
            try {
                writeMultiFiles(selectedFile, accessory.isExportingSirius(), accessory.isExportingFingerId());
            } catch (IOException e) {
                new ErrorReportDialog(this, e.toString());
            }
        }
    }

    private void writeMultiFiles(File selectedFile, boolean withSirius, boolean withFingerid) throws IOException {
        final Iterator<ExperimentContainer> containers = getCompounds();
        final HashSet<String> names = new HashSet<>();
        while (containers.hasNext()) {
            final ExperimentContainer container = containers.next();
            if (container.getResults() == null || container.getResults().size() == 0) continue;
            final String name;
            {
                String origName = escapeFileName(container.getName());
                String aname = origName;
                int i = 0;
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
                        final TreeScoring scoring = result.getResolvedTree().getAnnotationOrNull(TreeScoring.class);
                        bw.write(String.valueOf(result.getResolvedTree().numberOfVertices()));
                        bw.write('\t');
                        bw.write(scoring == null ? "\"\"" : String.valueOf(scoring.getExplainedIntensity()));
                        bw.write('\n');
                    }
                }
            }
            if (withFingerid) {
                final ArrayList<FingerIdData> datas = new ArrayList<>();
                for (SiriusResultElement elem : container.getResults()) {
                    if (elem.getFingerIdData() == null) continue;
                    datas.add(elem.getFingerIdData());
                }
                final File resultFile = new File(selectedFile, name + ".csv");
                new CSVExporter().exportToFile(resultFile, datas);
            }
        }
    }

    private String escapeFileName(String name) {
        final String n = name.replaceAll("[:\\\\/*\"?|<>']", "");
        if (n.length() > 128) {
            return n.substring(0, 128);
        } else return n;
    }

    private void computeCurrentCompound() {
        List<ExperimentContainer> ecs = compoundList.getSelectedValuesList();
        if (ecs != null && !ecs.isEmpty()) {
            new BatchComputeDialog(this, ecs);
        }
    }

    private void deleteCurrentCompound() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                List<ExperimentContainer> toRemove = compoundList.getSelectedValuesList();

                if (!config.isCloseNeverAskAgain()) {
                    CloseDialogNoSaveReturnValue diag = new CloseDialogNoSaveReturnValue(MainFrame.this, "When removing the selected experiment(s) you will loose all computed identification results?", config);
                    CloseDialogReturnValue val = diag.getReturnValue();
                    if (val == CloseDialogReturnValue.abort) return;
                }
                for (ExperimentContainer cont : toRemove) {
                    backgroundComputation.cancel(cont);
                    names.remove(cont.getGUIName());
                }
                compoundEventList.removeAll(toRemove);
                compoundList.clearSelection();
            }
        });
    }

    public void importOneExperimentPerFile(List<File> msFiles, List<File> mgfFiles) {
        BatchImportDialog batchDiag = new BatchImportDialog(this);
        batchDiag.start(msFiles, mgfFiles);

        List<ExperimentContainer> ecs = batchDiag.getResults();
        List<String> errors = batchDiag.getErrors();
        importOneExperimentPerFileStep2(ecs, errors);
    }

    public void importOneExperimentPerFile(File[] files) {
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
                if (fl != null) {
                    for (File g : fl)
                        if (!g.isDirectory()) filelist.add(g);
                }
            } else {
                filelist.add(f);
            }
        }
        return filelist.toArray(new File[filelist.size()]);
    }

    public void importOneExperimentPerFileStep2(List<ExperimentContainer> ecs, List<String> errors) {
        if (ecs != null) {
            for (ExperimentContainer ec : ecs) {
                if (ec == null) {
                    continue;
                } else {
                    importCompound(ec);
                }
            }
        }


        if (errors != null) {
            if (errors.size() > 1) {
                ErrorListDialog elDiag = new ErrorListDialog(this, errors);
            } else if (errors.size() == 1) {
                ErrorReportDialog eDiag = new ErrorReportDialog(this, errors.get(0));
            }

        }
    }


    public void importCompound(final ExperimentContainer ec) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (ec.getGUIName() != null && !ec.getGUIName().isEmpty()) {
                        if (names.contains(ec.getGUIName())) {
                            ec.setSuffix(ec.getSuffix() + 1);
                        } else {
                            names.add(ec.getGUIName());
                            break;
                        }
                    } else {
                        ec.setName("Unknown");
                        ec.setSuffix(1);
                    }
                }
                compoundEventList.add(ec);
                compoundList.setSelectedValue(ec, true);
                if (ec.getResults().size() > 0) ec.setComputeState(ComputingStatus.COMPUTED);
                if (ec.getComputeState() == ComputingStatus.COMPUTED) {
                    exportResultsB.setEnabled(true);
                }
            }
        });
    }

    public void clearWorkspace() {
        this.names.clear();
        this.compoundEventList.clear();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == this.compoundList) {
            if (this.compoundEventList.size() > 0) {
                this.computeAllB.setEnabled(true);
            } else {
                this.computeAllB.setEnabled(false);
            }
            int index = compoundList.getSelectedIndex();
            refreshComputationMenuItem();
            if (index < 0) {
                saveB.setEnabled(false);

                closeMI.setEnabled(false);
                editMI.setEnabled(false);
                computeMI.setEnabled(false);
                this.showResultsPanel.changeData(null);
            } else {
                saveB.setEnabled(true);

                closeMI.setEnabled(true);
                editMI.setEnabled(true);
                computeMI.setEnabled(true);
                this.showResultsPanel.changeData(compoundEventList.get(index));
                resultsPanelCL.show(resultsPanel, RESULTS_CARD);
            }
        }
    }

    public void selectExperimentContainer(ExperimentContainer container) {
        this.showResultsPanel.changeData(container);
        compoundList.setSelectedValue(container, true);
        resultsPanelCL.show(resultsPanel, RESULTS_CARD);
    }

    public void selectExperimentContainer(ExperimentContainer container, SiriusResultElement element) {
        selectExperimentContainer(container);
        showResultsPanel.select(element, true);
    }

    public void computationStarted() {
        this.computeAllActive = true;
        this.computeAllB.setText("    Cancel    "); //todo: ugly hack to prevent button resizing in toolbar, find nice solution
        this.computeAllB.setIcon(Icons.CANCEL_32);
    }

    public void computationComplete() {
        // check if computation is complete
        this.computeAllActive = false;
        this.computeAllB.setText("Compute All");
        this.computeAllB.setIcon(Icons.RUN_32);
    }

    public void cancelComputation() {
        for (ExperimentContainer c : backgroundComputation.cancelAll()) {
            refreshCompound(c);
        }
        computationCanceled();
    }

    public void computationCanceled() {
        this.computeAllActive = false;
        this.computeAllB.setText("Compute All");
        this.computeAllB.setIcon(Icons.RUN_32);
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

    public void drop(DropTargetDropEvent dtde) {
        final List<File> newFiles = DragAndDrop.getFileListFromDrop(dtde);

        if (newFiles.size() > 0) {
            importDragAndDropFiles(Arrays.asList(resolveFileList(newFiles.toArray(new File[newFiles.size()]))));
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
            importWorkspace(siriusFiles);
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
            LoadController lc = new LoadController(this, config);
//			files

            lc.addSpectra(csvFiles, msFiles, mgfFiles);
            lc.showDialog();

            if (lc.getReturnValue() == ReturnValue.Success) {
                ExperimentContainer ec = lc.getExperiment();

                importCompound(ec);
            }
        } else if (csvFiles.size() == 0 && mgfFiles.size() == 0 && msFiles.size() > 0) {
            importOneExperimentPerFile(msFiles, mgfFiles);
        } else {
            DragAndDropOpenDialog diag = new DragAndDropOpenDialog(this);
            DragAndDropOpenDialogReturnValue rv = diag.getReturnValue();
            if (rv == DragAndDropOpenDialogReturnValue.abort) {
                return;
            } else if (rv == DragAndDropOpenDialogReturnValue.oneExperimentForAll) {
                LoadController lc = new LoadController(this, config);
                lc.addSpectra(csvFiles, msFiles, mgfFiles);
                lc.showDialog();

                if (lc.getReturnValue() == ReturnValue.Success) {
                    ExperimentContainer ec = lc.getExperiment();

                    importCompound(ec);
                }
            } else if (rv == DragAndDropOpenDialogReturnValue.oneExperimentPerFile) {
                importOneExperimentPerFile(msFiles, mgfFiles);
            }
        }
    }

    /////////////////// Mouselistener ///////////////////////

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getSource().equals(compoundList)) {
            if (e.getClickCount() == 2) {
                // Double-click detected
                int index = compoundList.locationToIndex(e.getPoint());
                compoundList.setSelectedIndex(index);
                computeCurrentCompound();
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            int indx = compoundList.locationToIndex(e.getPoint());
            boolean select = true;
            for (int i : compoundList.getSelectedIndices()) {
                if (indx == i) {
                    select = false;
                    break;
                }
            }
            if (select){
                compoundList.setSelectedIndex(indx);
            }
        }
        if (e.isPopupTrigger()) {
            this.expPopMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // fleisch thinks that is not needed
        /*if (e.isPopupTrigger()) {
            this.expPopMenu.show(e.getComponent(), e.getX(), e.getY());
        }*/
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    public int numberOfCompounds() {
        return compoundEventList.size();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == 27) {
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (JobLog.getInstance().hasActiveJobs()) {
                    jobs.setIcon(Icons.FB_LOADER_RUN_32);
                } else jobs.setIcon(Icons.FB_LOADER_STOP_32);
            }
        });
    }

    @Override
    public void jobIsRunning(JobLog.Job job) {
        jobIsSubmitted(job);
    }

    @Override
    public void jobIsDone(final JobLog.Job job) {
        jobIsSubmitted(job);
    }

    @Override
    public void jobIsFailed(JobLog.Job job) {
        jobIsSubmitted(job);
    }

    @Override
    public void jobDescriptionChanged(JobLog.Job job) {

    }
}

class SiriusSaveFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;
        String name = f.getName();
        if (name.endsWith(".sirius")) {
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return ".sirius";
    }

}

