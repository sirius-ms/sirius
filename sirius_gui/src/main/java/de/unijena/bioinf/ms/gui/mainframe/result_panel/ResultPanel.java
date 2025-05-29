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

package de.unijena.bioinf.ms.gui.mainframe.result_panel;

import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassBean;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassList;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.fingerid.fingerprints.FingerprintList;
import de.unijena.bioinf.ms.gui.lcms_viewer.LCMSViewerPanel;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.*;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaListHeaderPanel;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchList;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfoStore;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourUtils;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import io.sirius.ms.sdk.model.CanopusPrediction;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import io.sirius.ms.sdk.model.ProjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ResultPanel extends JTabbedPane {

    protected static final Logger logger = LoggerFactory.getLogger(ResultPanel.class);

    private final CompoundList compoundList;
    private final SiriusGui gui;

    private final FormulaOverviewPanel formulasTab;

    private LCMSViewerPanel lcmsTab;
    private final CandidateListDetailViewPanel structuresTab;
    private final DeNovoStructureListDetailViewPanel deNovoStructuresTab;
    @Getter
    private final EpimetheusPanel structureAnnoTab;
    private final FingerprintPanel fingerprintTab;
    private final CompoundClassPanel canopusTab;
    private final StructEditPanel structEditPanel;
    private SpectralMatchingPanel spectralMatchingTab;
    private KendrickMassDefectPanel massDefectTab;

    private final String spectralMatchingTabName = "Library Matches";
    private final String massDefectTabName = "Homologue Series";

    private StructureList databaseStructureList;
    private StructureList combinedStructureListSubstructureView;
    private StructureList combinedStructureListDeNovoView;
    private StructureList combinedStructureListSketcherView;
    private FormulaList siriusResultElements;
    private SpectralMatchList spectralMatchList;
    private CompoundClassList compoundClassList;
    private FingerprintList fingerprintList;

    private ProjectType type;

    public ResultPanel(@NotNull CompoundList compoundList, @NotNull SiriusGui gui) {
        super();
        this.gui = gui;
        //these are the two base lists for all views.
        this.compoundList = compoundList; //feature level list (InstanceBean)
        this.siriusResultElements = new FormulaList(compoundList); // formula result level list.

        //lcms tab
        //Not need to clean up the listener since compoundlist will be invalidated together with ResultPanel
        compoundList.getSortedSource().addListEventListener(evt -> {
            if (type == null && !evt.getSourceList().isEmpty()) {
                type = gui.applySiriusClient((c, pid) ->
                        c.projects().getProject(pid, List.of(ProjectInfoOptField.NONE))).getType();
                showLcmsTab(EnumSet.of(ProjectType.ALIGNED_RUNS, ProjectType.UNALIGNED_RUNS).contains(type));
            }
        });
        //Check for first time if project has already a type.
        this.type = gui.applySiriusClient((c, pid) ->
                c.projects().getProject(pid, List.of(ProjectInfoOptField.NONE))).getType();
        showLcmsTab(EnumSet.of(ProjectType.ALIGNED_RUNS, ProjectType.UNALIGNED_RUNS).contains(type));


        // formulas tabs
        formulasTab = new FormulaOverviewPanel(siriusResultElements, gui);
        addTab("Formulas", null, formulasTab, formulasTab.getDescription());

        // fingerprint tab
        fingerprintList = null;
        try {
            fingerprintList = new FingerprintList(siriusResultElements, gui);
        } catch (IOException e) {
            logger.error("Error when loading FingerprintList. Fingerprint tab will not be available.", e);
        }
        fingerprintTab = fingerprintList == null ? null : new FingerprintPanel(fingerprintList);
        FormulaListHeaderPanel formulaHeaderFingerprint;
        if (fingerprintList != null) {
            formulaHeaderFingerprint = new FormulaListHeaderPanel(siriusResultElements, fingerprintTab);
            formulaHeaderFingerprint.addTutorialInformationToCompactView(SoftwareTourInfoStore.Fingerprint_Formulas);
            addTab("Predicted Fingerprints", null, formulaHeaderFingerprint, fingerprintTab.getDescription());
            fingerprintList.addActiveResultChangedListener((instanceBean, sre, resultElements, selections) -> {
                checkAndInitFingerprintSoftwareTour(formulaHeaderFingerprint, instanceBean, gui);
            });
        } else {
            formulaHeaderFingerprint = null;
        }


        // canopus tab
        compoundClassList = new CompoundClassList(siriusResultElements,
                sre -> sre.getCanopusPrediction()
                        .stream().map(CanopusPrediction::getClassyFireClasses).filter(Objects::nonNull)
                        .flatMap(List::stream).map(CompoundClassBean::new).toList());
        canopusTab = new CompoundClassPanel(compoundClassList, siriusResultElements, gui);
        FormulaListHeaderPanel formulaHeaderCanopus = new FormulaListHeaderPanel(siriusResultElements, canopusTab);
        formulaHeaderCanopus.addTutorialInformationToCompactView(SoftwareTourInfoStore.Canopus_Formulas);
        compoundClassList.addActiveResultChangedListener((instanceBean, sre, resultElements, selections) -> {
            checkAndInitCanopusSoftwareTour(formulaHeaderCanopus, instanceBean, gui);
        });
        addTab("Compound Classes", null, formulaHeaderCanopus, canopusTab.getDescription());

        // structure db search tab
        databaseStructureList = new StructureList(compoundList, (inst, k, loadDatabaseHits, loadDenovo) -> inst.getStructureCandidates(k, true), false);
        structuresTab = new CandidateListDetailViewPanel(this, databaseStructureList, gui);
        addTab("Structures", null, structuresTab, structuresTab.getDescription());

        // combined denovo structure db search tabs
        combinedStructureListDeNovoView = new StructureList(compoundList, (inst, k, loadDatabaseHits, loadDenovo) -> inst.getBothStructureCandidates(k, true, loadDatabaseHits, loadDenovo), true);
        deNovoStructuresTab = new DeNovoStructureListDetailViewPanel(this, combinedStructureListDeNovoView, gui);
        addTab("De Novo Structures", null, deNovoStructuresTab, deNovoStructuresTab.getDescription());

        // substructure annotation tab
        combinedStructureListSubstructureView = new StructureList(compoundList, (inst, k, loadDatabaseHits, loadDenovo) -> inst.getBothStructureCandidates(k, true, loadDatabaseHits, loadDenovo), true);
        structureAnnoTab = new EpimetheusPanel(combinedStructureListSubstructureView);
        addTab("Substructure Annotations", null, structureAnnoTab, structureAnnoTab.getDescription());

        combinedStructureListSketcherView = new StructureList(compoundList, (inst, k, loadDatabaseHits, loadDenovo) -> inst.getBothStructureCandidates(k, true, loadDatabaseHits, loadDenovo), true);
        structEditPanel = new StructEditPanel(combinedStructureListSketcherView);
        addTab("Structure Sketcher",null,structEditPanel,structEditPanel.getDescription());


        //software tour listener
        addChangeListener(e -> {
            Component selectedComponent = getSelectedComponent();

            if (selectedComponent == formulasTab && siriusResultElements.getSelectedElement() != null) {
                //formulas tab
                formulasTab.initSoftwareTour(gui.getProperties());
            } else if (selectedComponent == formulaHeaderFingerprint && siriusResultElements.getSelectedElement() != null) {
                //fingerprint tab
                checkAndInitFingerprintSoftwareTour(formulaHeaderFingerprint, siriusResultElements.getSelectedElement(), gui);
            } else if (selectedComponent == formulaHeaderCanopus && siriusResultElements.getSelectedElement() != null) {
                //canopus tab
                checkAndInitCanopusSoftwareTour(formulaHeaderCanopus, siriusResultElements.getSelectedElement(), gui);
            } else if (selectedComponent == structureAnnoTab && combinedStructureListSubstructureView.getSelectedElement() != null) {
                //epimetheus tab
                structureAnnoTab.initSoftwareTour(gui.getProperties());
            } else if (selectedComponent == structuresTab && !databaseStructureList.getElementList().isEmpty()) {
                //database search tab
                structuresTab.initSoftwareTour(gui.getProperties());
            } else if (selectedComponent == deNovoStructuresTab && combinedStructureListDeNovoView.getElementList().stream().anyMatch(c -> c.isDeNovo())) {
                //de novo structures tab
                deNovoStructuresTab.initSoftwareTour(gui.getProperties());
            }
        });


        // global spectra match search list
        gui.getProperties().addPropertyChangeListener("showSpectraMatchPanel", evt ->
                showSpectralMatchingTab((Boolean) evt.getNewValue()));
        showSpectralMatchingTab(gui.getProperties().isShowSpectraMatchPanel());

        // KMD plot
        gui.getProperties().addPropertyChangeListener("showHomologueSeriesPanel", evt ->
                showHomologueSeriesTab((Boolean) evt.getNewValue()));
        showHomologueSeriesTab(gui.getProperties().isShowHomologueSeriesPanel());
    }

    private void checkAndInitCanopusSoftwareTour(FormulaListHeaderPanel formulaHeaderCanopus, FormulaResultBean instanceBean, @NotNull SiriusGui gui) {
        if (instanceBean != null) {
            checkAndInitSoftwareTour(formulaHeaderCanopus, instanceBean.getCanopusPrediction(), SoftwareTourInfoStore.CanopusTabTourName, SoftwareTourInfoStore.CanopusTabTourKey, gui);
        }
    }

    private void checkAndInitFingerprintSoftwareTour(FormulaListHeaderPanel formulaHeaderCanopus, FormulaResultBean instanceBean, @NotNull SiriusGui gui) {
        if (instanceBean != null) {
            checkAndInitSoftwareTour(formulaHeaderCanopus, instanceBean.getPredictedFingerprint(), SoftwareTourInfoStore.FingerprintTabTourName, SoftwareTourInfoStore.FingerprintTabTourKey, gui);
        }
    }

    private void checkAndInitSoftwareTour(FormulaListHeaderPanel formulaHeader, Optional data, String tourName, String tourKey, @NotNull SiriusGui gui) {
        if (data.isPresent() && Objects.nonNull(data.get())) {
            Jobs.runEDTLater(() -> SoftwareTourUtils.checkAndInitTour(formulaHeader, tourName, tourKey, gui.getProperties()));
        }
    }

    private void showSpectralMatchingTab(boolean show) {
        int idx = indexOfTab(spectralMatchingTabName);
        if (show && idx < 0) {
            if (spectralMatchList == null) {
                spectralMatchList = new SpectralMatchList(compoundList);
            }
            SpectralMatchingPanel spectralMatchingTab = new SpectralMatchingPanel(spectralMatchList);
            // add to second last position
            int homologueSeriesTabIndex = indexOfTab(massDefectTabName);
            insertTab(spectralMatchingTabName, null, spectralMatchingTab, spectralMatchingTab.getDescription(), homologueSeriesTabIndex < 0 ? getTabCount() : homologueSeriesTabIndex);
            return;
        }

        if (!show && idx >= 0) {
            removeTabAt(idx);
        }
    }

    private void showHomologueSeriesTab(boolean show) {
        int idx = indexOfTab(massDefectTabName);
        if (show && idx < 0) {
            KendrickMassDefectPanel massDefectTab = new KendrickMassDefectPanel(compoundList, gui);
            // add to last position
            int spectralMatchingTabIndex = indexOfTab(spectralMatchingTabName);
            insertTab(massDefectTabName, null, massDefectTab, massDefectTab.getDescription(), spectralMatchingTabIndex < 0 ? getTabCount() : spectralMatchingTabIndex + 1);
            return;
        }

        if (!show && idx >= 0) {
            removeTabAt(idx);
        }
    }


    private void addLcmsTab() {
        if (lcmsTab == null)
            lcmsTab = new LCMSViewerPanel(gui, siriusResultElements);

        insertTab("LC-MS", null, lcmsTab, lcmsTab.getDescription(), 0);
    }

    public void showLcmsTab(boolean show) {
        int idx = indexOfTab("LC-MS");
        SiriusActions.ORDER_BY_QUALITY.getInstance(gui, true).setEnabled(show);

        if (show && idx < 0) {
            addLcmsTab();
            setSelectedIndex(0);
            return;
        }
        if (!show && idx >= 0) {
            removeTabAt(idx);
        }
    }
}
