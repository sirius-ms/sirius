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

import de.unijena.bioinf.elgordo.LipidSpecies;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
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
import java.util.List;
import java.util.Set;

public class LipidLabel extends JLabel implements ActiveElementChangedListener<FingerprintCandidateBean, Set<FormulaResultBean>> {


    private volatile LipidSpecies lipidSpecies;
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
                        //nice but too much info?
                        /*try {
                            List<String> lmIds = ProxyManager.applyClient(client -> {
                                URI uri = URI.create(String.format(Locale.US, "https://www.lipidmaps.org/rest/compound/abbrev/%s/lm_id", URLEncoder.encode(lipidSpecies.toString(), StandardCharsets.UTF_8)));
                                System.out.println(uri);
                                HttpGet get = new HttpGet(uri);
                                return client.execute(get, r -> {
                                    ObjectNode array = new ObjectMapper().readValue(r.getEntity().getContent(), ObjectNode.class);
                                    List<String> ids = new ArrayList<>();
                                    array.forEach(node -> {
                                        if (node.has("lm_id"))
                                            ids.add(node.get("lm_id").asText(null));
                                    });
                                    return ids.stream().filter(Objects::nonNull).collect(Collectors.toList());
                                });
                            });
                            if (lmIds != null && !lmIds.isEmpty())
                                lmIds.forEach(lmId -> open(URI.create(String.format(Locale.US, DataSource.LIPID.URI, URLEncoder.encode(lmId, StandardCharsets.UTF_8)))));
                        } catch (Exception ex) {
                            LoggerFactory.getLogger(getClass()).error("Could not fetch lipid maps URL.", ex);
                        }*/
                        open(lipidSpecies.getLipidClass().lipidMapsClassLink());
                        open(lipidSpecies.lipidMapsFuzzySearchLink());
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
    public void resultsChanged(Set<FormulaResultBean> experiment, FingerprintCandidateBean sre, List<FingerprintCandidateBean> resultElements, ListSelectionModel selections) {
        setText(null);
        setVisible(false);
        this.lipidSpecies = null;
        if (experiment != null && !experiment.isEmpty()) {
            FormulaResultBean current = experiment.iterator().next();
            current.getFragTree().flatMap(t -> t.getAnnotation(LipidSpecies.class)).ifPresent(lipidSpecies -> {
                this.lipidSpecies = lipidSpecies;
                setText("<html>" +
                        "<b>" + lipidSpecies + "</b>" +
                        " - " +
                        elgordoExplanation(lipidSpecies) +
                        " </html>");
                setVisible(true);
            });
            repaint();
        }
    }

    private String elgordoExplanation(LipidSpecies species) {
        StringBuilder buf = new StringBuilder();
        buf.append("<b>El Gordo</b> classified this compound as <b>");
        buf.append(species.getLipidClass().longName());
        buf.append("</b>.");
        buf.append("<br>Note that neither the exact chain locations nor the stereochemistry and the double bond locations can be determined from the MS/MS.");
        if (species.chainsUnknown()) {
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
