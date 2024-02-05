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

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.ImportAction;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.JobDialog;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.logging.LogDialog;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.ExperimentListView;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.FilterableExperimentListPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.InstanceImporter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MainFrame extends JFrame implements DropTargetListener {

    public static final CookieManager cookieGuard = new CookieManager();

    static {
        CookieHandler.setDefault(cookieGuard);
    }

    //Logging Panel
    private final LogDialog log;
    private final SiriusGui gui;

    public SiriusGui getGui() {
        return gui;
    }

    public LogDialog getLogConsole() {
        return log;
    }
//    private GuiProjectSpaceManager ps;

    private boolean closeProjectOnDispose = true;

    public boolean isCloseProjectOnDispose() {
        return closeProjectOnDispose;
    }

    public void setCloseProjectOnDispose(boolean closeProjectOnDispose) {
        this.closeProjectOnDispose = closeProjectOnDispose;
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

    private StructureList structureList;

    public StructureList getStructureList() {
        return structureList;
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


    private final ActionMap globalActions = new ActionMap();

    @NotNull
    public ActionMap getGlobalActions() {
        return globalActions;
    }

    // methods for creating the mainframe
    public MainFrame(SiriusGui gui) {
        super(ApplicationCore.VERSION_STRING());
        //inti connection monitor
        setIconImage(Icons.SIRIUS_APP_IMAGE);
        configureTaskbar();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());
        new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

        log = new LogDialog(null, false, Level.INFO); //todo property

        this.gui = gui;
    }

    //if we want to add taskbar stuff we can configure this here
    private void configureTaskbar() {
        if (Taskbar.isTaskbarSupported()) {
            LoggerFactory.getLogger(getClass()).debug("Adding Taskbar support");
            if (Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE))
                Taskbar.getTaskbar().setIconImage(Icons.SIRIUS_APP_IMAGE);
        }
    }

    public void setTitlePath(String path) {
        setTitle(ApplicationCore.VERSION_STRING() + " on Project: '" + path + "'");
    }


    public void decoradeMainFrame() {
        Jobs.runEDTAndWaitLazy(() -> setTitlePath(gui.getProjectManager().getProjectLocation()));

        // create models for views
        compoundList = new CompoundList(gui);
        formulaList = new FormulaList(compoundList);
        structureList = new StructureList(compoundList);


        //CREATE VIEWS
        jobDialog = new JobDialog(this);
        // results Panel
        resultsPanel = new ResultPanel(structureList, formulaList, compoundList, gui);
        JPanel resultPanelContainer = new JPanel(new BorderLayout());
        resultPanelContainer.setBorder(BorderFactory.createEmptyBorder());
        resultPanelContainer.add(resultsPanel, BorderLayout.CENTER);
        if (PropertyManager.getBoolean("de.unijena.bioinf.webservice.infopanel", false))
            resultPanelContainer.add(new WebServiceInfoPanel(gui.getConnectionMonitor()), BorderLayout.SOUTH);

        // toolbar
        toolbar = new SiriusToolbar(gui);

        final JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        add(mainPanel, BorderLayout.CENTER);

        //build left sidepane
        FilterableExperimentListPanel experimentListPanel = new FilterableExperimentListPanel(new ExperimentListView(gui, compoundList));
        experimentListPanel.setPreferredSize(new Dimension(228, (int) experimentListPanel.getPreferredSize().getHeight()));
        mainPanel.setDividerLocation(232);

        //BUILD the MainFrame (GUI)
        mainPanel.setLeftComponent(experimentListPanel);
        mainPanel.setRightComponent(resultPanelContainer);
        add(toolbar, BorderLayout.NORTH);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(new Dimension((int) (screen.width * .7), (int) (screen.height * .7)));
        setLocationRelativeTo(null); //init mainframe
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

    public static final String DONT_ASK_NEW_WINDOW_KEY = "de.unijena.bioinf.sirius.dragdrop.newWindow.dontAskAgain";

    @Override
    public void drop(DropTargetDropEvent dtde) {
        boolean openInNewWindow = false;

        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = Jobs.runInBackgroundAndLoad(this, "Analyzing Dropped Files...", false,
                InstanceImporter.makeExpandFilesJJob(DragAndDrop.getFileListFromDrop(this, dtde))).getResult();

        if (!inputF.msInput.isEmpty()) {
            if (inputF.msInput.isSingleProject()) {
                openInNewWindow = new QuestionDialog(this, "<html><body>Open the dropped Project in an additional Window? Otherwise the current one will be replace. </body></html>", DONT_ASK_NEW_WINDOW_KEY).isSuccess();

                if (openInNewWindow) {
                    // todo nightsky open new project via API
                    // todo nightsky prevent importing projects jsut ask whether it should opened in the new window or not!
                    throw new IllegalStateException("openNewProject in new window not yet implemented");
//                openNewProjectSpace(inputF.msInput.projects.keySet().iterator().next());
                } else {
                    throw new IllegalStateException("openNewProject in same window not yet implemented");
                }
            } else {
                importDragAndDropFiles(inputF); //does not support importing projects
            }
        }
    }


    private void importDragAndDropFiles(InputFilesOptions files) {
        //import all batch mode importable file types (e.g. .ms, .mgf, .mzml, .mzxml)
        ((ImportAction) SiriusActions.IMPORT_EXP_BATCH.getInstance(gui)).importOneExperimentPerLocation(files, this);

        if (files.msInput != null && !files.msInput.unknownFiles.isEmpty()) {
            new WarningDialog(this, "The following files are not supported and will not be Imported: "
                    + files.msInput.unknownFiles.keySet().stream().map(Path::toString)
                    .collect(Collectors.joining(", ")));
        }
    }
}