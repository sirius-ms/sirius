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

package de.unijena.bioinf.ms.gui.mainframe.instance_panel;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.confidence_score.ConfidenceMode;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.ConfidenceScoreApproximate;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.projectspace.InstanceBean;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.stream.Stream;

public class CompoundCellRenderer extends JLabel implements ListCellRenderer<InstanceBean> {

    private InstanceBean ec;

    private Color backColor, foreColor;

    private Font valueFont, compoundFont, propertyFont, statusFont;

    private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
    private Color activatedForeground;

    private DecimalFormat numberFormat;

    public CompoundCellRenderer() {
        this.setPreferredSize(new Dimension(210, 86));
        initColorsAndFonts();
        this.numberFormat = new DecimalFormat("#0.00");
    }

    public void initColorsAndFonts() {
        compoundFont = Fonts.FONT_BOLD.deriveFont(13f);
        propertyFont = Fonts.FONT_BOLD.deriveFont(12f);
        statusFont = Fonts.FONT_BOLD.deriveFont(24f);
        valueFont = Fonts.FONT.deriveFont(12f);

        selectedBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
        selectedForeground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
        evenBackground = Colors.LIST_EVEN_BACKGROUND;
        unevenBackground = Colors.LIST_UNEVEN_BACKGROUND;
        activatedForeground = UIManager.getColor("List.foreground");
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends InstanceBean> list, InstanceBean value,
            int index, boolean isSelected, boolean cellHasFocus) {
        this.ec = value;
        if (isSelected) {
            this.backColor = this.selectedBackground;
            this.foreColor = this.selectedForeground;
        } else {
            if (index % 2 == 0) this.backColor = this.evenBackground;
            else this.backColor = this.unevenBackground;
            this.foreColor = this.activatedForeground;
        }

        this.setToolTipText(ec.getGUIName());

        return this;
    }


    @Override
    public void paint(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(this.backColor);

        g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());

        FontMetrics compoundFm = g2.getFontMetrics(this.compoundFont);
        FontMetrics propertyFm = g2.getFontMetrics(this.propertyFont);
        FontMetrics valueFm = g2.getFontMetrics(this.valueFont);

        g2.setColor(this.foreColor);

        final int maxWidth = getWidth() - 2;
        int compoundLength = compoundFm.stringWidth(ec.getGUIName()) + 4;

        boolean trigger = compoundLength + 2 > maxWidth;

        Paint p = g2.getPaint();

        if (trigger) {
            g2.setPaint(new GradientPaint(maxWidth - 18, 0, foreColor, maxWidth - 1, 0, backColor));
        }

        g2.drawLine(2, 17, Math.min(maxWidth - 3, 2 + compoundLength), 17);

        g2.setFont(compoundFont);
        g2.drawString(ec.getGUIName(), 4, 13);

        if (trigger) g2.setPaint(p);

        String ionizationProp = "Ionization";
        String focMassProp = "Precursor";
        String rtProp = "RT";
        String confProp = ec.getProjectManager().getConfidenceDisplayMode() == ConfidenceMode.APPROXIMATE ?
                ConfidenceScore.NA(ConfidenceScoreApproximate.class).shortName() :
                ConfidenceScore.NA(ConfidenceScore.class).shortName();

        g2.setFont(propertyFont);
        g2.drawString(ionizationProp, 4, 32);
        g2.drawString(focMassProp, 4, 48);
        g2.drawString(rtProp, 4, 64);
        g2.drawString(confProp, 4, 80);

        int xPos = Stream.of(
                propertyFm.stringWidth(ionizationProp),
                propertyFm.stringWidth(focMassProp),
                propertyFm.stringWidth(rtProp),
                propertyFm.stringWidth(confProp)
        ).max(Integer::compareTo).get() + 15;

        String ionValue = ec.getIonType().toString();
        double focD = ec.getIonMass();
        String focMass = focD > 0 ? numberFormat.format(focD) + " Da" : "unknown";
        String rtValue = ec.getRT().map(RetentionTime::getRetentionTimeInSeconds).map(s -> s / 60)
                .map(numberFormat::format).map(i -> i + " min").orElse("N/A");

        g2.setFont(valueFont);
        g2.drawString(ionValue, xPos, 32);
        g2.drawString(focMass, xPos, 48);
        g2.drawString(rtValue, xPos, 64);

        ec.getConfidenceScoreDefault().ifPresent(confScore -> {
            g2.setFont(propertyFont);
            String conf = confScore < 0 || Double.isNaN(confScore) ? ConfidenceScore.NA() : BigDecimal.valueOf(confScore).setScale(3, RoundingMode.HALF_UP).toString();
            g2.drawString(conf, xPos, 80);
        });


        g2.setFont(statusFont);
        GuiUtils.drawListStatusElement(ec.isComputing(), g2, this);
    }

}