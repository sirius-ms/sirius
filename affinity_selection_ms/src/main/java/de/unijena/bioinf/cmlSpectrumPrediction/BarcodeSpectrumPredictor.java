package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.cmlFragmentation.FragmentationPredictor;
import de.unijena.bioinf.fragmenter.CombinatorialFragment;
import de.unijena.bioinf.fragmenter.MolecularGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarcodeSpectrumPredictor extends AbstractMs2SpectrumPredictor<Peak>{

    private final boolean isPositiveMode;

    public BarcodeSpectrumPredictor(FragmentationPredictor fragPredictor, boolean positiveMode){
        super(fragPredictor, positiveMode ? PeriodicTable.getInstance().getPrecursorProtonation() : PeriodicTable.getInstance().getPrecursorDeprotonation());
        this.isPositiveMode = positiveMode;
    }

    @Override
    public Ms2Spectrum<Peak> predictSpectrum() {
        List<CombinatorialFragment> predFragments = this.fragPredictor.getFragments();
        SimpleMutableSpectrum spec = new SimpleMutableSpectrum(predFragments.size());

        for(CombinatorialFragment fragment : predFragments){
            double neutralFragmentMass = fragment.getFormula().getMass();
            double fragmentMz = this.precursorIonType.neutralMassToPrecursorMass(neutralFragmentMass);
            SimplePeak peak = new SimplePeak(fragmentMz, 1d);
            spec.addPeak(peak);
            this.peak2fragment.put(peak, fragment);
        }
        Spectrums.sortSpectrumByMass(spec);

        MolecularGraph precursorMolecule = this.fragPredictor.getMolecule();
        double precursorMz = this.precursorIonType.neutralMassToPrecursorMass(precursorMolecule.getFormula().getMass());
        this.peak2fragment.put(new SimplePeak(precursorMz, 1d), precursorMolecule.asFragment());
        this.spectrum = new MutableMs2Spectrum(spec, precursorMz, null, 2);
        return this.spectrum;
    }

    public boolean isPositiveMode(){
        return this.isPositiveMode;
    }

}
