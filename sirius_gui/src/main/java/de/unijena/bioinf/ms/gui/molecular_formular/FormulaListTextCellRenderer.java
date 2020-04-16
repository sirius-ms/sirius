package de.unijena.bioinf.ms.gui.molecular_formular;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.FormulaScore;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.sirius.scores.SiriusScore;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
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

    private final Function<FormulaResultBean, Double> scoreFunc;
    private final Function<FormulaResultBean, Boolean> bestHitFunc;


    public FormulaListTextCellRenderer(Function<FormulaResultBean, Double> scoreFunc, Function<FormulaResultBean,Boolean> bestHitFuction) {
        this.setPreferredSize(new Dimension(250, 45));
        initColorsAndFonts();
        sre = null;
//        this.numberFormat = new DecimalFormat("#0.000000");
        this.scoreFunc = scoreFunc;
        this.bestHitFunc = bestHitFuction;
    }

    public void initColorsAndFonts() {
        //todo replace font with them from utils class
        try {
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans-Bold.ttf");
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            mfFont = tempFont.deriveFont(13f);
            propertyFont = tempFont.deriveFont(12f);
            statusFont = tempFont.deriveFont(24f);
            rankFont = tempFont.deriveFont(16f);
        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }

        try {
            InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans.ttf");
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
            valueFont = tempFont.deriveFont(12f);

        } catch (Exception e) {
            LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
        }

        selectedBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
        selectedForeground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
        evenBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\".background");
        disableBackground = UIManager.getColor("ComboBox.background");
        unevenBackground = new Color(213, 227, 238);
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

        g2.setColor(this.foreColor);

        final String formulaText = sre.getFormulaAndIonText();
        final int charge = sre.getCharge();


        int mfLength = mfFm.stringWidth(formulaText) + 4;
        int rankLength = rankFm.stringWidth(Integer.toString(sre.getRank()));

        g2.drawLine(13 + rankLength, 17, 15 + mfLength + rankLength, 17);

        g2.setFont(mfFont);
        g2.drawString(formulaText, 15 + rankLength, 13);
        g2.drawString(charge > 0 ? "+" : "-", 15 + mfLength + rankLength - 4, 13 - 4);

        g2.setFont(rankFont);
        g2.drawString(Integer.toString(sre.getRank()), 2, 15);

        int scoreLength = propertyFm.stringWidth("Score:");
        g2.setFont(propertyFont);
        g2.drawString("Score:", 10, 35);
        g2.setFont(valueFont);

        g2.drawString(String.format("%.3f", scoreFunc.apply(sre)) + "%", 15 + scoreLength, 35);
    }

    private static class DummySiriusResult extends FormulaResultBean {

        @Override
        public int getRank() {
            return 25;
        }

        @Override
        public String getFormulaAndIonText() {
            return "CH6H12O6CHLOR2";
        }

        @Override
        public int getCharge() {
            return 1;
        }


        @Override
        public <T extends FormulaScore> double getScoreValue(Class<T> scoreType) {
            return 9000;
        }

        @Override
        public <T extends FormulaScore> Optional<T> getScore(Class<T> scoreType) {
            return Optional.of(FormulaScore.NA(scoreType));
        }

        public DummySiriusResult() {
            super(1);
        }
    }
}
