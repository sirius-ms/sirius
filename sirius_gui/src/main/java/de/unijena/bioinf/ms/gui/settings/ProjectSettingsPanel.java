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

package de.unijena.bioinf.ms.gui.settings;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.utils.TwoColumnPanel;
import io.sirius.ms.sdk.model.ProjectInfo;
import io.sirius.ms.sdk.model.ProjectInfoOptField;

import javax.swing.*;
import java.util.List;
import java.util.Optional;

public class ProjectSettingsPanel extends TwoColumnPanel implements SettingsPanel {

    private final SiriusGui gui;
    private final JTextField sizeField;

    public ProjectSettingsPanel(SiriusGui gui, JDialog parent) {
        super();
        this.gui = gui;

        JTextField pathField = new JTextField(this.gui.getProjectManager().getProjectLocation());
        pathField.setEditable(false);
        addNamed("Location", pathField);

        sizeField = new JTextField("");
        sizeField.setEditable(false);
        addNamed("Size", sizeField);
        loadSize();

        JButton compactButton = new JButton("Compact");
        compactButton.setToolTipText("Compacting the project tries to reduce the project's file size. May take some time for large projects.");
        addNamed("", compactButton);

        compactButton.addActionListener(e -> {
            ProjectInfo projectInfo = gui.getProjectManager().compactWithLoading(parent);
            if (projectInfo != null) {
                loadSize(projectInfo);
            }
        });

        JButton compactFullButton = new JButton("Full Compact");
        compactFullButton.setToolTipText("Compacts the project and rewrites how its records are stored, even where "
                + "that does not look necessary. Reclaims space that plain compacting cannot, for projects written "
                + "by older versions. Reads and writes every record, so it takes considerably longer.");
        addNamed("", compactFullButton);

        compactFullButton.addActionListener(e -> {
            ProjectInfo projectInfo = gui.getProjectManager().compactWithLoading(parent, true);
            if (projectInfo != null) {
                loadSize(projectInfo);
            }
        });


        JButton rebuildSearchIndex = new JButton("Rebuild Search Index");
        rebuildSearchIndex.setToolTipText("Rebuild the search index used for filtering the feature list.");
        addNamed("", rebuildSearchIndex);
        rebuildSearchIndex.addActionListener(evt -> {
            Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Rebuilding search index...", () -> {
                gui.getSiriusClient().projects().buildSearchIndex(gui.getProjectManager().getProjectId(), true);
                gui.getProjectManager().reloadFeatures();
            });
        });

        addVerticalGlue();
    }

    @Override
    public void saveProperties() {}

    @Override
    public String name() {
        return "Project";
    }

    private void loadSize() {
        loadSize(gui.getProjectManager().getProjectInfo(List.of(ProjectInfoOptField.SIZE_INFORMATION)));
    }

    private void loadSize(ProjectInfo projectInfo) {
        long size = Optional.ofNullable(projectInfo.getNumOfBytes()).orElseThrow();
        String readable = FileUtils.sizeToReadableString(size);
        boolean compactionRecommended = Boolean.TRUE.equals(projectInfo.isCompactionRecommended());

        sizeField.setText(compactionRecommended ? readable + "  (compacting is recommended)" : readable);
        // cleared rather than left standing, so the hint disappears once the project has been compacted
        sizeField.setToolTipText(compactionRecommended
                ? "This project is stored in a layout an earlier SIRIUS version wrote, which takes markedly "
                + "more space than it needs. Compacting rewrites it and reclaims that space.\n"
                + "It reads and writes the whole project, so expect minutes for a large one, "
                + "and free disk space for a second copy while it runs."
                : null);
    }
}
