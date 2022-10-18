/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.mainframe;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.rest.NetUtils;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.frontend.subtools.gui.GuiAppOptions;
import de.unijena.bioinf.ms.gui.compute.JobDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.io.LoadController;
import de.unijena.bioinf.ms.gui.io.spectrum.csv.CSVFormatReader;
import de.unijena.bioinf.ms.gui.logging.LogDialog;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListView;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.FilterableExperimentListPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.net.ConnectionMonitor;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MainFrame extends JFrame implements DropTargetListener {

    public static final CookieManager cookieGuard = new CookieManager();
    public static final MainFrame MF = new MainFrame();

    static {
        CookieHandler.setDefault(cookieGuard);
    }

    //Logging Panel
    private final LogDialog log;

    public LogDialog getLogConsole() {
        return log;
    }

    // Project Space
    private GuiProjectSpaceManager ps;

    public GuiProjectSpaceManager ps() {
        return ps;
    }

    private BasicEventList<InstanceBean> compoundBaseList;

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


    public ConnectionMonitor CONNECTION_MONITOR() {
        return CONNECTION_MONITOR;
    }

    //internet connection monitor
    private final ConnectionMonitor CONNECTION_MONITOR;

    // methods for creating the mainframe
    private MainFrame() {
        super(ApplicationCore.VERSION_STRING());
        //inti connection monitor
        CONNECTION_MONITOR = new ConnectionMonitor();

        setIconImage(Icons.SIRIUS_APP_IMAGE);
        configureTaskbar();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

        log = new LogDialog(null,false, Level.INFO); //todo property
    }

    //if we want to add taskbar stuff we can configure this here
    private void configureTaskbar() {
        if (Taskbar.isTaskbarSupported()){
            LoggerFactory.getLogger(getClass()).debug("Adding Taskbar support");
            if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE))
                Taskbar.getTaskbar().setIconImage(Icons.SIRIUS_APP_IMAGE);
        }
    }

    public void setTitlePath(String path) {
        setTitle(ApplicationCore.VERSION_STRING() + " on Project: '" + path + "'");
    }


    public void openNewProjectSpace(Path selFile) {
        changeProject(() -> new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).openExistingProjectSpace(selFile));
    }

    public void createNewProjectSpace(Path selFile) {
        changeProject(() -> new ProjectSpaceIO(ProjectSpaceManager.newDefaultConfig()).createNewProjectSpace(selFile));
    }

    protected void changeProject(IOFunctions.IOSupplier<SiriusProjectSpace> makeSpace) {
        final BasicEventList<InstanceBean> psList = compoundBaseList;
        final AtomicBoolean compatible = new AtomicBoolean(true);
        this.ps = Jobs.runInBackgroundAndLoad(MF, "Opening new Project...", () -> {
            GuiProjectSpaceManager old = this.ps;
            try {
                final SiriusProjectSpace ps = makeSpace.get();
                compatible.set(InstanceImporter.checkDataCompatibility(ps, NetUtils.checkThreadInterrupt(Thread.currentThread())) == null);
                Jobs.cancelAllRuns();
                final GuiProjectSpaceManager gps = new GuiProjectSpaceManager(ps, psList, PropertyManager.getInteger(GuiAppOptions.COMPOUND_BUFFER_KEY, 10));
                inEDTAndWait(() -> MF.setTitlePath(gps.projectSpace().getLocation().toString()));

                gps.projectSpace().addProjectSpaceListener(event -> {
                    if (event.equals(ProjectSpaceEvent.LOCATION_CHANGED))
                        inEDTAndWait(() -> MF.setTitlePath(gps.projectSpace().getLocation().toString()));
                });
                return gps;
            } finally {
                old.close();
            }
        }).getResult();

        if (this.ps == null) {
            try {
                LoggerFactory.getLogger(getClass()).warn("Error when changing project-space. Falling back to tmp project-space");
                createNewProjectSpace(ProjectSpaceIO.createTmpProjectSpaceLocation());
            } catch (IOException e) {
                new StacktraceDialog(MF, "Cannot recreate a valid project-space due to: " + e.getMessage() + "'.  SIRIUS will not work properly without valid project-space. Please restart SIRIUS.", e);
            }
        }
        if (!compatible.get())
            if (new QuestionDialog(MF, "<html><body>" +
                    "The opened project-space contains results based on an outdated fingerprint version.<br><br>" +
                    "You can either convert the project to the new fingerprint version and <b>lose all fingerprint related results</b> (e.g. CSI:FingerID an CANOPUS),<br>" +
                    "or you stay with the old fingerprint version but without being able to execute any fingerprint related computations (e.g. for data visualization).<br><br>" +
                    "Do you wish to convert and lose all fingerprint related results?" +
                    "</body></html>").isSuccess())
                ps().updateFingerprintData();
    }

    public void decoradeMainFrameInstance(@NotNull GuiProjectSpaceManager projectSpaceManager) {
        //add project-space
        ps = projectSpaceManager;
        compoundBaseList = ps.INSTANCE_LIST;
        inEDTAndWait(() -> MF.setTitlePath(ps.projectSpace().getLocation().toString()));

        // create models for views
        compoundList = new CompoundList(ps);
        formulaList = new FormulaList(compoundList);


        //CREATE VIEWS
        jobDialog = new JobDialog(this);
        // results Panel
        resultsPanel = new ResultPanel(formulaList, ApplicationCore.WEB_API);
        JPanel resultPanelContainer = new JPanel(new BorderLayout());
        resultPanelContainer.setBorder(BorderFactory.createEmptyBorder());
        resultPanelContainer.add(resultsPanel,BorderLayout.CENTER);
        if (PropertyManager.getBoolean("de.unijena.bioinf.webservice.infopanel", false))
            resultPanelContainer.add(new WebServiceInfoPanel(CONNECTION_MONITOR()), BorderLayout.SOUTH);

        // toolbar
        toolbar = new SiriusToolbar();

        final JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        add(mainPanel, BorderLayout.CENTER);

        //build left sidepane
        FilterableExperimentListPanel experimentListPanel = new FilterableExperimentListPanel(new ExperimentListView(compoundList));
        experimentListPanel.setPreferredSize(new Dimension(228, (int) experimentListPanel.getPreferredSize().getHeight()));
        mainPanel.setDividerLocation(232);

        //BUILD the MainFrame (GUI)
        mainPanel.setLeftComponent(experimentListPanel);
        mainPanel.setRightComponent(resultPanelContainer);
        add(toolbar, BorderLayout.NORTH);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(new Dimension((int) (screen.width * .7), (int) (screen.height * .7)));
        MainFrame.MF.setLocationRelativeTo(null); //init mainframe
        setVisible(true);
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

    public static final String DONT_ASK_OPEN_KEY = "de.unijena.bioinf.sirius.dragdrop.open.dontAskAgain";

    @Override
    public void drop(DropTargetDropEvent dtde) {
        boolean openNewProject = false;

        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = Jobs.runInBackgroundAndLoad(MF, "Analyzing Dropped Files...", false,
                InstanceImporter.makeExpandFilesJJob(DragAndDrop.getFileListFromDrop(dtde))).getResult();

        if (!inputF.msInput.isEmpty()) {
            if (inputF.msInput.isSingleProject())
                openNewProject = new QuestionDialog(MF, "<html><body>Do you want to open the dropped Project instead of importing it? <br> The currently opened project will be closed!</br></body></html>"/*, DONT_ASK_OPEN_KEY*/).isSuccess();

            if (openNewProject) {
                MF.openNewProjectSpace(inputF.msInput.projects.keySet().iterator().next());
            } else {
                importDragAndDropFiles(inputF);
            }
        }
    }


    private void importDragAndDropFiles(InputFilesOptions files) {
        ps.importOneExperimentPerLocation(files); //import all batch mode importable file types (e.g. .sirius, project-dir, .ms, .mgf, .mzml, .mzxml)

        // check if unknown files contain csv files with spectra
        final CSVFormatReader csvChecker = new CSVFormatReader();
        List<File> csvFiles = files.msInput != null ? files.msInput.unknownFiles.keySet().stream().map(Path::toFile)
                .filter(f -> csvChecker.isCompatible(f) || f.getName().toLowerCase().endsWith(".txt"))
                .collect(Collectors.toList()) : Collections.emptyList();

        if (!csvFiles.isEmpty())
            openImporterWindow(csvFiles, Collections.emptyList(), Collections.emptyList());
    }

    private void openImporterWindow(List<File> csvFiles, List<File> msFiles, List<File> mgfFiles) {
        LoadController lc = new LoadController(this);
        lc.addSpectra(csvFiles, msFiles, mgfFiles);
        lc.showDialog();
    }

    public static void inEDTAndWait(@NotNull final Runnable run) {
        if (SwingUtilities.isEventDispatchThread()) {
            run.run();
        } else {
            try {
                Jobs.runEDTAndWait(run);
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}