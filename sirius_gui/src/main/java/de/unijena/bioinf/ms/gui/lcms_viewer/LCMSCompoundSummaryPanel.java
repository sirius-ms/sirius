/*
 * This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 * Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker, Chair of Bioinformatics, Friedrich-Schilller University.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.lcms_viewer;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.lcms.CoelutingTraceSet;
import de.unijena.bioinf.ms.gui.configs.Icons;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LCMSCompoundSummaryPanel extends JPanel {

    CoelutingTraceSet traceSet;
    Ms2Experiment experiment;

    public LCMSCompoundSummaryPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(512,1024));
    }

    public void clear() {
        this.traceSet = null;
        updateContent();
    }

    public void set(CoelutingTraceSet traceSet, Ms2Experiment experiment) {
        this.traceSet = traceSet;
        this.experiment = experiment;
        updateContent();
    }

    public void setTraceSet(CoelutingTraceSet traceSet) {
        if (this.traceSet!=traceSet) {
            this.traceSet = traceSet;
            updateContent();
        }
    }

    private void updateContent() {
        removeAll();
        if (this.traceSet!=null) {

            final LCMSCompoundSummary summary = new LCMSCompoundSummary(traceSet, traceSet.getIonTrace(), experiment);

            addSection("Peak Quality", summary.peakCheck, summary.peakQuality);

            addSection("Isotope Quality", summary.isotopeCheck, summary.isotopeQuality);
            addSection("MS/MS Quality", summary.ms2Check, summary.ms2Quality);

        }
        revalidate();
        repaint();
    }


    private void addSection(String title, List<LCMSCompoundSummary.Check> checkList, LCMSCompoundSummary.Quality quality) {
        if (quality==null) return;
        final TitledIconBorder peakHeader = new TitledIconBorder(title);
        peakHeader.setIcon(Icons.TRAFFIC_LIGHT_MEDIUM[quality.ordinal()]);
        JPanel peakPanel = new JPanel();
        peakPanel.setLayout(new BoxLayout(peakPanel,BoxLayout.Y_AXIS));
        peakPanel.setBorder(peakHeader);
        add(peakPanel);

        for (LCMSCompoundSummary.Check check : checkList) {
            JLabel c = new JLabel("<html>"+check.getDescription()+"</html>", Icons.TRAFFIC_LIGHT_SMALL[check.getQuality().ordinal()], JLabel.LEADING);
            c.setBorder(BorderFactory.createEmptyBorder(6,0,2,0));
            peakPanel.add(c);
        }
    }

}
