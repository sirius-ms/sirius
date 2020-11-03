/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.utils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.swing.GlazedListsSwing;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.MsExperiments;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ms.gui.configs.Colors;
import de.unijena.bioinf.ms.gui.configs.Fonts;
import de.unijena.bioinf.ms.gui.io.spectrum.SpectrumContainer;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

// todo dummy String for size calculation
public class PrecursorSelector extends JPanel {
    public static final String name = "Precursor mass (m/z)";

    private final JComboBox<Peak> box;
    private final JButton autoDetectFM = new JButton("Most intensive");
    private final BasicEventList<Peak> peaks = new BasicEventList<>();
    private double maxInt;


    public PrecursorSelector(final InstanceBean ec) {
        this(ec.getExperiment());
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
        autoDetectFM.addActionListener(e -> setMostIntensivePrecursorMass());

        add(box);
        add(autoDetectFM);
    }

    protected boolean setMostIntensivePrecursorMass() {
        box.setSelectedItem(peaks.stream().max(Comparator.comparingDouble(Peak::getIntensity)).orElse(null));
        return box.getSelectedItem() == null;
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
        setMaxMass();
        if (peaks.isEmpty())
            autoDetectFM.setEnabled(false);
        else
            setMostIntensivePrecursorMass();
    }

    private void setMaxMass() {
        maxInt = 0;
        for (Peak peak : peaks)
            maxInt = Math.max(maxInt, peak.getIntensity());
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
        setMaxMass();
        box.setSelectedItem(masses.getDefaultPrecursor() != null ? masses.getDefaultPrecursor() : String.valueOf(ioMass));
        if (peaks.isEmpty()) autoDetectFM.setEnabled(false);

    }


    private JComboBox<Peak> createParentPeakSelector() {
        //create box
        JComboBox<Peak> box = new JComboBox<>(GlazedListsSwing.eventComboBoxModel(peaks));
        box.setRenderer(new MyListCellRenderer());
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
        if (mass <= 0) {
            setMostIntensivePrecursorMass();
            return false;
        } else {
            MsExperiments.PrecursorCandidates masses = MsExperiments.findPossiblePrecursorPeaks(peaks, mass);
            box.setSelectedItem(masses.getDefaultPrecursor() != null ? masses.getDefaultPrecursor() : String.valueOf(mass));
            return true;
        }
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


    private class MyListCellRenderer implements ListCellRenderer<Peak> {

        protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

        private DecimalFormat numberFormat = new DecimalFormat("#0.0%");
        private DecimalFormat numberFormatMass = new DecimalFormat("#0.0000");

        MyListCellRenderer() {
            defaultRenderer.setFont(Fonts.FONT.deriveFont(12f));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Peak> list, Peak value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index,
                    isSelected, cellHasFocus);

            final String intS = numberFormat.format(value.getIntensity() / maxInt);
            final String massS = numberFormatMass.format(value.getMass());

            Color foreColor;
            Color backColor;

            if (isSelected) {
                backColor = Colors.LIST_SELECTED_BACKGROUND;
                foreColor = Colors.LIST_SELECTED_FOREGROUND;
            } else {
                if (index % 2 == 0) backColor = Colors.LIST_EVEN_BACKGROUND;
                else backColor = Colors.LIST_UNEVEN_BACKGROUND;
                foreColor = Colors.LIST_ACTIVATED_FOREGROUND;
            }

            renderer.setBackground(backColor);
            renderer.setForeground(foreColor);
            renderer.setText("<html>" + massS + " <font color=rgb(83,134,139)>(" + intS + ")</font>" + "</html>");

            return renderer;
        }
    }
}
