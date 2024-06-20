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

import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.nightsky.sdk.model.LipidAnnotation;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class LipidLabel extends JLabel implements ActiveElementChangedListener<FingerprintCandidateBean, InstanceBean> {



    public static URI makeLipidMapsFuzzySearchLink(String abbrev) {
        String encAbb = URLEncoder.encode(abbrev, StandardCharsets.UTF_8);
        return URI.create(String.format(Locale.US, "https://www.lipidmaps.org/data/structure/LMSDFuzzySearch.php?Name=%s&s=%s&SortResultsBy=Name", encAbb, encAbb));
    }

    private volatile LipidAnnotation lipidSpecies;

    public LipidLabel(StructureList source) {
        setBorder(BorderFactory.createEmptyBorder(3, GuiUtils.SMALL_GAP, 3, GuiUtils.SMALL_GAP));
        setForeground(Color.WHITE);
        setBackground(Colors.DB_ELGORDO);
        setOpaque(true);
        setToolTipText(GuiUtils.formatToolTip(
                "El Gordo classified this compound as lipid and determined the lipid species de novo. " +
                        "Structure candidates that belong to this lipid species are tagged as such. " +
                        "Candidates can be filtered to these candidates by selecting the 'Lipid' database flag."));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (lipidSpecies != null) {
                    Jobs.runInBackground(() -> {
                        if (lipidSpecies.getLipidMapsId() != null)
                            open(URI.create("https://lipidmaps.org/databases/lmsd/" + lipidSpecies.getLipidMapsId()));
                        if (lipidSpecies.getLipidClassName() != null) //could also extract abbreviation instead
                            open(makeLipidMapsFuzzySearchLink(lipidSpecies.getLipidClassName()));
                    });
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        source.addActiveResultChangedListener(this);
    }

    @Override
    public void resultsChanged(InstanceBean elementsParent, FingerprintCandidateBean selectedElement, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        setText(null);
        setVisible(false);
        this.lipidSpecies = null;
        if (elementsParent != null) {
            this.lipidSpecies = elementsParent.getFormulaAnnotationAsBean().map(b -> b.getLipidAnnotation().orElse(null)).orElse(null);
            if (this.lipidSpecies != null && lipidSpecies.getLipidSpecies() != null){
                setText("<html>" +
                        "<b>" + lipidSpecies.getLipidSpecies() + "</b>" +
                        " - " +
                        elgordoExplanation(lipidSpecies) +
                        " </html>");
                setVisible(true);
            }

            repaint();
        }
    }

    private String elgordoExplanation(@NotNull LipidAnnotation species) {
        StringBuilder buf = new StringBuilder();
        buf.append("<b>El Gordo</b> classified this compound as <b>");
        buf.append(species.getLipidClassName());
        buf.append("</b>.");
        buf.append("<br>Note that neither the exact chain locations nor the stereochemistry and the double bond locations can be determined from the MS/MS.");
        if (species.isChainsUnknown()) {
            buf.append("The formula composition of the chains could not be determined from the MS/MS, too.");
        }
        return buf.toString();
    }

    private void open(URI uri) {
        if (uri != null) {
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(uri);
                } catch (IOException e) {
                    LoggerFactory.getLogger(getClass()).error("Error when opening link '" + uri + ".", e);
                }
            } else {
                LoggerFactory.getLogger(getClass()).error("No Browser found.");
            }
        } else {
            LoggerFactory.getLogger(getClass()).warn("No link to be opened.");
        }
    }
}
