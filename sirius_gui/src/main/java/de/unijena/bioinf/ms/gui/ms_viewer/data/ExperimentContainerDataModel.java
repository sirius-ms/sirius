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

package de.unijena.bioinf.ms.gui.ms_viewer.data;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.projectspace.FormulaResultBean;
import de.unijena.bioinf.projectspace.FormulaResultId;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ExperimentContainerDataModel implements MSViewerDataModel {
    private static final String NA_EV = "N/A eV";
    public static final String MS1_DISPLAY = "MS 1", MS1_MERGED_DISPLAY = "MS 1 merged", MSMS_DISPLAY = "MSMS", MSMS_MERGED_DISPLAY = "MSMS merged";
    private static final DecimalFormat cEFormat = new DecimalFormat("#0.0");

    //data model
    protected InstanceBean ec; //todo maybe remove this
    protected FormulaResultBean currentResult;
    private Map<String, Spectrum<?>> identifierToSpectrum = new HashMap<>();
    private final DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<>();


    protected MSViewerDataModel underlyingModel;
    protected TreeSet<Integer> marked;

    protected double minMz, maxMz;

    public ExperimentContainerDataModel() {
        this.ec = null;
        this.marked = new TreeSet<Integer>();

        refreshRanges();
        selectSpectrum(null); //shows dummy spectrum
    }

    public String toString() {
        return underlyingModel.getClass().getName();
    }

    public DefaultComboBoxModel<String> getComboBoxModel() {
        return cbModel;
    }

    public boolean changeData(InstanceBean ec, FormulaResultBean result) {
        final FormulaResultId currentID = this.currentResult != null ? this.currentResult.getID() : null;
        final FormulaResultId newID = result != null ? result.getID() : null;

        if (this.ec != ec || currentID != newID) {
            this.ec = ec;

            this.currentResult = result;
            cbModel.removeAllElements();
            identifierToSpectrum = new HashMap<>();

            if (ec != null) {
                List<SimpleSpectrum> ms1 = ec.getMs1Spectra();
                List<MutableMs2Spectrum> ms2 = ec.getMs2Spectra();

                //addMS1 and merged ms1
                if (!ms1.isEmpty() || ec.getMergedMs1Spectrum() != null) {
                    if (ec.getMergedMs1Spectrum() != null) {
                        cbModel.addElement(MS1_MERGED_DISPLAY);
                        identifierToSpectrum.put(MS1_MERGED_DISPLAY, ec.getMergedMs1Spectrum());
                    } else if (ms1.size() > 1) {
                        cbModel.addElement(MS1_MERGED_DISPLAY);
                        identifierToSpectrum.put(MS1_MERGED_DISPLAY, merge(ms1));
                    }

                    for (SimpleSpectrum ms1Spec : ms1) {
                        final String key = buildName(MS1_DISPLAY);
                        identifierToSpectrum.put(key, ms1Spec);
                        cbModel.addElement(key);
                    }
                }

                //add ms2 and merged ms2
                if (ms2 != null) {
                    if (ms2.size() > 1) {
                        cbModel.addElement(MSMS_MERGED_DISPLAY);
                        identifierToSpectrum.put(MSMS_MERGED_DISPLAY, null);//calculated on demand
                    }

                    for (MutableMs2Spectrum sp : ms2) {
                        String key = null;

                        if (sp.getCollisionEnergy() != null) {
                            double minEn = sp.getCollisionEnergy().getMinEnergy();
                            double maxEn = sp.getCollisionEnergy().getMaxEnergy();

                            if (minEn == 0d && maxEn == 0d) {
                                key = NA_EV;
                            } else if (minEn == maxEn) {
                                key = cEFormat.format(minEn) + " eV";
                            } else {
                                key = cEFormat.format(minEn) + "-" + cEFormat.format(maxEn) + " eV";
                            }
                            int counter = 2;
                            while (identifierToSpectrum.containsKey(key)) {
                                if (key.equals(NA_EV)) {
                                    key = NA_EV + " (" + counter + ")";
                                } else if (minEn == maxEn) {
                                    key = cEFormat.format(minEn) + " eV (" + counter + ")";
                                } else {
                                    key = cEFormat.format(minEn) + "-" + cEFormat.format(maxEn) + " eV (" + counter + ")";
                                }
                                counter++;
                            }
                        } else {
                            key = buildName(MSMS_DISPLAY);
                        }

                        identifierToSpectrum.put(key, sp);
                        cbModel.addElement(key);
                    }
                }
            }

            refreshRanges();
            return true;
        }
        return false;
    }

    private SimpleSpectrum merge(List<? extends Spectrum<Peak>> spectra) {
        return Spectrums.mergeSpectra(new Deviation(10, 0.1), true, false, spectra);
    }

    private String buildName(final String prefix) {
        String key = prefix;
        int counter = 2;
        while (identifierToSpectrum.containsKey(key)) {
            key = prefix + " (" + counter + ")";
            counter++;
        }
        return key;
    }

    private void refreshRanges() {
        if (ec == null) {
            this.minMz = 0d;
            this.maxMz = 400d;
        } else {
            this.minMz = 0d;
            this.maxMz = ec.getIonMass() + 5;
        }
    }

    private Spectrum<?> getSpectrumByID(String id) {
        if (this.ec == null) return null;
        return identifierToSpectrum.get(id);
    }

    public void selectSpectrum(final String id) {
        if (id == null) {
            underlyingModel = new DummySpectrumModel();
            return;
        }
        final Spectrum<?> spec = getSpectrumByID(id);

        if (spec == null || spec.isEmpty()){
            underlyingModel = new DummySpectrumModel();
            LoggerFactory.getLogger(getClass()).warn("Cannot render empty Spectrum!");
            return;
        }

        if (id.equals(MSMS_MERGED_DISPLAY)) {
            if (currentResult != null && currentResult.getFragTree().isPresent()) {
                underlyingModel = new SiriusMergedMs2Annotated(currentResult.getFragTree().get(), ec.getExperiment(), minMz, maxMz);
            } else {
                underlyingModel = new SiriusMergedMs2(ec.getExperiment(), minMz, maxMz);
            }
        } else if (spec == null) {
            underlyingModel = new DummySpectrumModel();
        } else {
            if (currentResult != null && currentResult.getFragTree().isPresent()) {
                if (spec.getMsLevel() == 1) {
                    underlyingModel = new SiriusIsotopePattern(currentResult.getFragTree().get(), ec.getExperiment(), spec);
                } else {
                    underlyingModel = new SiriusSingleSpectrumAnnotated(currentResult.getFragTree().get(), spec, minMz, maxMz);
                }
            } else {
                underlyingModel = new SiriusSingleSpectrumModel(spec);
            }
        }
    }

    public void setMarked(int index, boolean add) {
        if (add) marked.add(index);
        else marked.remove(index);
    }

    public int getFirstMarkedIndex() {
        return marked.first();
    }

    public int getLastMarkedIndex() {
        return marked.last();
    }

    public void removeMarkings() {
        marked.clear();
    }

    @Override
    public double minMz() {
        return underlyingModel.minMz();
    }

    @Override
    public double maxMz() {
        return underlyingModel.maxMz();
    }

    @Override
    public int getSize() {
        return underlyingModel.getSize();
    }

    @Override
    public double getMass(int index) {
        return underlyingModel.getMass(index);
    }

    @Override
    public double getRelativeIntensity(int index) {
        return underlyingModel.getRelativeIntensity(index);
    }

    @Override
    public double getAbsoluteIntensity(int index) {
        return underlyingModel.getAbsoluteIntensity(index);
    }

    @Override
    public String getMolecularFormula(int index) {
        return underlyingModel.getMolecularFormula(index);
    }

    @Override
    public PeakInformation getInformations(int index) {
        return underlyingModel.getInformations(index);
    }

    @Override
    public boolean isMarked(int index) {
        return marked.contains(index);
    }

    @Override
    public boolean isImportantPeak(int index) {
        return underlyingModel.isImportantPeak(index);
    }

    @Override
    public boolean isUnimportantPeak(int index) {
        return underlyingModel.isUnimportantPeak(index);
    }

    @Override
    public boolean isPlusZeroPeak(int index) {
        return underlyingModel.isPlusZeroPeak(index);
    }

    @Override
    public boolean isIsotope(int index) {
        return underlyingModel.isIsotope(index);
    }

    @Override
    public int[] getIsotopePeaks(int index) {
        return underlyingModel.getIsotopePeaks(index);
    }

    @Override
    public String getLabel() {
        return "";
    }

    @Override
    public int getIndexWithMass(double mass) {
        return underlyingModel.getIndexWithMass(mass);
    }

    @Override
    public int findIndexOfPeak(double mass, double tolerance) {
        return underlyingModel.findIndexOfPeak(mass, tolerance);
    }

    @Override
    public String getIonization(int index) {
        return underlyingModel.getIonization(index);
    }
}
