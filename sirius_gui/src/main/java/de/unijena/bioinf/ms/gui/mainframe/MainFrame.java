package de.unijena.bioinf.ms.gui.mainframe;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.gui.io.LoadController;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.io.projectspace.GuiProjectSpace;
import de.unijena.bioinf.ms.frontend.io.projectspace.InstanceBean;
import de.unijena.bioinf.ms.frontend.io.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.gui.compute.JobDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDropOpenDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.FileImportDialog;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListView;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.FilterableExperimentListPanel;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.projectspace.ProjectSpaceIO;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MainFrame extends JFrame implements DropTargetListener {
    public static final MainFrame MF = new MainFrame();

    // Project Space
    private GuiProjectSpace ps;

    public GuiProjectSpace getPS() {
        return ps;
    }

    //left side panel
    private CompoundList compoundList;

    public CompoundList getCompoundList() {
        return compoundList;
    }

    public EventList<InstanceBean> getCompounds() {
        return compoundList.getCompoundList();
    }

    public DefaultEventSelectionModel<InstanceBean> getCompoundListSelectionModel() {
        return compoundList.getCompoundListSelectionModel();
    }


    // right side panel
    private FormulaList formulaList;

    public FormulaList getFormulaList() {
        return formulaList;
    }

    private ResultPanel resultsPanel;

    public ResultPanel getResultsPanel() {
        return resultsPanel;
    }


    //job dialog
    private JobDialog jobDialog;

    public JobDialog getJobDialog() {
        return jobDialog;
    }

    //toolbar
    private SiriusToolbar toolbar;

    public SiriusToolbar getToolbar() {
        return toolbar;
    }


    //drop target for file input
    private DropTarget dropTarget;


    //internet connection monitor
    public static final ConnectionMonitor CONNECTION_MONITOR = new ConnectionMonitor();


    // some global switch that should better be within the property manager
    private boolean fingerid;

    public void setFingerIDEnabled(boolean enableFingerID) {
        fingerid = enableFingerID;
    }

    public boolean isFingerid() {
        return fingerid;
    }



    // methods for creating the mainframe
    private MainFrame() {
        super(ApplicationCore.VERSION_STRING());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this); //todo do we want to have the left table as drop target?
    }

    public void setTitlePath(String path) {
        setTitle(ApplicationCore.VERSION_STRING() + " on Project: '" + path + "'");
    }

    public void decoradeMainFrameInstance(@NotNull ProjectSpaceManager projectSpaceManager) {
        //create computation
        //todo get predictor from application core?

        // create project space
        ps = new GuiProjectSpace(projectSpaceManager);

        // create models for views
        compoundList = new CompoundList(ps);
        formulaList = new FormulaList(compoundList);


        //CREATE VIEWS
        jobDialog = new JobDialog(this);
        // results Panel
        resultsPanel = new ResultPanel(formulaList, ApplicationCore.WEB_API);

        toolbar = new SiriusToolbar();

        final JPanel mainPanel = new JPanel(new BorderLayout());

        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        add(mainPanel, BorderLayout.CENTER);

        //build left sidepane
        FilterableExperimentListPanel experimentListPanel = new FilterableExperimentListPanel(new ExperimentListView(compoundList));

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
            importDragAndDropFiles(resolveFileList(newFiles));
        }
    }



    // todo this should be somewhere else?
    private void importDragAndDropFiles(List<File> rawFiles) {
        rawFiles = new ArrayList<>(rawFiles);
        // entferne nicht unterstuetzte Files und suche nach CSVs
        // suche nach Sirius files
        //todo into fileimport dialog
        final List<File> siriusFiles = new ArrayList<>();
        final Iterator<File> rawFileIterator = rawFiles.iterator();
        while (rawFileIterator.hasNext()) {
            final File f = rawFileIterator.next();
            if (ProjectSpaceIO.isZipProjectSpace(f) || (f.isDirectory() && ProjectSpaceIO.isExistingProjectspaceDirectory(f))) {
                siriusFiles.add(f);
                rawFileIterator.remove();
            }
        }

        if (siriusFiles.size() > 0) {
            ps.importFromProjectSpace(siriusFiles);
        }

        FileImportDialog dropDiag = new FileImportDialog(this, rawFiles);
        if (dropDiag.getReturnValue() == de.unijena.bioinf.ms.gui.utils.ReturnValue.Abort) {
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
            ps.importOneExperimentPerLocation(msFiles, mgfFiles,null,null);
        } else {
            DragAndDropOpenDialog diag = new DragAndDropOpenDialog(this);
            DragAndDropOpenDialog.ReturnValue rv = diag.getReturnValue();
            if (rv == DragAndDropOpenDialog.ReturnValue.abort) {
            } else if (rv == DragAndDropOpenDialog.ReturnValue.oneExperimentForAll) {
                openImporterWindow(csvFiles, msFiles, mgfFiles);
            } else if (rv == DragAndDropOpenDialog.ReturnValue.oneExperimentPerFile) {
                ps.importOneExperimentPerLocation(msFiles, mgfFiles,null,null);
            }
        }
    }

    private void openImporterWindow(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        LoadController lc = new LoadController(this);
        lc.addSpectra(csvFiles, msFiles, mgfFiles);
        lc.showDialog();
    }

    public static File[] resolveFileList(File[] files) {
        List<File> l = resolveFileList(Arrays.asList(files));
        return l.toArray(new File[l.size()]);
    }

    public static List<File> resolveFileList(List<File> files) {
        final ArrayList<File> filelist = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory() && !ProjectSpaceIO.isExistingProjectspaceDirectory(f)) {
                final File[] fl = f.listFiles();
                if (fl != null) {
                    for (File g : fl)
                        if (!g.isDirectory()) filelist.add(g);
                }
            } else {
                filelist.add(f);
            }
        }
        return filelist;
    }

}



