package de.unijena.bioinf.sirius.gui.mainframe;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.actions.SiriusActions;
import de.unijena.bioinf.sirius.gui.compute.BackgroundComputation;
import de.unijena.bioinf.sirius.gui.compute.JobDialog;
import de.unijena.bioinf.sirius.gui.compute.JobLog;
import de.unijena.bioinf.sirius.gui.dialogs.DragAndDropOpenDialog;
import de.unijena.bioinf.sirius.gui.dialogs.DragAndDropOpenDialogReturnValue;
import de.unijena.bioinf.sirius.gui.dialogs.NewsDialog;
import de.unijena.bioinf.sirius.gui.dialogs.UpdateDialog;
import de.unijena.bioinf.sirius.gui.ext.DragAndDrop;
import de.unijena.bioinf.sirius.gui.fingerid.CSIFingerIdComputation;
import de.unijena.bioinf.sirius.gui.fingerid.VersionsInfo;
import de.unijena.bioinf.sirius.gui.fingerid.WebAPI;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentList;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListView;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.FilterableExperimentListPanel;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

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

    public EventList<ExperimentContainer> getCompounds() {
        return experimentList.getCompoundList();
    }

    public DefaultEventSelectionModel<ExperimentContainer> getCompoundListSelectionModel() {
        return experimentList.getCompoundListSelectionModel();
    }

    private FormulaList formulaList;

    public FormulaList getFormulaList() {
        return formulaList;
    }

    private ResultPanel resultsPanel;

    public ResultPanel getResultsPanel() {
        return resultsPanel;
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

    private BackgroundComputation backgroundComputation;

    public BackgroundComputation getBackgroundComputation() {
        return backgroundComputation;
    }

    private DropTarget dropTarget;


    private MainFrame() {
        super(ApplicationCore.VERSION_STRING);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this); //todo do we want to have the left table as drop target?
    }

    private static void decoradeMainFrameInstance(final MainFrame mf) {
        //create computation
        mf.csiFingerId = new CSIFingerIdComputation();
        mf.backgroundComputation = new BackgroundComputation(mf.csiFingerId);

        // create models for views
        mf.experimentList = new ExperimentList();
        mf.formulaList = new FormulaList(mf.experimentList);


        //CREATE VIEWS
        mf.jobDialog = new JobDialog(mf);
        // results Panel
        mf.resultsPanel = new ResultPanel(mf.formulaList);

        mf.toolbar = new SiriusToolbar();


        //Init actions
//        SiriusActionManager.initRootManager();

        final JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        mf.add(mainPanel, BorderLayout.CENTER);

        //build left sidepane
        FilterableExperimentListPanel experimentListPanel = new FilterableExperimentListPanel(new ExperimentListView(mf.experimentList));

        //BUILD the MainFrame (GUI)
        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.addTab("Compounds", experimentListPanel);
        tabbedPane.addTab("Identifications", new JPanel());
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setPreferredSize(new Dimension(218, (int) tabbedPane.getPreferredSize().getHeight()));
        mainPanel.add(tabbedPane, BorderLayout.WEST);
        mainPanel.add(mf.resultsPanel, BorderLayout.CENTER);
        mf.add(mf.toolbar, BorderLayout.NORTH);
        mf.setSize(new Dimension(1368, 1024));


        //finger id observer
        final SwingWorker w = new SwingWorker<VersionsInfo, VersionsInfo>() {
            @Override
            protected VersionsInfo doInBackground() {
                VersionsInfo result = null;
                try (WebAPI api = WebAPI.newInstance()) {
                    if (api.isConnected())
                        result = api.needsUpdate();
                    LoggerFactory.getLogger(mf.getClass()).debug("FingerID response " + (result != null ? String.valueOf(result.toString()) : "NULL"));
                } catch (Exception e) {
                    LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
                }

                publish(result);
                return result;
            }

            @Override
            protected void process(List<VersionsInfo> chunks) {
                super.process(chunks);
                final VersionsInfo versionsNumber = chunks.get(0);
                if (versionsNumber != null) {
                    mf.csiFingerId.setVersionNumber(versionsNumber);
                    if (versionsNumber.outdated()) {
                        new UpdateDialog(mf, versionsNumber.siriusGuiVersion.toString());
                    } else {
                        mf.csiFingerId.setEnabled(true);
                    }
                    if (versionsNumber.hasNews()) {
                        new NewsDialog(mf, versionsNumber.getNews());
                    }
                } else {
                    SiriusActions.CHECK_CONNECTION.getInstance().actionPerformed(null);
                }
            }

            @Override
            protected void done() {
                super.done();
            }
        };

        w.execute();


        //this is just to not lost the exceptions
        try {
            w.get();
        } catch (Exception e) {
            LoggerFactory.getLogger(mf.getClass()).error("Error during connection test", e);
        }

        mf.setVisible(true);
    }

    @Override
    public void dispose() {
        resultsPanel.dispose();
        csiFingerId.shutdown();
        super.dispose();
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
            importDragAndDropFiles(Arrays.asList(Workspace.resolveFileList(newFiles.toArray(new File[newFiles.size()]))));
        }
    }

    protected static Pattern CANOPUS_PATTERN = Pattern.compile("canopus[^.]*\\.data(?:\\.gz)?", Pattern.CASE_INSENSITIVE);
    private void importDragAndDropFiles(List<File> rawFiles) {
        rawFiles = new ArrayList<>(rawFiles);
        // entferne nicht unterstuetzte Files und suche nach CSVs
        // suche nach Sirius files
        final List<File> siriusFiles = new ArrayList<>();
        final Iterator<File> rawFileIterator = rawFiles.iterator();
        while (rawFileIterator.hasNext()) {
            final File f = rawFileIterator.next();
            if (f.getName().toLowerCase().endsWith(".sirius")) {
                siriusFiles.add(f);
                rawFileIterator.remove();
            } else if (CANOPUS_PATTERN.matcher(f.getName()).matches()) {
                importCanopus(f);
                rawFileIterator.remove();
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

    private void importCanopus(final File f) {
        final SwingWorker<Object,Object> worker = new SwingWorker<Object, Object>() {
            @Override
            protected Object doInBackground() throws Exception {
                final JobLog.Job j = JobLog.getInstance().submit("Load CANOPUS", "Load CANOPUS prediction model");
                try {
                    getCsiFingerId().loadCanopus(f);
                } catch (Exception e) {
                    j.error(e.getMessage(), e);
                    return null;
                }
                j.done();
                return null;
            }

            @Override
            protected void done() {
                super.done();
                activateCanopus();
            }
        };
        worker.execute();
    }

    private void activateCanopus() {
        resultsPanel.enableCanopus();
    }


}



