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

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeatureQuality;
import de.unijena.bioinf.ms.nightsky.sdk.model.Category;
import de.unijena.bioinf.ms.nightsky.sdk.model.Item;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LCMSCompoundSummaryPanel extends JPanel {

    private AlignedFeatureQuality report;

    public LCMSCompoundSummaryPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(400,1024));
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
        peakHeader.setIcon(getLargeColoredIcon(DataQuality.valueOf(qualityCheckResult.getOverallQuality().getValue())));
        JPanel peakPanel = new JPanel();
        peakPanel.setLayout(new BoxLayout(peakPanel,BoxLayout.Y_AXIS));
        peakPanel.setBorder(peakHeader);
        add(peakPanel);

        for (Item check : checkList) {
            JLabel c = new JLabel("<html>"+check.getDescription()+"</html>", getSmallColoredIcon(DataQuality.valueOf(check.getQuality().getValue())), JLabel.LEADING);
            c.setBorder(BorderFactory.createEmptyBorder(6,0,2,0));
            peakPanel.add(c);
        }
    }

    private Icon getSmallColoredIcon(DataQuality quality) {
        switch (quality) {
            case GOOD:
                return Icons.TRAFFIC_LIGHT_SMALL[2];
            case DECENT:
                return Icons.TRAFFIC_LIGHT_SMALL[1];
            case BAD:
                return Icons.TRAFFIC_LIGHT_SMALL[0];
            case LOWEST, NOT_APPLICABLE:
                return Icons.TRAFFIC_LIGHT_SMALL_GRAY;
        }
        return Icons.TRAFFIC_LIGHT_SMALL_GRAY;
    }
    private Icon getLargeColoredIcon(DataQuality quality) {
        switch (quality) {
            case GOOD:
                return Icons.TRAFFIC_LIGHT_MEDIUM[2];
            case DECENT:
                return Icons.TRAFFIC_LIGHT_MEDIUM[1];
            case BAD:
                return Icons.TRAFFIC_LIGHT_MEDIUM[0];
            case LOWEST, NOT_APPLICABLE:
                return Icons.TRAFFIC_LIGHT_MEDIUM_GRAY;
        }
        return Icons.TRAFFIC_LIGHT_MEDIUM_GRAY;
    }
}
