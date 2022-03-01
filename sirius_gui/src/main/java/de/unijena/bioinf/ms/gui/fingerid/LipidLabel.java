/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.table.ActiveElementChangedListener;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LipidLabel extends JLabel implements ActiveElementChangedListener<FingerprintCandidateBean, Set<FormulaResultBean>> {


    private volatile URI uri;

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
                open(uri);
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
    public void resultsChanged(Set<FormulaResultBean> experiment, FingerprintCandidateBean sre, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        setText(null);
        setVisible(false);
        uri = null;
        if (experiment != null && !experiment.isEmpty()) {
            FormulaResultBean current = experiment.iterator().next();
            current.getFragTree().flatMap(t -> t.getAnnotation(LipidSpecies.class)).ifPresent(lipidSpecies -> {
                setText("<html>" +
                        "<b>" + lipidSpecies + "</b>" +
                        " - " +
                        "<b>El Gordo</b> classified this compound as <b>lipid</b>" +
                        " </html>");
                String link = String.format(Locale.US, DataSource.LIPID.URI, URLEncoder.encode(lipidSpecies.toString(), StandardCharsets.UTF_8));
                try {
                    uri = new URI(link);
                } catch (URISyntaxException e) {
                    LoggerFactory.getLogger(getClass()).error("Could not create Link URI from '" + link + "'.", e);
                }
                setVisible(true);
            });

            repaint();
        }
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
