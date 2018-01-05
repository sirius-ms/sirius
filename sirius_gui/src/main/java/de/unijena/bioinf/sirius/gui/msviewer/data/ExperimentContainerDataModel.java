package de.unijena.bioinf.sirius.gui.msviewer.data;

import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.ProcessedInput;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import java.util.TreeSet;

public class ExperimentContainerDataModel implements MSViewerDataModel {

    public enum DisplayMode {
        MS, MSMS, MERGED, DUMMY
    }

    protected ExperimentContainer ec;
    protected IdentificationResult currentResult;
    protected MSViewerDataModel underlyingModel;

    protected TreeSet<Integer> marked;

    protected double minMz, maxMz;

    public ExperimentContainerDataModel(ExperimentContainer ec) {
        this.ec = ec;
        this.marked = new TreeSet<Integer>();
        refreshRanges();
        showDummySpectrum();
    }

    public DisplayMode getMode() {
        return mode;
    }

    public int getSelectedMs2Spectrum() {
        return selectedMs2Spectrum;
    }

    public String toString() {
        return underlyingModel.getClass().getName();
    }

    protected DisplayMode mode;
    protected int selectedMs2Spectrum;

    protected void refreshSpectrum() {
        if (mode == DisplayMode.MS) {
            final SimpleSpectrum spec = ec.getMs2Experiment().getMergedMs1Spectrum();
            if (spec == null) {
                underlyingModel = new DummySpectrumModel();
            } else if (currentResult != null) {
                final FTree tree = currentResult.getRawTree();
                underlyingModel = new SiriusIsotopePattern(tree, spec);
            } else {
                underlyingModel = new SiriusSingleSpectrumModel(spec);
            }
        } else if (mode == DisplayMode.DUMMY) {
            underlyingModel = new DummySpectrumModel();
        } else {
            if (currentResult != null) {
                final FTree tree = currentResult.getRawTree();
                final ProcessedInput experiment = tree.getAnnotationOrNull(ProcessedInput.class);
                final MutableMs2Experiment ms2;
                if (experiment != null) {
                    ms2 = experiment.getExperimentInformation();
                } else {
                    ms2 = ec.getMs2Experiment();
                }
                double ionMass;
                if (tree != null && tree.getAnnotationOrNull(PrecursorIonType.class) != null) {
                    ionMass = tree.getAnnotationOrNull(PrecursorIonType.class).addIonAndAdduct(tree.getRoot().getFormula().getMass());
                } else {
                    ionMass = ec.getSelectedFocusedMass();
                }
                // remove peaks behind the parent
                for (MutableMs2Spectrum ms2spec : ms2.getMs2Spectra()) {
                    Spectrums.cutByMassThreshold(ms2spec, ionMass + 1d);
                }

                if (mode == DisplayMode.MSMS) {
                    underlyingModel = new SiriusSingleSpectrumAnnotated(tree, ms2.getMs2Spectra().get(selectedMs2Spectrum), minMz, maxMz);
                } else if (mode == DisplayMode.MERGED) {
                    underlyingModel = new SiriusMergedMs2Annotated(tree, ms2, minMz, maxMz);
                } else {
                    underlyingModel = new DummySpectrumModel();
                }
            } else {
                final Ms2Experiment experiment = ec.getMs2Experiment();
                if (mode == DisplayMode.MSMS) {
                    underlyingModel = new SiriusSingleSpectrumModel(experiment.getMs2Spectra().get(selectedMs2Spectrum), minMz, maxMz);
                } else if (mode == DisplayMode.MERGED) {
                    underlyingModel = new SiriusMergedMs2(experiment, minMz, maxMz);
                } else {
                    underlyingModel = new DummySpectrumModel();
                }
            }
        }
    }

    public void changeData(ExperimentContainer container, SiriusResultElement result, DisplayMode mode, int ms2index) {
        this.ec = container;
        if (result != null && result.getResult() != null) {
            this.currentResult = result.getResult();
        } else {
            this.currentResult = null;
        }
        this.mode = mode;
        this.selectedMs2Spectrum = ms2index;
        refreshRanges();
        refreshSpectrum();
    }

    private void refreshRanges() {
        Range<Double> range = null;
        if (ec == null) {
            this.minMz = 0d;
            this.maxMz = 400d;
        } else {
            /*
            for (CompactSpectrum spec : ec.getMs2Spectra()) {
                Range<Double> mzRange = SiriusSingleSpectrumAnnotated.getVisibleRange(SiriusDataConverter.myxoMs2ToSiriusMs2(spec, ec.getFocusedMass()));
                if (range == null) range = mzRange;
                else range = range.span(mzRange);
            }
            this.minMz = range.lowerEndpoint();
            this.maxMz = ec.getDataFocusedMass()+5;
            */
            this.minMz = 0d;
            this.maxMz = ec.getFocusedMass() + 5;
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

    public void showDummySpectrum() {
        mode = DisplayMode.DUMMY;
        selectedMs2Spectrum = -1;
        refreshSpectrum();
    }

    public void selectMS1Spectrum() {
        mode = DisplayMode.MS;
        refreshSpectrum();
    }

    public void selectMS2Spectrum(int index) {
        mode = DisplayMode.MSMS;
        selectedMs2Spectrum = index;
        refreshSpectrum();
    }

    public void selectMergedSpectrum() {
        mode = DisplayMode.MERGED;
        selectedMs2Spectrum = -1;
        refreshSpectrum();
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

//    @Override
//    public double getSignalNoise(int index) {
//        return underlyingModel.getSignalNoise(index);
//    }

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
}
