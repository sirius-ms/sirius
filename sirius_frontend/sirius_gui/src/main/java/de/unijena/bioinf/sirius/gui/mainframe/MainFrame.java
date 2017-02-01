package de.unijena.bioinf.sirius.gui.mainframe;

import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.actions.SiriusActionManager;
import de.unijena.bioinf.sirius.gui.compute.BackgroundComputation;
import de.unijena.bioinf.sirius.gui.compute.JobDialog;
import de.unijena.bioinf.sirius.gui.db.DatabaseDialog;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.ext.DragAndDrop;
import de.unijena.bioinf.sirius.gui.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.sirius.gui.fingerid.ConfidenceList;
import de.unijena.bioinf.sirius.gui.fingerid.VersionsInfo;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentList;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListView;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.FilterableExperimentListPanel;
import de.unijena.bioinf.sirius.gui.mainframe.results.ResultPanel;
import de.unijena.bioinf.sirius.gui.mainframe.results.result_element_view.FormulaTable;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static de.unijena.bioinf.sirius.gui.mainframe.Workspace.CONFIG_STORAGE;

public class MainFrame extends JFrame implements DropTargetListener {
    public static final MainFrame MF = new MainFrame();

    static {
        decoradeMainFrameInstance(MF);
    }

    //left side panel
    private ExperimentList experimentList;

    public ExperimentList getExperimentList() {
        return experimentList;
    }

    private CSIFingerIdComputation csiFingerId;

    public CSIFingerIdComputation getCsiFingerId() {
        return csiFingerId;
    }

    private JobDialog jobDialog;

    public JobDialog getJobDialog() {
        return jobDialog;
    }

    private SiriusToolbar toolbar;

    public SiriusToolbar getToolbar() {
        return toolbar;
    }


    private FormulaTable formulaList;

    public FormulaTable getFormulaList() {
        return formulaList;
    }

    private ResultPanel resultsPanel;

    public ResultPanel getResultsPanel() {
        return resultsPanel;
    }


    private JPanel resultsContainerPanel;
    private CardLayout resultsPanelCL;

    private static final String DUMMY_CARD = "dummy";
    private static final String RESULTS_CARD = "results";
    private DatabaseDialog dbDialog;

    private BackgroundComputation backgroundComputation;

    private boolean removeWithoutWarning = false; //todo config storage

    private DropTarget dropTarget;
    private ConfidenceList confidenceList;


    private MainFrame() {
        super(ApplicationCore.VERSION_STRING);
    }

    private static void decoradeMainFrameInstance(final MainFrame mf) {

        mf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        this.addWindowListener(this);
        mf.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setActionMap(SiriusActionManager.ROOT_MANAGER);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        mf.add(mainPanel, BorderLayout.CENTER);


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

        mf.backgroundComputation = new BackgroundComputation(mf.csiFingerId);


        /////////////////////////////// LEFT Panel (Compunt list) //////////////////////////////
        mf.experimentList = new ExperimentList();
        /////////////////////////////// LEFT Panel (Compunt list) DONE //////////////////////////////

        mf.formulaList = new FormulaTable(mf.experimentList);

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


        // results PAnel
        mf.resultsPanelCL = new CardLayout();
        mf.resultsContainerPanel = new JPanel(mf.resultsPanelCL);
        JPanel dummyPanel = new JPanel();
        mf.resultsContainerPanel.add(dummyPanel, DUMMY_CARD);

        mf.resultsPanel = new ResultPanel(mf.formulaList);

        // resluts done

        mf.toolbar = new SiriusToolbar();


        mf.dropTarget = new DropTarget(mf, DnDConstants.ACTION_COPY_OR_MOVE, mf);


        SiriusActionManager.initRootManager();


        FilterableExperimentListPanel experimentListPanel = new FilterableExperimentListPanel(new ExperimentListView(mf.experimentList));

        //BUILD the MainFrame (GUI)
        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.addTab("Experiments", experimentListPanel);
        tabbedPane.addTab("Identifications", new JPanel());
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setPreferredSize(new Dimension(218, (int) tabbedPane.getPreferredSize().getHeight()));
        mainPanel.add(tabbedPane, BorderLayout.WEST);
        mainPanel.add(mf.resultsPanel, BorderLayout.CENTER);
        mf.add(mf.toolbar, BorderLayout.NORTH);
        mf.setSize(new Dimension(1368, 1024));


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
        resultsPanel.dispose();
        csiFingerId.shutdown();
        super.dispose();
    }

    public BackgroundComputation getBackgroundComputation() {
        return backgroundComputation;
    }

    public List<ExperimentContainer> getCompounds() {
        return experimentList.getCompoundList();
    }

    public DefaultEventSelectionModel<ExperimentContainer> getCompoundListSelectionModel() {
        return experimentList.getCompoundListSelectionModel();
    }

    /*public List<ExperimentContainer> getSelectedCompounds() {
        return experimentList.compoundListView.getSelectedValuesList();
    }*/

//    public EventList<ExperimentContainer> getCompoundsList() {
//        return compoundList;
//    }

    /* public ListModel getCompoundModel() {
        return compoundModel;
    }*/

   /* public void refreshCompound(final ExperimentContainer c) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (compoundListView.getSelectedValue() == c) {
                    resultsPanel.changeData(c);
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


    //todo i think we nee a listener for this
    /*@Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getSource() == this.compoundListView) {

            if (index < 0) {

                this.resultsPanel.changeData(null);
            } else {

                this.resultsPanel.changeData(compoundList.get(index));
                resultsPanelCL.show(resultsContainerPanel, RESULTS_CARD);
            }
        }
    }*/

  /*  public void selectExperimentContainer(ExperimentContainer container) {
        this.resultsPanel.changeData(container);
        compoundListView.setSelectedValue(container, true);
        resultsPanelCL.show(resultsContainerPanel, RESULTS_CARD);
    }

    public void selectExperimentContainer(ExperimentContainer container, SiriusResultElement element) {
        selectExperimentContainer(container);
        resultsPanel.select(element, true);
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

    public boolean csiConnectionAvailable() {
        //Test connection
        if (!WebAPI.getRESTDb(BioFilter.ALL).testConnection()) {
            new NoConnectionDialog(this);
            return false;
        }
        return true;
    }
}



