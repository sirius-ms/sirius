package de.unijena.bioinf.ms.gui.sirius.msviewer.data;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.ms.gui.sirius.ExperimentResultBean;
import de.unijena.bioinf.ms.gui.sirius.SiriusResultElement;

import javax.swing.*;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ExperimentContainerDataModel implements MSViewerDataModel {

    public static final String MS1_DISPLAY = "MS 1", MS1_MERGED_DISPLAY = "MS 1 merged", MSMS_DISPLAY = "MSMS", MSMS_MERGED_DISPLAY = "MSMS merged";
    private static final DecimalFormat cEFormat = new DecimalFormat("#0.0");

    //data model
    protected ExperimentResultBean ec; //todo maybe remove this
    protected IdentificationResult currentResult;
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

    public boolean changeData(ExperimentResultBean ec, SiriusResultElement result) {
        if (this.ec != ec || (result != null && this.currentResult != result.getResult())) {
            this.ec = ec;

            if (result != null && result.getResult() != null) {
                this.currentResult = result.getResult();
            } else {
                this.currentResult = null;
            }

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

                            if (minEn == maxEn) {
                                key = cEFormat.format(minEn) + " eV";
                            } else {
                                key = cEFormat.format(minEn) + "-" + cEFormat.format(maxEn) + " eV";
                            }
                            int counter = 2;
                            while (identifierToSpectrum.containsKey(key)) {
                                if (minEn == maxEn) {
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

        if (id.equals(MSMS_MERGED_DISPLAY)) {
            if (currentResult != null) {
                //todo @kai, why is the ms2experiment modified during view???
                final FTree tree = currentResult.getRawTree();
                final MutableMs2Experiment ms2;

                if (tree != null && tree.getAnnotationOrNull(ProcessedInput.class) != null) {
                    ms2 = tree.getAnnotationOrNull(ProcessedInput.class).getExperimentInformation();
                } else {
                    ms2 = ec.getMs2Experiment();
                }

                //ioMass
                double ionMass;
                if (tree != null && tree.getAnnotationOrNull(PrecursorIonType.class) != null) {
                    ionMass = tree.getAnnotationOrNull(PrecursorIonType.class).addIonAndAdduct(tree.getRoot().getFormula().getMass());
                } else {
                    ionMass = ec.getIonMass();
                }

                // remove peaks behind the parent
                /*
                for (MutableMs2Spectrum ms2spec : ms2.getMs2Spectra()) {
                    Spectrums.cutByMassThreshold(ms2spec, ionMass + 1d);
                }
                */

                underlyingModel = new SiriusMergedMs2Annotated(tree, ms2, minMz, maxMz);
            } else {
                underlyingModel = new SiriusMergedMs2(ec.getMs2Experiment(), minMz, maxMz);
            }
        } else if (spec == null) {
            underlyingModel = new DummySpectrumModel();
        } else {
            if (currentResult != null) {
                final FTree tree = currentResult.getRawTree();
                if (spec.getMsLevel() == 1) {
                    underlyingModel = new SiriusIsotopePattern(tree, spec);
                } else {
                    underlyingModel = new SiriusSingleSpectrumAnnotated(tree, spec, minMz, maxMz);
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
