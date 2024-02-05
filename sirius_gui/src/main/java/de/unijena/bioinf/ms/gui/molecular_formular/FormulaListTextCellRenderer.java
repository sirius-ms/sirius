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

package de.unijena.bioinf.ms.gui.molecular_formular;

import de.unijena.bioinf.fingerid.ConfidenceScore;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.projectspace.FormulaResultBean;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.function.Function;

public class FormulaListTextCellRenderer extends JLabel implements ListCellRenderer<FormulaResultBean> {
    public static final DummySiriusResult PROTOTYPE = new DummySiriusResult();

    private Color backColor, foreColor;

    private Font valueFont, mfFont, propertyFont, rankFont, statusFont;

    private Color selectedBackground, evenBackground, unevenBackground, selectedForeground;
    private Color activatedForeground, deactivatedForeground, disableBackground;

    private FormulaResultBean sre;

//    private DecimalFormat numberFormat;

    private final Function<FormulaResultBean, Boolean> bestHitFunc;


    public FormulaListTextCellRenderer(Function<FormulaResultBean,Boolean> bestHitFuction) {
        this.setPreferredSize(new Dimension(250, 45));
        initColorsAndFonts();
        sre = null;
        this.bestHitFunc = bestHitFuction;
    }

    public void initColorsAndFonts() {
        //todo replace font with them from utils class
        mfFont = Fonts.FONT_BOLD.deriveFont(13f);
        propertyFont = Fonts.FONT_BOLD.deriveFont(12f);
        statusFont = Fonts.FONT_BOLD.deriveFont(24f);
        rankFont = Fonts.FONT_BOLD.deriveFont(16f);
        valueFont = Fonts.FONT.deriveFont(12f);

        selectedBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
        selectedForeground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
        evenBackground = Colors.LIST_EVEN_BACKGROUND;
        disableBackground = UIManager.getColor("ComboBox.background");
        unevenBackground = Colors.LIST_UNEVEN_BACKGROUND;
        activatedForeground = UIManager.getColor("List.foreground");
        deactivatedForeground = Color.GRAY;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends FormulaResultBean> list, FormulaResultBean value,
            int index, boolean isSelected, boolean cellHasFocus) {
        this.sre = value;

        if (isSelected) {
            if (bestHitFunc.apply(value)) {
                this.backColor = Colors.LIST_SELECTED_GREEN;
            } else {
                this.backColor = this.selectedBackground;
            }
            this.foreColor = this.selectedForeground;
        } else {
            if (bestHitFunc.apply(value)) {
                this.backColor = Colors.LIST_LIGHT_GREEN;
            } else {
                if (index % 2 == 0) this.backColor = this.evenBackground;
                else this.backColor = this.unevenBackground;
            }
            this.foreColor = this.activatedForeground;
        }

        return this;
    }


    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(this.backColor);

        g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());

        FontMetrics mfFm = g2.getFontMetrics(this.mfFont);
        FontMetrics propertyFm = g2.getFontMetrics(this.propertyFont);
        FontMetrics rankFm = g2.getFontMetrics(this.rankFont);

        final int gap = 5;

        g2.setColor(this.foreColor);

        final String formulaText = sre.getFormulaAndIonText();
        final int charge = sre.getCharge();


        int mfLength = mfFm.stringWidth(formulaText) + 4;

        g2.drawLine(13, 17, 15 + mfLength, 17);

        g2.setFont(mfFont);
        g2.drawString(formulaText, 15, 13);
        g2.drawString(charge > 0 ? "+" : "-", 15 + mfLength - 4, 13 - 4);

        {
            RenderScore renderScore = sre.getZodiacScore().map(s -> new RenderScore(s, "Zodiac"))
                    .orElse(new RenderScore(sre.getSiriusScore().orElse(Double.NaN), "SIRIUS"));

            int scoreLength = propertyFm.stringWidth(renderScore.name);
            g2.setFont(propertyFont);
            g2.drawString(renderScore.name, 10, 35);
            g2.setFont(valueFont);
            g2.drawString(String.format("%.3f", renderScore.score) + "%", 10 + gap + scoreLength, 35);
        }

        sre.getTopCSIScore().ifPresent(score -> {
            String cosmicLab = "CSI";
            String cosmicVal = (Double.isNaN(score)) ? ConfidenceScore.NA() : BigDecimal.valueOf(score).setScale(3, RoundingMode.HALF_UP).toString();
            final int labStart = (int) getSize().getWidth() - (10 + gap + propertyFm.stringWidth(cosmicLab) + g2.getFontMetrics(valueFont).stringWidth(cosmicVal));
            g2.setFont(propertyFont);
            g2.drawString(cosmicLab, labStart, 35);
            g2.setFont(valueFont);
            g2.drawString(cosmicVal, labStart + gap + propertyFm.stringWidth(cosmicLab), 35);
        });
    }

    private static class DummySiriusResult extends FormulaResultBean {

        @Override
        public String getFormulaAndIonText() {
            return "CH6H12O6CHLOR2";
        }

        @Override
        public int getCharge() {
            return 1;
        }


        private  Optional<Double> getScoreValue() {
            return Optional.of(9000d);
        }

        @Override
        public Optional<Double> getSiriusScore() {
            return getScoreValue();
        }

        @Override
        public Optional<Double> getZodiacScore() {
            return getScoreValue();
        }

        @Override
        public Optional<Double> getTopCSIScore() {
            return getScoreValue();
        }
    }

    public static class RenderScore{
        public final double score;
        public final String name;

        public RenderScore(double score, String name) {
            this.score = score;
            this.name = name;
        }

        public static RenderScore of(double score, String name){
            return new RenderScore(score, name);
        }
    }
}
