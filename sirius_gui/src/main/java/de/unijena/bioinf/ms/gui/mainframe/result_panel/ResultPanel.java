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

import ca.odell.glazedlists.event.ListEventListener;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassBean;
import de.unijena.bioinf.ms.gui.canopus.compound_classes.CompoundClassList;
import de.unijena.bioinf.ms.gui.fingerid.StructureList;
import de.unijena.bioinf.ms.gui.fingerid.fingerprints.FingerprintList;
import de.unijena.bioinf.ms.gui.lcms_viewer.LCMSViewerPanel;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.mainframe.result_panel.tabs.*;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaList;
import de.unijena.bioinf.ms.gui.molecular_formular.FormulaListHeaderPanel;
import de.unijena.bioinf.ms.gui.spectral_matching.SpectralMatchList;
import de.unijena.bioinf.projectspace.InstanceBean;
import io.sirius.ms.sdk.model.CanopusPrediction;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;
import io.sirius.ms.sdk.model.ProjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

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
    private SpectralMatchingPanel spectralMatchingTab;

    private StructureList databaseStructureList;
    private StructureList combinedStructureListSubstructureView;
    private StructureList combinedStructureListDeNovoView;
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
                        c.projects().getProjectSpace(pid, List.of(ProjectInfoOptField.NONE))).getType();
                showLcmsTab(EnumSet.of(ProjectType.ALIGNED_RUNS, ProjectType.UNALIGNED_RUNS).contains(type));
            }
        });
        //Check for first time if project has already a type.
        this.type = gui.applySiriusClient((c, pid) ->
                c.projects().getProjectSpace(pid, List.of(ProjectInfoOptField.NONE))).getType();
        showLcmsTab(EnumSet.of(ProjectType.ALIGNED_RUNS, ProjectType.UNALIGNED_RUNS).contains(type));


        // formulas tabs
        formulasTab = new FormulaOverviewPanel(siriusResultElements);
        addTab("Formulas", null, formulasTab, formulasTab.getDescription());


        // fingerprint tab
        fingerprintList = null;
        try {
            fingerprintList = new FingerprintList(siriusResultElements, gui);
        } catch (IOException e) {
            logger.error("Error when loading FingerprintList. Fingerprint tab will not be available.", e);
        }
        fingerprintTab = fingerprintList == null ? null : new FingerprintPanel(fingerprintList);
        if (fingerprintList != null)
            addTab("Predicted Fingerprints", null, new FormulaListHeaderPanel(siriusResultElements, fingerprintTab), fingerprintTab.getDescription());


        // canopus tab
        compoundClassList = new CompoundClassList(siriusResultElements,
                sre -> sre.getCanopusPrediction()
                        .stream().map(CanopusPrediction::getClassyFireClasses).filter(Objects::nonNull)
                        .flatMap(List::stream).map(CompoundClassBean::new).toList());
        canopusTab = new CompoundClassPanel(compoundClassList, siriusResultElements);
        addTab("Compound Classes", null, new FormulaListHeaderPanel(siriusResultElements, canopusTab), canopusTab.getDescription());


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


        // global spectra match search list
        gui.getProperties().addPropertyChangeListener("showSpectraMatchPanel", evt ->
                showSpectralMatchingTab((Boolean) evt.getNewValue()));
        showSpectralMatchingTab(gui.getProperties().isShowSpectraMatchPanel());
    }

    private void showSpectralMatchingTab(boolean show) {
        String name = "Library Matches";
        int idx = indexOfTab(name);
        if (show && idx < 0) {
            if (spectralMatchList == null) {
                spectralMatchList = new SpectralMatchList(compoundList);
                spectralMatchingTab = new SpectralMatchingPanel(spectralMatchList);
            }
            // add to last position
            addTab(name, null, spectralMatchingTab, spectralMatchingTab.getDescription());
            return;
        }

        if (!show && idx >= 0) {
            removeTabAt(idx);
        }
    }


    private void addLcmsTab() {
        if (lcmsTab == null)
            lcmsTab = new LCMSViewerPanel(siriusResultElements);

        insertTab("LC-MS", null, lcmsTab, lcmsTab.getDescription(), 0);
    }

    public void showLcmsTab(boolean show) {
        int idx = indexOfTab("LC-MS");
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
