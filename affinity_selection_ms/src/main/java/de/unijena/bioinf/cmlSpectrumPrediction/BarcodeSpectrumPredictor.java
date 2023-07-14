package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.MolecularGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarcodeSpectrumPredictor extends SpectrumPredictor<Peak>{

    private final boolean isPositiveMode;
    private MutableMs2Spectrum spectrum;
    private HashMap<Peak, CombinatorialFragment> mapping;

    public BarcodeSpectrumPredictor(FragmentationPredictor fragPredictor, boolean positiveMode){
        super(fragPredictor, positiveMode ? PeriodicTable.getInstance().getPrecursorProtonation() : PeriodicTable.getInstance().getPrecursorDeprotonation());
        this.isPositiveMode = positiveMode;
    }

    @Override
    public Ms2Spectrum<Peak> predictSpectrum() {
        FragmentationPredictor fragPredictor = this.getFragmentationPredictor();
        PrecursorIonType ionType = this.getPrecursorIonType();
        List<CombinatorialFragment> predFragments = fragPredictor.getFragments();

        SimpleMutableSpectrum spec = new SimpleMutableSpectrum(predFragments.size());
        this.mapping = new HashMap<>(predFragments.size());
        for(CombinatorialFragment fragment : predFragments){
            double neutralFragmentMass = fragment.getFormula().getMass();
            double fragmentMz = ionType.neutralMassToPrecursorMass(neutralFragmentMass);
            SimplePeak peak = new SimplePeak(fragmentMz, 1d);

            spec.addPeak(peak);
            this.mapping.put(peak, fragment);
        }

        MolecularGraph precursorMolecule = fragPredictor.getMolecule();
        double precursorMz = ionType.neutralMassToPrecursorMass(precursorMolecule.getFormula().getMass());
        this.spectrum = new MutableMs2Spectrum(spec, precursorMz, null, 2);
        return this.spectrum;
    }

    @Override
    public Map<Peak, CombinatorialFragment> getPeakFragmentMapping() {
        return this.mapping;
    }

    public boolean isPositiveMode(){
        return this.isPositiveMode;
    }

    public MutableMs2Spectrum getSpectrum(){
        return this.spectrum;
    }
}
