package de.unijena.bioinf.sirius.gui.utils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.MsExperiments;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SpectrumContainer;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class PrecursorSelector extends JPanel {
    public static final String name = "Parent mass (m/z)";

    private final JComboBox<Peak> box;
    private final JButton autoDetectFM = new JButton("Most intensive");
    private final BasicEventList<Peak> peaks = new BasicEventList<>();


    public PrecursorSelector(final ExperimentContainer ec) {
        this(ec.getMs2Experiment());
    }

    public PrecursorSelector(final Ms2Experiment exp) {
        this(exp.getMs1Spectra(), exp.getMs2Spectra(), exp.getIonMass());
    }

    public PrecursorSelector(Iterable<? extends Spectrum<? extends Peak>> spectra, double ionMass) {
        this();
        //add data
        setData(spectra, ionMass);
    }

    public PrecursorSelector(java.util.List<SimpleSpectrum> ms1, java.util.List<Ms2Spectrum<Peak>> ms2, double ionMass) {
        this();
        //add data
        setData(ms1, ms2, ionMass);
    }


    public PrecursorSelector() {
        super(new FlowLayout(FlowLayout.LEFT, 5, 0));

        //build components
        box = createParentPeakSelector();
        autoDetectFM.setToolTipText("Set most intensive peak as parent mass");
        autoDetectFM.addActionListener(e -> box.setSelectedItem(peaks.stream().max(Comparator.comparingDouble(Peak::getIntensity)).orElse(null)));

        add(box);
        add(autoDetectFM);
    }

    //todo proress panel and backround task for setData

    public void setData(java.util.List<SimpleSpectrum> ms1, java.util.List<Ms2Spectrum<Peak>> ms2) {
        setData(ms1, ms2, 0d);
    }

    public void setData(Iterable<? extends Spectrum<? extends Peak>> spectra) {
        setData(spectra, 0d);
    }

    public void setData(Collection<Peak> masses) {
        peaks.clear();
        peaks.addAll(masses);
        if (peaks.isEmpty()) autoDetectFM.setEnabled(false);
        else
            autoDetectFM.getAction().actionPerformed(new ActionEvent(autoDetectFM, 0, autoDetectFM.getActionCommand()));
    }

    public void setData(java.util.List<SpectrumContainer> spectra, double ioMass) {
        java.util.List<SimpleSpectrum> ms1 = new ArrayList<>();
        java.util.List<Ms2Spectrum<Peak>> ms2 = new ArrayList<>();
        for (SpectrumContainer container : spectra) {
            if (container.getSpectrum().getMsLevel() == 1)
                ms1.add((SimpleSpectrum) container.getSpectrum());
            else ms2.add((Ms2Spectrum<Peak>) container.getSpectrum());
        }

        setData(ms1, ms2, ioMass);
    }

    public void setData(Iterable<? extends Spectrum<? extends Peak>> spectra, double ioMass) {
        java.util.List<SimpleSpectrum> ms1 = new ArrayList<>();
        java.util.List<Ms2Spectrum<Peak>> ms2 = new ArrayList<>();
        for (Spectrum<? extends Peak> peakSpectrum : spectra) {
            if (peakSpectrum.getMsLevel() == 1)
                ms1.add((SimpleSpectrum) peakSpectrum);
            else ms2.add((Ms2Spectrum<Peak>) peakSpectrum);
        }

        setData(ms1, ms2, ioMass);
    }

    public void setData(java.util.List<SimpleSpectrum> ms1, java.util.List<? extends Ms2Spectrum<Peak>> ms2, double ioMass) {
        MsExperiments.PrecursorCandidates masses = MsExperiments.findPossiblePrecursorPeaks(ms1, ms2, ioMass);
        peaks.clear();
        peaks.addAll(masses);
        box.setSelectedItem(masses.getDefaultPrecursor() != null ? masses.getDefaultPrecursor() : String.valueOf(ioMass));
        if (peaks.isEmpty()) autoDetectFM.setEnabled(false);
    }


    private JComboBox<Peak> createParentPeakSelector() {
        //create box
        JComboBox<Peak> box = new JComboBox<>(GlazedListsSwing.eventComboBoxModel(peaks));
        MyListCellRenderer renderer = new MyListCellRenderer(peaks);
        box.setRenderer(renderer);
        box.setEditable(true);

        AutoCompleteDecorator.decorate(box, new ObjectToStringConverter() {
            @Override
            public String getPreferredStringForItem(Object item) {
                if (item instanceof Peak) {
                    Peak peak = (Peak) item;
                    return String.valueOf(peak.getMass());
                } else {
                    return (String) item;
                }

            }
        });
        return box;
    }

    public boolean setSelectedItem(double mass) {
        MsExperiments.PrecursorCandidates masses = MsExperiments.findPossiblePrecursorPeaks(peaks, mass);
        box.setSelectedItem(masses.getDefaultPrecursor() != null ? masses.getDefaultPrecursor() : String.valueOf(mass));
        return true;
    }

    public double getSelectedIonMass() {
        Object selected = box.getSelectedItem();
        double pm;
        if (selected instanceof Peak) {
            Peak cp = (Peak) selected;
            pm = cp.getMass();
        } else if (selected != null && !selected.toString().isEmpty()) {
            pm = Double.parseDouble(selected.toString());
        } else return 0d;
        return pm;
    }


    private class MyListCellRenderer extends JLabel implements ListCellRenderer<Peak> {

        private double maxInt;
        private Font textfont;
        private Color massColor, intColor, selectedForeground, selectedBackground;

        private DecimalFormat numberFormat;

        private Peak cp;
        private Collection<Peak> peaks;

        private int intPos;

        private int idealWidth;

        boolean isInit;
        boolean isSelected;

        MyListCellRenderer(Collection<Peak> peaks) {

            initColorsAndFonts();

            isInit = false;
            this.peaks = peaks;
            intPos = 0;
            isSelected = false;

            this.numberFormat = new DecimalFormat("#0.0%");
            //		FontMetrics fm = Toolkit.getDefaultToolkit().getfo
            //		 = this.getGraphics().getFontMetrics(textfont);this.getgr

            maxInt = 0;
            for (Peak peak : peaks) {
                if (peak.getIntensity() > maxInt) maxInt = peak.getIntensity();
            }

            //		this.setMinimumSize(new Dimension(145,15));
            //		this.setPreferredSize(new Dimension(145,15));
            //		this.setSize(new Dimension(151,15));

            cp = null;

            computeSize();

        }

        public void computeSize() {
            BufferedImage im = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);

            FontMetrics fm = im.getGraphics().getFontMetrics(this.textfont);

            int maxMassWidth = 0;
            int maxIntWidth = 0;
            for (Peak peak : peaks) {
                String massS = String.valueOf(peak.getMass());
                String intS = numberFormat.format(peak.getIntensity() / maxInt);
                int massWidth = fm.stringWidth(massS);
                int intWidth = fm.stringWidth(intS);
                if (massWidth > maxMassWidth) maxMassWidth = massWidth;
                if (intWidth > maxIntWidth) maxIntWidth = intWidth;
                //			int width = fm.stringWidth(massS)+fm.stringWidth(intS)+20;
                //			if(width>maxWidth) maxWidth = width;
            }

            this.intPos = 15 + maxMassWidth;

            this.idealWidth = maxMassWidth + maxIntWidth + 20;

            this.setSize(new Dimension(idealWidth, 15));
            this.setPreferredSize(new Dimension(idealWidth, 15));
            this.setMinimumSize(new Dimension(idealWidth, 15));

        }

        public void initColorsAndFonts() {
            try {
                InputStream fontFile = getClass().getResourceAsStream("/ttf/DejaVuSans.ttf");
                Font tempFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                textfont = tempFont.deriveFont(12f);
            } catch (Exception e) {
                LoggerFactory.getLogger(this.getClass()).error(e.getMessage(), e);
            }

            massColor = Color.BLACK;
            intColor = new Color(83, 134, 139);


            selectedBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].background");
            selectedForeground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\"[Selected].textForeground");
            //		evenBackground = UIManager.getColor("ComboBox:\"ComboBox.listRenderer\".background");
            //		disableBackground = UIManager.getColor("ComboBox.background");
            //		System.out.println("Farbe: "+disableBackground);
            //		unevenBackground = new Color(213,227,238);
            //		activatedForeground = UIManager.getColor("List.foreground");
            //		deactivatedForeground = Color.GRAY;

        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends Peak> list, Peak value, int index,
                boolean isSelected, boolean cellHasFocus) {
            this.setText(value.getMass() + " " + value.getIntensity());
            this.cp = value;

            this.isSelected = isSelected;
            return this;
        }


        @Override
        public void paint(Graphics g) {

            Graphics2D g2 = (Graphics2D) g;

            FontMetrics fm = g2.getFontMetrics(this.textfont);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (isSelected) {
                g2.setColor(this.selectedBackground);
            } else {
                g2.setColor(Color.white);
            }


            g2.fillRect(0, 0, (int) this.getSize().getWidth(), (int) this.getSize().getWidth());

            if (cp == null) return;

            //		FontMetrics fm = g2.getFontMetrics(this.textfont);

            String massS = String.valueOf(cp.getMass());
            String intS = numberFormat.format(cp.getIntensity() / maxInt);

            if (isSelected) {
                g2.setColor(this.selectedForeground);
            } else {
                g2.setColor(massColor);
            }

            g2.drawString(massS, 5, 12);

            if (isSelected) {
                g2.setColor(this.selectedForeground);
            } else {
                g2.setColor(intColor);
            }

            g2.drawString(intS, intPos, 12);

        }

    }
}
