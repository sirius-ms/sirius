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

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.fingerid.ConfidenceScoreApproximate;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.properties.ConfidenceDisplayMode;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import io.sirius.ms.sdk.model.DataQuality;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompoundCellRenderer extends JLabel implements ListCellRenderer<InstanceBean> {
    public static final String TOOLTIP_TEMPLATE =
            "<html>" +
                    "<h3><span style=\"text-decoration: underline;white-space:nowrap\">%s</span></h3>" +
                    "<table>" +
                    "<tbody>" +
                    "<tr>" +
                    "<td><strong>Feature Quality</strong></td>" +
                    "<td>%s</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Detected Adduct</strong></td>" +
                    "<td>%s</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Precursor Mass</strong></td>" +
                    "<td>%s</td>" + // Placeholder for precursor mass
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Retention Time</strong></td>" +
                    "<td>%s</td>" + // Placeholder for retention time
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>%s</strong></td>" +
                    "<td>%s</td>" + // Placeholder for confidence score
                    "</tr>" +
                    "</tbody>" +
                    "</table>" +
                    "</html>";

    private final SiriusGui gui;
    private InstanceBean ec;

    private Color backColor, foreColor;

    private Font valueFont, compoundFont, propertyFont, statusFont;

    private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
    private Color activatedForeground;

    private final DecimalFormat numberFormat;
    private final DecimalFormat numberFormatMass;
    private final DecimalFormat numberFormatMassLong;

    public CompoundCellRenderer(@NotNull SiriusGui gui) {
        this.gui = gui;
        this.setPreferredSize(new Dimension(210, 86));
        initColorsAndFonts();
        this.numberFormat = new DecimalFormat("#0.00");
        this.numberFormatMass = new DecimalFormat("#0.000");
        this.numberFormatMassLong = new DecimalFormat("#0.00000");

    }

    public void initColorsAndFonts() {
        compoundFont = Fonts.FONT_MEDIUM.deriveFont(13f);
        propertyFont = Fonts.FONT_MEDIUM.deriveFont(12f);
        statusFont = Fonts.FONT_DEJAVU_SANS.deriveFont(24f);
        valueFont = Fonts.FONT.deriveFont(12f);

        selectedBackground = Colors.CellsAndRows.LargerCells.SELECTED_CELL;
        selectedForeground = Colors.CellsAndRows.LargerCells.SELECTED_CELL_TEXT;
        evenBackground = Colors.CellsAndRows.LargerCells.ALTERNATING_CELL_1;
        unevenBackground = Colors.CellsAndRows.LargerCells.ALTERNATING_CELL_2;
        activatedForeground = Colors.CellsAndRows.LargerCells.CELL_TEXT;
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

        // Use String.format to replace placeholders with actual values
        this.setToolTipText(String.format(TOOLTIP_TEMPLATE,
                ec.getGUIName(),
                ec.getQuality() != null ? ec.getQuality().name() : "",
                ec.getDetectedAdductsOrUnknown().stream().sorted().map(PrecursorIonType::toString).collect(Collectors.joining(" or ")),
                ec.getIonMass() > 0 ? numberFormatMassLong.format(ec.getIonMass()) + " m/z" : "",
                ec.getRT().map(RetentionTime::getRetentionTimeInSeconds).map(s -> s / 60).map(numberFormat::format).map(i -> i + " min").orElse(""),
                gui.getProperties().isConfidenceViewMode(ConfidenceDisplayMode.APPROXIMATE) ? "Confidence Approximate" : "Confidence Exact",
                ec.getConfidenceScore(gui.getProperties().getConfidenceDisplayMode()).map(confScore -> confScore < 0 || Double.isNaN(confScore) ? ConfidenceScore.NA() : BigDecimal.valueOf(confScore).setScale(3, RoundingMode.HALF_UP).toString()).orElse(""))
        );
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
//        FontMetrics valueFm = g2.getFontMetrics(this.valueFont);

        g2.setColor(this.foreColor);

        final int maxWidth = getWidth() - 2;
        Paint defaultPaint = g2.getPaint();
        Paint gradientPaint = new GradientPaint(maxWidth - 18, 0, foreColor, maxWidth - 1, 0, backColor);

        g2.setFont(compoundFont);
        DataQuality q = ec.getSourceFeature().getQuality();
        int compoundLength;
        if (q != null) {
            compoundLength = compoundFm.stringWidth(ec.getGUIName()) + 13;
            if (compoundLength + 2 > maxWidth)
                g2.setPaint(gradientPaint);
            getQualityIcon(q).paintIcon(this, g2, 2, 4);
            g2.drawString(ec.getGUIName(), 13, 13);
        } else {
            compoundLength = compoundFm.stringWidth(ec.getGUIName()) + 4;
            if (compoundLength + 2 > maxWidth)
                g2.setPaint(gradientPaint);
            g2.drawString(ec.getGUIName(), 4, 13);
        }
        g2.drawLine(2, 17, Math.min(maxWidth - 3, 2 + compoundLength), 17);

        g2.setPaint(defaultPaint);

        String ionizationProp = "Det. Adduct";
        String focMassProp = "Precursor";
        String rtProp = "RT";
        String confProp = gui.getProperties().isConfidenceViewMode(ConfidenceDisplayMode.APPROXIMATE)
                ? ConfidenceScore.NA(ConfidenceScoreApproximate.class).shortName()
                : ConfidenceScore.NA(ConfidenceScore.class).shortName();

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



        String ionValue = ec.getDetectedAdductsOrUnknown().stream().sorted().map(PrecursorIonType::toString).collect(Collectors.joining(" or "));
        double focD = ec.getIonMass();
        String focMass = focD > 0 ? numberFormatMass.format(focD) + " m/z" : "";
        String rtValue = ec.getRT().map(RetentionTime::getRetentionTimeInSeconds).map(s -> s / 60)
                .map(numberFormat::format).map(i -> i + " min").orElse("");

        FontMetrics valueFm = g2.getFontMetrics(this.valueFont);
        g2.setFont(valueFont);

        int maxFontWidth = Stream.of(ionizationProp, focMassProp, rtProp, confProp).mapToInt(valueFm::stringWidth)
                .map(w -> w + xPos).max().getAsInt();

        if (maxFontWidth + 2 > maxWidth)
            g2.setPaint(gradientPaint);

        g2.drawString(ionValue, xPos, 32);
        g2.drawString(focMass, xPos, 48);
        g2.drawString(rtValue, xPos, 64);

        ec.getConfidenceScore(gui.getProperties().getConfidenceDisplayMode()).ifPresent(confScore -> {
            g2.setFont(propertyFont);
            String conf = confScore < 0 || Double.isNaN(confScore) ? ConfidenceScore.NA() : BigDecimal.valueOf(confScore).setScale(3, RoundingMode.HALF_UP).toString();
            g2.drawString(conf, xPos, 80);
        });

        g2.setPaint(defaultPaint);

        g2.setFont(statusFont);
        GuiUtils.drawListStatusElement(ec.isComputing(), g2, this);
    }

    private static Icon getQualityIcon(@NotNull DataQuality quality) {
        return switch (quality) {
            case BAD -> Icons.TRAFFIC_LIGHT_BOARDER[0].derive(9,9);
            case DECENT -> Icons.TRAFFIC_LIGHT_BOARDER[1].derive(9,9);
            case GOOD -> Icons.TRAFFIC_LIGHT_BOARDER[2].derive(9,9);
            default -> Icons.TRAFFIC_LIGHT_LOWEST_BOARDER.derive(9,9);
        };
    }

}