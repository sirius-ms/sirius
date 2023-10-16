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

/**
 * An object of this class predicts a barcode tandem mass spectrum
 * (each peak in the barcode spectrum has intensity one).
 */
public class BarcodeSpectrumPredictor extends AbstractMs2SpectrumPredictor<Peak>{

    /**
     * Determines whether [M+H]+ or [M-H]- is assumed.
     */
    private final boolean isPositiveMode;

    /**
     * The number of considered hydrogen shifts occurring during fragmentation.
     */
    private final int numHydrogenShifts;

    /**
     * Constructs a BarcodeSpectrumPredictor.<br>
     * If {@code positiveMode} is set to {@code true}, the considered {@link PrecursorIonType} will be [M+H]+;
     * if it is set to {@code false}, the considered PrecursorIonType will be [M-H]-.
     *
     * @param fragPredictor the {@link FragmentationPredictor} used for predicting the fragments of the molecule
     *                      generated during MS/MS acquisition
     * @param positiveMode if {@code true}, the considered precursor ion type is [M+H]+;
     *                     otherwise [M-H]-
     * @param numHydrogenShifts the number of possible hydrogen rearrangements which can occur during fragmentation
     */
    public BarcodeSpectrumPredictor(FragmentationPredictor fragPredictor, boolean positiveMode, int numHydrogenShifts){
        super(fragPredictor, positiveMode ? PeriodicTable.getInstance().getPrecursorProtonation() : PeriodicTable.getInstance().getPrecursorDeprotonation());
        this.isPositiveMode = positiveMode;
        this.numHydrogenShifts = numHydrogenShifts;
    }

    /**
    * Constructs a BarcodeSpectrumPredictor.<br>
    * If {@code positiveMode} is set to {@code true}, the considered {@link PrecursorIonType} will be [M+H]+;
    * if it is set to {@code false}, the considered PrecursorIonType will be [M-H]-.
    *
    * @param fragPredictor the {@link FragmentationPredictor} used for predicting the fragments of the molecule
    *                      generated during MS/MS acquisition
    * @param positiveMode if {@code true}, the considered precursor ion type is [M+H]+;
    *                     otherwise [M-H]-
    */
    public BarcodeSpectrumPredictor(FragmentationPredictor fragPredictor, boolean positiveMode){
        this(fragPredictor, positiveMode, 0);
    }


    @Override
    public Ms2Spectrum<Peak> predictSpectrum() {
        List<CombinatorialFragment> predFragments = this.fragPredictor.getFragments();
        SimpleMutableSpectrum spec = new SimpleMutableSpectrum(predFragments.size()*(2*this.numHydrogenShifts+1));

        for(CombinatorialFragment fragment : predFragments){
            double neutralFragmentMass = fragment.getFormula().getMass();
            double fragmentMz = this.precursorIonType.neutralMassToPrecursorMass(neutralFragmentMass);

            for(int h = -this.numHydrogenShifts; h <= this.numHydrogenShifts; h++){
                double shiftedFragmentMz = fragmentMz + h * 1.007276;
                SimplePeak peak = new SimplePeak(shiftedFragmentMz, 1d);
                spec.addPeak(peak);
                this.peak2fragment.put(peak, fragment);
            }
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
