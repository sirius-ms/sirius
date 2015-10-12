package de.unijena.bioinf.sirius.gui.structure;

import de.unijena.bioinf.myxo.gui.msview.data.DefaultPeakInformation;
import de.unijena.bioinf.myxo.gui.msview.data.MSViewerDataModel;
import de.unijena.bioinf.myxo.gui.msview.data.PeakInformation;
import de.unijena.bioinf.myxo.structure.CompactSpectrum;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class ResultsMSViewerDataModel implements MSViewerDataModel {

    ExperimentContainer ec;

    private CompactSpectrum selectedSpectrum;
    private boolean[] isImportant;
    private TreeSet<Integer> isMarkedSet; //TODO gegen was mit primitiven Datentypen ersetzen
    private String[] mfStorage; //sinnvoll?
//	private List<DefaultMyxoPeak> peaks;

    public ResultsMSViewerDataModel(ExperimentContainer ec) {
        this.ec = ec;
        selectedSpectrum = null;
        isImportant = null;
        mfStorage = null;
        isMarkedSet = new TreeSet<>();
//		peaks = null;
    }

    public void showDummySpectrum() {
        this.selectedSpectrum = null;
    }

    public void selectMS1Spectrum() {
        if (ec != null) {
            List<CompactSpectrum> ms1 = ec.getMs1Spectra();
            if (ms1 == null || ms1.isEmpty()) return;
            this.selectedSpectrum = ms1.get(0);
            update();
        }
    }

    public void selectMS2Spectrum(int index) {
        if (ec != null) {
            List<CompactSpectrum> ms2 = ec.getMs2Spectra();
            if (ms2 == null || index >= ms2.size()) return;
            this.selectedSpectrum = ms2.get(index);
            update();
        }
    }

    private void update() {
        if (selectedSpectrum == null) {
            this.isImportant = null;
            this.mfStorage = null;
        } else {
            this.isImportant = new boolean[selectedSpectrum.getSize()];
            Arrays.fill(isImportant, false);
            this.mfStorage = new String[selectedSpectrum.getSize()];
            Arrays.fill(mfStorage, null);
        }
        isMarkedSet.clear();
//		isMarked = selectedSpectrum == null ? null : new boolean[selectedSpectrum.getSize()];

//		peaks = new ArrayList<>();
//		for(selectedSpectrum.get)
    }


    @Override
    public int findIndexOfPeak(double mass, double tolerance) { //TODO schneller implementieren
        if (selectedSpectrum == null) return -1;
        tolerance = Math.abs(tolerance);
        double minMass = mass - tolerance;
        double maxMass = mass + tolerance;
        for (int i = 0; i < selectedSpectrum.getSize(); i++) {
            if (selectedSpectrum.getPeak(i).getMass() >= minMass && selectedSpectrum.getPeak(i).getMass() <= maxMass) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public double getAbsoluteIntensity(int index) {
        if (selectedSpectrum == null) return -1;
        return this.selectedSpectrum.getAbsoluteIntensity(index);
    }

    @Override
    public int getIndexWithMass(double mass) {
        if (selectedSpectrum == null) return -1;
        for (int i = 0; i < selectedSpectrum.getSize(); i++) {
            if (mass == selectedSpectrum.getMass(i)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public PeakInformation getInformations(int index) {
        if (selectedSpectrum == null) return null;
        DefaultPeakInformation dpi = new DefaultPeakInformation();
        dpi.setAbsoluteIntensity(selectedSpectrum.getAbsoluteIntensity(index));
        dpi.setMass(selectedSpectrum.getMass(index));
        dpi.setRelativeIntensity(selectedSpectrum.getRelativeIntensity(index));
        dpi.setSignalNoise(selectedSpectrum.getSignalToNoise(index));
        return dpi;
    }

    @Override
    public int[] getIsotopePeaks(int index) {
        return null;
    }

    @Override
    public String getLabel() {
        return "";
    }

    @Override
    public double getMass(int index) {
        if (selectedSpectrum == null) return -1;
        return selectedSpectrum.getMass(index);
    }

    @Override
    public double getRelativeIntensity(int index) {
        if (selectedSpectrum == null) return -1;
        return selectedSpectrum.getRelativeIntensity(index);
    }

    @Override
    public double getSignalNoise(int index) {
        if (selectedSpectrum == null) return -1;
        return selectedSpectrum.getSignalToNoise(index);
    }

    @Override
    public int getSize() {
        if (selectedSpectrum == null) return 0;
        return selectedSpectrum.getSize();
    }

    @Override
    public boolean isIsotope(int arg0) {
        return false;
    }

    @Override
    public boolean isMarked(int index) {
        if (selectedSpectrum == null) return false;
        return isMarkedSet.contains(index);
    }

    public void setMarked(int index, boolean isMarked) {
        if (selectedSpectrum == null) return;
        if (isMarked) this.isMarkedSet.add(index);
        else this.isMarkedSet.remove(index);
    }

    public void removeMarkings() {
        this.isMarkedSet.clear();
    }

    public boolean markingsPresent() {
        if (selectedSpectrum == null) return false;
        return this.isMarkedSet.size() > 0;
    }

    public int getFirstMarkedIndex() {
        if (selectedSpectrum == null) return -1;
        if (this.isMarkedSet.isEmpty()) return -1;
        else return this.isMarkedSet.first();
    }

    public int getLastMarkedIndex() {
        if (selectedSpectrum == null) return -1;
        if (this.isMarkedSet.isEmpty()) return -1;
        else return this.isMarkedSet.last();
    }

    @Override
    public boolean isPlusZeroPeak(int arg0) {
        return false;
    }

    public void setImportant(int index, boolean important) {
        if (selectedSpectrum == null) return;
        this.isImportant[index] = important;
    }

    public void markAllPeaksAsUnimportant() {
        if (selectedSpectrum == null) return;
        Arrays.fill(this.isImportant, false);
    }

    @Override
    public String getMolecularFormula(int index) {
        if (selectedSpectrum == null) return null;
        return this.mfStorage[index];
    }

    public void setMolecularFormula(int index, String mf) {
        if (selectedSpectrum == null) return;
        this.mfStorage[index] = mf;
    }

    @Override
    public boolean isImportantPeak(int index) {
        if (selectedSpectrum == null) return false;
        return this.isImportant[index];
    }

    @Override
    public boolean isUnimportantPeak(int arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}
