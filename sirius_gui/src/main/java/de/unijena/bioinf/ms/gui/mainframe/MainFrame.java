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
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import de.unijena.bioinf.ms.frontend.core.ApplicationCore;
import de.unijena.bioinf.ms.frontend.subtools.InputFilesOptions;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.ImportAction;
import de.unijena.bioinf.ms.gui.actions.ProjectOpenAction;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.WarningDialog;
import de.unijena.bioinf.ms.gui.dialogs.input.DragAndDrop;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundListView;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.FilterableCompoundListPanel;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.LandingPage;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.ResultPanel;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchList;
import de.unijena.bioinf.ms.gui.utils.loading.SiriusCardLayout;
import de.unijena.bioinf.ms.properties.PropertyManager;
import de.unijena.bioinf.projectspace.InstanceBean;
import de.unijena.bioinf.projectspace.InstanceImporter;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import io.sirius.ms.sdk.model.ProjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.io.File;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.persistence.storage.SiriusProjectDocumentDatabase.SIRIUS_PROJECT_SUFFIX;

public class MainFrame extends JFrame implements DropTargetListener {

    public static final CookieManager cookieGuard = new CookieManager();

    static {
        CookieHandler.setDefault(cookieGuard);
    }

    //Logging Panel
    @Getter
    private final SiriusGui gui;
    private CompoundListView compoundListView;

    @Getter
    private FilterableCompoundListPanel filterableCompoundListPanel;

    public void ensureCompoundIsVisible(int index){
        compoundListView.ensureIndexIsVisible(index);
    }

    //left side panel
    @Getter
    private CompoundList compoundList;

    public EventList<InstanceBean> getCompounds() {
        return compoundList.getCompoundList();
    }

    public DefaultEventSelectionModel<InstanceBean> getCompoundListSelectionModel() {
        return compoundList.getCompoundListSelectionModel();
    }


    // right side panel
    private FormulaList formulaList;
    private StructureList databaseStructureList;
    private StructureList combinedStructureListSubstructureView;
    private StructureList combinedStructureListDeNovoView;
    private SpectralMatchList spectralMatchList;


    @Getter
    private ResultPanel resultsPanel;

    //toolbar
    @Getter
    private SiriusToolbar toolbar;


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
        Jobs.runEDTLater(() -> setTitlePath(gui.getProjectManager().getProjectLocation()));

        // create models for views
        compoundList = new CompoundList(gui);
        formulaList = new FormulaList(compoundList);
        databaseStructureList = new StructureList(compoundList, (inst, k, loadDatabaseHits, loadDenovo) -> inst.getStructureCandidates(k, true), false);
        combinedStructureListSubstructureView = new StructureList(compoundList, (inst, k, loadDatabaseHits, loadDenovo) -> inst.getBothStructureCandidates(k, true, loadDatabaseHits, loadDenovo), true);
        combinedStructureListDeNovoView = new StructureList(compoundList, (inst, k, loadDatabaseHits, loadDenovo) -> inst.getBothStructureCandidates(k, true, loadDatabaseHits, loadDenovo), true);
        spectralMatchList = new SpectralMatchList(compoundList);


        //CREATE VIEWS
        // results Panel
        ProjectInfo projectInfo = getGui().applySiriusClient((c,pid) ->
                c.projects().getProjectSpace(pid, List.of(ProjectInfoOptField.NONE)));

        resultsPanel = new ResultPanel(databaseStructureList, combinedStructureListSubstructureView, combinedStructureListDeNovoView, formulaList, spectralMatchList, gui);
        resultsPanel.showLcmsTab(EnumSet.of(ProjectType.ALIGNED_RUNS, ProjectType.UNALIGNED_RUNS).contains(projectInfo.getType()));
        JPanel resultPanelContainer = new JPanel(new BorderLayout());
        resultPanelContainer.setBorder(BorderFactory.createEmptyBorder());
        resultPanelContainer.add(resultsPanel, BorderLayout.CENTER);
        if (PropertyManager.getBoolean("de.unijena.bioinf.webservice.infopanel", false))
            resultPanelContainer.add(new WebServiceInfoPanel(gui.getConnectionMonitor()), BorderLayout.SOUTH);

        SiriusCardLayout layout = new SiriusCardLayout();
        JPanel landingPanelSwitcher = new JPanel(layout);
        LandingPage landingPage = new LandingPage(gui, ApplicationCore.WEB_API.getAuthService());

        landingPanelSwitcher.add("landing", landingPage);
        landingPanelSwitcher.add("results", resultPanelContainer);

        compoundList.getSortedSource().addListEventListener(listEvent -> {
            if (listEvent.getSourceList().isEmpty())
                layout.show(landingPanelSwitcher, "landing");
            else
                layout.show(landingPanelSwitcher, "results");
        });
        layout.show(landingPanelSwitcher, compoundList.getFullSize() < 1 ? "landing" : "results");

        // toolbar
        toolbar = new SiriusToolbar(gui);

        final JSplitPane mainPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
        add(mainPanel, BorderLayout.CENTER);

        //build left sidepane
        compoundListView = new CompoundListView(gui, compoundList);
        filterableCompoundListPanel = new FilterableCompoundListPanel(compoundListView);
        filterableCompoundListPanel.setPreferredSize(new Dimension(228, (int) filterableCompoundListPanel.getPreferredSize().getHeight()));
        mainPanel.setDividerLocation(232);

        //BUILD the MainFrame (GUI)
        mainPanel.setLeftComponent(filterableCompoundListPanel);
        mainPanel.setRightComponent(landingPanelSwitcher);
        add(toolbar, BorderLayout.NORTH);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(new Dimension((int) (screen.width * .7), (int) (screen.height * .7)));
        setLocationRelativeTo(null); //init mainframe
        setVisible(true);
        toFront();
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
        List<File> files = DragAndDrop.getFileListFromDrop(this, dtde);
        //todo projectspace: add project file check to open project via drag and drop


        final InputFilesOptions inputF = new InputFilesOptions();
        inputF.msInput = Jobs.runInBackgroundAndLoad(this, "Analyzing Dropped Files...", false,
                InstanceImporter.makeExpandFilesJJob(files)).getResult();

        List<Path> projectFiles = inputF.msInput.unknownFiles.keySet().stream().filter(p -> p.toString().endsWith(SIRIUS_PROJECT_SUFFIX)).toList();
        inputF.msInput.unknownFiles.clear();
        if (!projectFiles.isEmpty()){
            Boolean replaceCurrent = projectFiles.size() == 1 ? null : false;
            ProjectOpenAction opener = (ProjectOpenAction) SiriusActions.LOAD_WS.getInstance(gui);
            projectFiles.forEach(f -> opener.openProject(f, replaceCurrent));

        }
        if (!inputF.msInput.isEmpty())
            importDragAndDropFiles(inputF); //does not support importing projects
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