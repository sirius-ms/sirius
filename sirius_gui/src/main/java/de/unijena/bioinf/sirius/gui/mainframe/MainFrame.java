package de.unijena.bioinf.sirius.gui.mainframe;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.fingerid.CSIFingerIDComputation;
import de.unijena.bioinf.fingerid.webapi.VersionsInfo;
import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.sirius.core.ApplicationCore;
import de.unijena.bioinf.sirius.gui.compute.JobDialog;
import de.unijena.bioinf.sirius.gui.dialogs.*;
import de.unijena.bioinf.sirius.gui.ext.DragAndDrop;
import de.unijena.bioinf.sirius.gui.io.WorkspaceIO;
import de.unijena.bioinf.sirius.gui.load.LoadController;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentList;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.ExperimentListView;
import de.unijena.bioinf.sirius.gui.mainframe.experiments.FilterableExperimentListPanel;
import de.unijena.bioinf.sirius.gui.mainframe.molecular_formular.FormulaList;
import de.unijena.bioinf.sirius.gui.net.ConnectionMonitor;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.ReturnValue;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class MainFrame extends JFrame implements DropTargetListener {
    public static final MainFrame MF = new MainFrame();

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

    private CSIFingerIDComputation csiFingerId;

    public CSIFingerIDComputation getCsiFingerId() {
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

    private DropTarget dropTarget;

    public static final ConnectionMonitor CONECTION_MONITOR = new ConnectionMonitor();


    private MainFrame() {
        super(ApplicationCore.VERSION_STRING);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this); //todo do we want to have the left table as drop target?
    }

    public void decoradeMainFrameInstance() {
        //create computation
        csiFingerId = new CSIFingerIDComputation();

        // create models for views
        experimentList = new ExperimentList();
        formulaList = new FormulaList(experimentList);


        //CREATE VIEWS
        jobDialog = new JobDialog(this);
        // results Panel
        resultsPanel = new ResultPanel(formulaList);

        toolbar = new SiriusToolbar();

        final JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        add(mainPanel, BorderLayout.CENTER);

        //build left sidepane
        FilterableExperimentListPanel experimentListPanel = new FilterableExperimentListPanel(new ExperimentListView(experimentList));

        //BUILD the MainFrame (GUI)
        final JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
        tabbedPane.addTab("Compounds", experimentListPanel);
        tabbedPane.addTab("Identifications", new JPanel());
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setPreferredSize(new Dimension(218, (int) tabbedPane.getPreferredSize().getHeight()));
        mainPanel.add(tabbedPane, BorderLayout.WEST);
        mainPanel.add(resultsPanel, BorderLayout.CENTER);
        add(toolbar, BorderLayout.NORTH);
        setSize(new Dimension(1368, 1024));

        setVisible(true);
    }

    @Override
    public void dispose() {
        resultsPanel.dispose();
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

    @Override
    public void drop(DropTargetDropEvent dtde) {
        final List<File> newFiles = DragAndDrop.getFileListFromDrop(dtde);

        if (newFiles.size() > 0) {
            importDragAndDropFiles(Arrays.asList(WorkspaceIO.resolveFileList(newFiles.toArray(new File[newFiles.size()]))));
        }
    }

    protected static Pattern CANOPUS_PATTERN = Pattern.compile("canopus[^.]*\\.data(?:\\.gz)?", Pattern.CASE_INSENSITIVE);

    private void importDragAndDropFiles(List<File> rawFiles) {
        rawFiles = new ArrayList<>(rawFiles);
        // entferne nicht unterstuetzte Files und suche nach CSVs
        // suche nach Sirius files
        //todo into fileimport dialog
        final List<File> siriusFiles = new ArrayList<>();
        final Iterator<File> rawFileIterator = rawFiles.iterator();
        while (rawFileIterator.hasNext()) {
            final File f = rawFileIterator.next();
            if (f.getName().toLowerCase().endsWith(".sirius") || (f.isDirectory() && WorkspaceIO.isSiriusWorkspaceDirectory(f))) {
                siriusFiles.add(f);
                rawFileIterator.remove();
            }
            //todo CANOPUS
            /*else if (CANOPUS_PATTERN.matcher(f.getName()).matches()) {
                importCanopus(f);
                rawFileIterator.remove();
            }*/
        }

        if (siriusFiles.size() > 0) {
            WorkspaceIO.importWorkspace(siriusFiles);
        }

        FileImportDialog dropDiag = new FileImportDialog(this, rawFiles);
        if (dropDiag.getReturnValue() == ReturnValue.Abort) {
            return;
        }

        List<File> csvFiles = dropDiag.getCSVFiles();
        List<File> msFiles = dropDiag.getMSFiles();
        List<File> mgfFiles = dropDiag.getMGFFiles();

        if (csvFiles.isEmpty() && msFiles.isEmpty() && mgfFiles.isEmpty()) return;

        //Frage den Anwender ob er batch-Import oder alles zu einen Experiment packen moechte
        if ((csvFiles.size() > 0 && (msFiles.size() + mgfFiles.size() == 0))) {   //nur CSV bzw. nur ein File
            openImporterWindow(csvFiles, msFiles, mgfFiles);
        } else if (csvFiles.size() == 0 && mgfFiles.size() == 0 && msFiles.size() > 0) {
            WorkspaceIO.importOneExperimentPerFile(msFiles, mgfFiles);
        } else {
            DragAndDropOpenDialog diag = new DragAndDropOpenDialog(this);
            DragAndDropOpenDialogReturnValue rv = diag.getReturnValue();
            if (rv == DragAndDropOpenDialogReturnValue.abort) {
            } else if (rv == DragAndDropOpenDialogReturnValue.oneExperimentForAll) {
                openImporterWindow(csvFiles, msFiles, mgfFiles);
            } else if (rv == DragAndDropOpenDialogReturnValue.oneExperimentPerFile) {
                WorkspaceIO.importOneExperimentPerFile(msFiles, mgfFiles);
            }
        }
    }

    private void openImporterWindow(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        LoadController lc = new LoadController(this);
        lc.addSpectra(csvFiles, msFiles, mgfFiles);
        lc.showDialog();

        ExperimentContainer ec = lc.getExperiment();
        if (ec != null) {
            Workspace.importCompound(ec);
        }
    }
    //todo insert canopus here
    /*private void importCanopus(final File f) {
        final SwingWorker<Object, Object> worker = new SwingWorker<Object, Object>() {
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
    }*/

    private void activateCanopus() {
        resultsPanel.enableCanopus();
    }


}



