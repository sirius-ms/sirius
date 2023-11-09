package de.unijena.bioinf.cmlSpectrumPrediction;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
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
     * The number of considered hydrogen shifts occurring during fragmentation.
     */
    private final int numHydrogenShifts;

    /**
     * Constructs a BarcodeSpectrumPredictor.<br>
     * This predictor takes the <b>already predicted</b> fragments of {@code fragPredictor} and constructs an MS/MS spectrum.
     * Corresponding to their masses and to the {@code precursorIonType}, each fragment is assigned an m/z value and
     * an intensity of 1.0.<br>
     * Because there is the possibility that hydrogen rearrangements occur during CID, there will be peaks at
     * {@code x-numHydrogenShifts},...,{@code x},...,{@code x+numHydrogenShifts}.
     *
     * @param fragPredictor the {@link FragmentationPredictor} used for predicting the fragments of the molecule
     *                      generated during MS/MS acquisition
     * @param precursorIonType the considered {@link PrecursorIonType}
     * @param numHydrogenShifts the number of possible hydrogen rearrangements which can occur during fragmentation
     */
    public BarcodeSpectrumPredictor(FragmentationPredictor fragPredictor, PrecursorIonType precursorIonType, int numHydrogenShifts){
        super(fragPredictor, precursorIonType);
        this.numHydrogenShifts = numHydrogenShifts;
    }

    /**
    * Constructs a BarcodeSpectrumPredictor.<br>
    * This predictor takes the <b>already predicted</b> fragments of {@code fragPredictor} and constructs an MS/MS spectrum.
    * Corresponding to their masses and to the {@code precursorIonType}, each fragment is assigned an m/z value and
    * an intensity of 1.0.<br>
    *
    * @param fragPredictor the {@link FragmentationPredictor} used for predicting the fragments of the molecule
    *                      generated during MS/MS acquisition
    * @param precursorIonType the considered {@link PrecursorIonType}
    */
    public BarcodeSpectrumPredictor(FragmentationPredictor fragPredictor, PrecursorIonType precursorIonType){
        this(fragPredictor, precursorIonType, 0);
    }


    @Override
    public Ms2Spectrum<Peak> predictSpectrum() {
        List<CombinatorialFragment> predFragments = this.fragPredictor.getFragments();
        SimpleMutableSpectrum spec = new SimpleMutableSpectrum(predFragments.size()*(2*this.numHydrogenShifts+1));
        final double hydrogenMass = MolecularFormula.getHydrogen().getMass();

        for(CombinatorialFragment fragment : predFragments){
            double neutralFragmentMass = fragment.getFormula().getMass();
            double fragmentMz = this.precursorIonType.neutralMassToPrecursorMass(neutralFragmentMass);

            for(int h = -this.numHydrogenShifts; h <= this.numHydrogenShifts; h++){
                double shiftedFragmentMz = fragmentMz + h * hydrogenMass;
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

}
