/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
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

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.nightsky.sdk.model.ConfidenceMode;
import de.unijena.bioinf.ms.nightsky.sdk.model.FeatureAnnotations;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ExpansiveSearchLabel extends JLabel implements ActiveElementChangedListener<FingerprintCandidateBean, InstanceBean> {

    public ExpansiveSearchLabel(StructureList source) {
        setBorder(BorderFactory.createEmptyBorder(3, GuiUtils.SMALL_GAP, 3, GuiUtils.SMALL_GAP));
        setForeground(Color.WHITE);
        setBackground(Colors.EXPANSIVE_SEARCH_WARNING);
        setOpaque(true);
        setToolTipText(GuiUtils.formatToolTip(
                "Search results were expnanded to the fallback database (PubChem) because the top molecular structure hit has a significantly higher confidence score than the hit in the specified database selection."));

        source.addActiveResultChangedListener(this);
    }

    @Override
    public void resultsChanged(InstanceBean elementsParent, FingerprintCandidateBean selectedElement, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        setText(null);
        setVisible(false);
        if (elementsParent != null) {
            ConfidenceMode mode = Optional.ofNullable(elementsParent.getSourceFeature(Collections.emptyList()).getTopAnnotations()).map(FeatureAnnotations::getExpansiveSearchState).orElse(null);
            if ((mode == ConfidenceMode.EXACT) || (mode == ConfidenceMode.APPROXIMATE)){
                setText("<html>" +
                        "More confident molecular structure hits from <b>PubChem</b> were added by expansive search in <b>" + mode.getValue().toLowerCase() + "</b> confidence mode." +
                        " </html>");
                setVisible(true);
            }
            repaint();
        }
    }
}
