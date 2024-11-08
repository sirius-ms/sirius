/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker, Chair of Bioinformatics, Friedrich-Schiller University.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.lcms_viewer;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.gui.configs.Icons;
import io.sirius.ms.sdk.model.AlignedFeatureQuality;
import io.sirius.ms.sdk.model.Category;
import io.sirius.ms.sdk.model.Item;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LCMSCompoundSummaryPanel extends JPanel {

    private AlignedFeatureQuality report;

    public LCMSCompoundSummaryPanel() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        //setPreferredSize(new Dimension(400,320));
    }

    public void reset() {
        setReport(null);
    }

    public void setReport(AlignedFeatureQuality report) {
        if (this.report!=report) {
            this.report = report;
            updateContent();
        }
    }

    private void updateContent() {
        removeAll();
        if (this.report!=null) {
            for (Category category : report.getCategories().values()) {
                addSection(category);
            }
        }
        revalidate();
        repaint();
    }


    private void addSection(Category qualityCheckResult) {
        List<Item> checkList = qualityCheckResult.getItems();
        DataQuality quality = DataQuality.valueOf(qualityCheckResult.getOverallQuality().getValue());
        if (quality==null || quality==DataQuality.NOT_APPLICABLE) return;
        final TitledIconBorder peakHeader = new TitledIconBorder(qualityCheckResult.getCategoryName());
        peakHeader.setIcon(getColoredIcon(DataQuality.valueOf(qualityCheckResult.getOverallQuality().getValue()), 32));
        JPanel peakPanel = new JPanel();
        peakPanel.setLayout(new BoxLayout(peakPanel,BoxLayout.Y_AXIS));
        peakPanel.setBorder(peakHeader);
        add(peakPanel);

        for (Item check : checkList) {
            JLabel c = new JLabel("<html>" + check.getDescription() + "</html>", getColoredIcon(DataQuality.valueOf(check.getQuality().getValue()), 16), JLabel.LEADING);
            c.setBorder(BorderFactory.createEmptyBorder(6, 0, 2, 0));
            peakPanel.add(c);
        }
    }

    private Icon getColoredIcon(DataQuality quality, int size) {
        return getColoredIcon(quality).derive(size, size);
    }

    private FlatSVGIcon getColoredIcon(DataQuality quality) {
        return switch (quality) {
            case GOOD -> Icons.TRAFFIC_LIGHT[2];
            case DECENT -> Icons.TRAFFIC_LIGHT[1];
            case BAD -> Icons.TRAFFIC_LIGHT[0];
            default -> Icons.TRAFFIC_LIGHT_LOWEST;
        };
    }

    @Override
    public Dimension getPreferredSize() {
        // Get the preferred height based on current content
        int preferredHeight = super.getPreferredSize().height;
        // Return a fixed width of 400, but allow height to grow dynamically
        return new Dimension(400, preferredHeight);
    }
}
